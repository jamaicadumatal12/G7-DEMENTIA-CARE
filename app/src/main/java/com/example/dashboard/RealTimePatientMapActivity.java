package com.example.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dashboard.models.PatientModel;
import com.example.dashboard.utils.NetworkUtils;
import com.example.dashboard.utils.NotificationHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.content.Context;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.os.Handler;
import android.graphics.Color;

public class RealTimePatientMapActivity extends AppCompatActivity {
    private static final String TAG = "RealTimePatientMap";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private List<PatientModel> patientList;
    private Map<String, Marker> patientMarkers;
    private Map<String, Polygon> safeZones;
    private TextView statusText;
    private TextView headerStatusText;
    private TextView zoomLevelText;
    private ImageButton zoomInButton;
    private ImageButton zoomOutButton;
    private FloatingActionButton myLocationButton;
    private boolean mapViewReady = false;
    private boolean initialCenteringDone = false;
    private Map<String, ValueEventListener> deviceListeners = new HashMap<>();
    private Map<String, Boolean> deviceOnlineStatus = new HashMap<>(); // Track device online/offline status
    private Map<String, Long> deviceLastUpdateTime = new HashMap<>(); // Track last update time
    private static final long DEVICE_OFFLINE_TIMEOUT = 60000; // 60 seconds = offline (device not updating)
    private Handler statusCheckHandler = new Handler();
    private Runnable statusCheckRunnable;
    
    // Distance measurement tool
    private boolean isMeasuringDistance = false;
    private Marker firstMeasurementPoint = null;
    private Marker secondMeasurementPoint = null;
    private Polyline measurementLine = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_patient_map);

        // Initialize OSMDroid with better configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setTileDownloadThreads((short) 8);
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(50 * 1024 * 1024); // 50MB cache
        Configuration.getInstance().setTileDownloadMaxQueueSize((short) 8);

        // Initialize variables
        patientList = new ArrayList<>();
        patientMarkers = new HashMap<>();
        safeZones = new HashMap<>();

        // Initialize views
        initializeViews();
        
        // Request location permission
        requestLocationPermission();
        
        // Load patients and start real-time monitoring
        loadPatientsAndStartMonitoring();
        
        // Start periodic status check to detect offline devices
        startPeriodicStatusCheck();
    }
    
    private void startPeriodicStatusCheck() {
        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkDeviceOnlineStatus();
                statusCheckHandler.postDelayed(this, 10000); // Check every 10 seconds
            }
        };
        statusCheckHandler.postDelayed(statusCheckRunnable, 10000);
    }
    
    private void stopPeriodicStatusCheck() {
        if (statusCheckRunnable != null) {
            statusCheckHandler.removeCallbacks(statusCheckRunnable);
        }
    }
    
    private void checkDeviceOnlineStatus() {
        long currentTime = System.currentTimeMillis();
        boolean statusChanged = false;
        
        for (PatientModel patient : patientList) {
            if (patient.getDeviceId() == null) continue;
            
            String deviceId = patient.getDeviceId();
            Long lastUpdate = deviceLastUpdateTime.get(deviceId);
            boolean wasOnline = deviceOnlineStatus.getOrDefault(deviceId, false);
            
            if (lastUpdate == null) {
                // Device never updated - mark as offline
                if (wasOnline || !deviceOnlineStatus.containsKey(deviceId)) {
                    setDeviceOffline(patient);
                    statusChanged = true;
                }
            } else {
                long timeSinceUpdate = currentTime - lastUpdate;
                boolean isOnline = timeSinceUpdate <= DEVICE_OFFLINE_TIMEOUT;
                
                if (isOnline != wasOnline) {
                    if (isOnline) {
                        setDeviceOnline(patient);
                    } else {
                        setDeviceOffline(patient);
                    }
                    statusChanged = true;
                }
            }
        }
        
        if (statusChanged) {
            updateStatus();
        }
    }
    
    private void setDeviceOnline(PatientModel patient) {
        String deviceId = patient.getDeviceId();
        deviceOnlineStatus.put(deviceId, true);
        Log.d(TAG, "Device " + deviceId + " (" + patient.getName() + ") is ONLINE");
        
        // Update marker if it exists
        Marker marker = patientMarkers.get(deviceId);
        if (marker != null) {
            updateMarkerOnlineStatus(marker, patient, true);
        }
    }
    
    private void setDeviceOffline(PatientModel patient) {
        String deviceId = patient.getDeviceId();
        deviceOnlineStatus.put(deviceId, false);
        Log.d(TAG, "Device " + deviceId + " (" + patient.getName() + ") is OFFLINE");
        
        // Update marker if it exists
        Marker marker = patientMarkers.get(deviceId);
        if (marker != null) {
            updateMarkerOnlineStatus(marker, patient, false);
        } else {
            // Show offline marker even if device never sent location
            showOfflineMarker(patient);
        }
    }
    
    private void showOfflineMarker(PatientModel patient) {
        if (mapView == null || !mapViewReady || patient.getDeviceId() == null) return;
        
        // Use last known location or default location
        double lat = patient.getLatitude() != 0 ? patient.getLatitude() : 7.0707; // Default to Davao
        double lon = patient.getLongitude() != 0 ? patient.getLongitude() : 125.6087;
        
        // Update marker with offline status (isOnline = false)
        updatePatientMarker(patient, lat, lon, false, 0, false);
    }
    
    private void updateMarkerOnlineStatus(Marker marker, PatientModel patient, boolean isOnline) {
        if (marker == null) return;
        
        // Update marker icon and text based on online/offline status
        if (!isOnline) {
            // Device offline - use gray/offline icon
            marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_offline));
            marker.setTitle(patient.getName() + " - OFFLINE");
            
            // Update snippet
            String snippet = "Device: " + patient.getDeviceId() + "\n";
            snippet += "Status: 🔴 OFFLINE\n";
            snippet += "Age: " + patient.getAge() + " | Gender: " + patient.getGender() + "\n";
            snippet += "Location: Last known position\n";
            marker.setSnippet(snippet);
        } else {
            // Device online - restore appropriate icon based on danger status
            Boolean inDanger = patient.isInDanger();
            Integer satellites = patient.getSatellites();
            
            if (satellites == null || satellites == 0) {
                marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_offline));
            } else if (inDanger != null && inDanger) {
                marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_danger));
            } else {
                marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_safe));
            }
            
            String title = patient.getName();
            String status = (inDanger != null && inDanger) ? "DANGER" : "SAFE";
            String satellitesText = (satellites != null && satellites > 0) ? " (" + satellites + " satellites)" : "";
            marker.setTitle(title + " - " + status + satellitesText);
            
            // Update snippet for online device
            String snippet = "Device: " + patient.getDeviceId() + "\n";
            snippet += "Status: 🟢 ONLINE\n";
            snippet += "Age: " + patient.getAge() + " | Gender: " + patient.getGender() + "\n";
            snippet += "Location: " + String.format("%.8f, %.8f", patient.getLatitude(), patient.getLongitude()) + "\n";
            marker.setSnippet(snippet);
        }
        
        mapView.postInvalidate();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.realTimeMapView);
        
        // Alternative stable map configuration
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        
        // Enhanced zoom levels for small safe zones
        mapView.setMinZoomLevel(6.0);
        mapView.setMaxZoomLevel(19.0); // Higher max zoom for small areas
        
        // Alternative rendering settings
        mapView.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);
        mapView.setTilesScaledToDpi(false);
        mapView.setUseDataConnection(true);
        
        // Disable some features that might cause issues
        mapView.setClickable(true);
        mapView.setFocusable(true);
        
        // Disable map listener to prevent zoom interference
        // mapView.setMapListener(null);

        // Set initial location (Davao) with stable zoom
        GeoPoint davao = new GeoPoint(7.0707, 125.6087);
        mapView.getController().setCenter(davao);
        mapView.getController().setZoom(12.0); // Lower initial zoom for stability

        // Mark mapView as ready
        mapViewReady = true;

        statusText = findViewById(R.id.statusText);
        headerStatusText = findViewById(R.id.headerStatusText);
        zoomLevelText = findViewById(R.id.zoomLevelText);
        zoomInButton = findViewById(R.id.zoomInButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        myLocationButton = findViewById(R.id.myLocationButton);

        // Setup zoom controls
        setupZoomControls();

        // Setup my location button
        setupMyLocationButton();

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Setup refresh button
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> refreshPatientData());

        // Setup distance measurement button (commented out until UI is added)
        // ImageButton measureButton = findViewById(R.id.measureButton);
        // if (measureButton != null) {
        //     measureButton.setOnClickListener(v -> toggleDistanceMeasurement());
        // }

        // Update initial zoom level
        updateZoomLevel();
    }

    private void setupZoomControls() {
        zoomInButton.setOnClickListener(v -> {
            double currentZoom = mapView.getZoomLevelDouble();
            if (currentZoom < mapView.getMaxZoomLevel()) {
                // Use setZoom for stable operation
                double newZoom = Math.min(currentZoom + 1.0, mapView.getMaxZoomLevel());
                mapView.getController().setZoom(newZoom);
                
                // Update zoom level display after a short delay
                new Handler().postDelayed(() -> {
                    updateZoomLevel();
                }, 200);
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            double currentZoom = mapView.getZoomLevelDouble();
            if (currentZoom > mapView.getMinZoomLevel()) {
                // Use setZoom for stable operation
                double newZoom = Math.max(currentZoom - 1.0, mapView.getMinZoomLevel());
                mapView.getController().setZoom(newZoom);
                
                // Update zoom level display after a short delay
                new Handler().postDelayed(() -> {
                    updateZoomLevel();
                }, 200);
            }
        });
    }

    private void setupMyLocationButton() {
        myLocationButton.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Get current location and center map
                getCurrentLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    private void updateZoomLevel() {
        if (zoomLevelText != null && mapView != null) {
            double zoomLevel = mapView.getZoomLevelDouble();
            zoomLevelText.setText(String.format("Zoom: %.1f", zoomLevel));
        }
    }



    private void getCurrentLocation() {
        // Show loading message
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();

        // Use Android native location manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    GeoPoint currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    mapView.getController().animateTo(currentLocation);
                    mapView.getController().setZoom(15.0);
                    updateZoomLevel();
                    Toast.makeText(this, "Location found!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Location not available. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "GPS not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshPatientData() {
        ImageButton refreshButton = findViewById(R.id.refreshButton);
            refreshButton.animate().rotationBy(360).setDuration(1000).start();
            loadPatientsAndStartMonitoring();
            Toast.makeText(this, "Refreshing patient locations...", Toast.LENGTH_SHORT).show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted");
            } else {
                Toast.makeText(this, "Location permission is required for real-time monitoring", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadPatientsAndStartMonitoring() {
        Log.d(TAG, "Starting real-time patient monitoring");

        DatabaseReference patientsRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("patients");

        patientsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                patientList.clear();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    PatientModel patient = snapshot.getValue(PatientModel.class);
                    if (patient != null && patient.getDeviceId() != null) {
                        patientList.add(patient);
                        Log.d(TAG, "Added patient: " + patient.getName() + " with device: " + patient.getDeviceId());
                        
                        // Initialize device as offline until first update confirms it's online
                        deviceOnlineStatus.put(patient.getDeviceId(), false);
                        
                        // Start monitoring this patient's device
                        startDeviceMonitoring(patient);
                    }
                }
                
                updateStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading patients: " + databaseError.getMessage());
                Toast.makeText(RealTimePatientMapActivity.this, 
                    "Error loading patients: " + databaseError.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDeviceMonitoring(PatientModel patient) {
        if (patient.getDeviceId() == null) return;
        
        DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("devices").child(patient.getDeviceId());
            
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                try {
                    // Check if device exists in database
                    if (!deviceSnapshot.exists()) {
                        Log.w(TAG, "Device " + patient.getDeviceId() + " does not exist in database - marking offline");
                        setDeviceOffline(patient);
                        return;
                    }
                    
                    // Device exists - mark as online and update timestamp
                    long currentTime = System.currentTimeMillis();
                    deviceLastUpdateTime.put(patient.getDeviceId(), currentTime);
                    
                    // Get real-time device data
                    Object latObj = deviceSnapshot.child("latitude").getValue();
                    Object lonObj = deviceSnapshot.child("longitude").getValue();
                    Long timestamp = deviceSnapshot.child("timestamp").getValue(Long.class);
                    Boolean inDanger = deviceSnapshot.child("inDanger").getValue(Boolean.class);
                    Integer satellites = deviceSnapshot.child("satellites").getValue(Integer.class);
                    Float hdop = deviceSnapshot.child("hdop").getValue(Float.class);
                    
                    // Mark device as online
                    if (!deviceOnlineStatus.getOrDefault(patient.getDeviceId(), false)) {
                        setDeviceOnline(patient);
                    }
                    
                    // Parse coordinates
                    Double latitude = null;
                    Double longitude = null;
                    
                    if (latObj instanceof Number) {
                        latitude = ((Number) latObj).doubleValue();
                    } else if (latObj instanceof String) {
                        try {
                            latitude = Double.parseDouble((String) latObj);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid latitude format: " + latObj);
                        }
                    }
                    
                    if (lonObj instanceof Number) {
                        longitude = ((Number) lonObj).doubleValue();
                    } else if (lonObj instanceof String) {
                        try {
                            longitude = Double.parseDouble((String) lonObj);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid longitude format: " + lonObj);
                        }
                    }
                    
                    if (latitude != null && longitude != null) {
                        Log.d(TAG, "Real-time update for " + patient.getName() + 
                              ": LAT=" + latitude + ", LNG=" + longitude + 
                              ", DANGER=" + inDanger + ", SAT=" + satellites);
                        
                        // Create final copies for lambda expression
                        final double finalLatitude = latitude;
                        final double finalLongitude = longitude;
                        final Boolean finalInDanger = inDanger;
                        final Integer finalSatellites = satellites;
                        
                        // Update patient data in memory
                        patient.setLatitude(finalLatitude);
                        patient.setLongitude(finalLongitude);
                        if (finalInDanger != null) patient.setInDanger(finalInDanger);
                        if (finalSatellites != null) patient.setSatellites(finalSatellites);
                        
                        // Update patient marker on map with delay to prevent zoom conflicts
                        new Handler().postDelayed(() -> {
                            updatePatientMarker(patient, finalLatitude, finalLongitude, finalInDanger, finalSatellites, true);
                        }, 50);
                        
                        // Check safe zones
                        checkSafeZones(patient, latitude, longitude);
                        
                        // Update status
                        updateStatus();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in device monitoring: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Device monitoring cancelled: " + error.getMessage());
                // When listener is cancelled, mark device as offline
                setDeviceOffline(patient);
            }
        };
        
        // Store the listener for later removal
        deviceListeners.put(patient.getDeviceId(), listener);
        
        // Add the listener to Firebase
        deviceRef.addValueEventListener(listener);
    }

    private void updatePatientMarker(PatientModel patient, double lat, double lon, Boolean inDanger, Integer satellites, boolean isOnline) {
        // Check if mapView is initialized and ready
        if (mapView == null || !mapViewReady) {
            Log.e(TAG, "MapView is not ready, cannot update marker");
            return;
        }
        
        try {
        // Remove existing marker
        Marker existingMarker = patientMarkers.get(patient.getDeviceId());
        if (existingMarker != null) {
                try {
            mapView.getOverlays().remove(existingMarker);
                } catch (Exception e) {
                    Log.w(TAG, "Error removing existing marker: " + e.getMessage());
                }
        }
        
        // Create new marker
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(lat, lon));
        
        // Check device online status first
        boolean deviceIsOnline = isOnline && deviceOnlineStatus.getOrDefault(patient.getDeviceId(), true);
        
        // Set marker title and snippet with enhanced info
        String title = patient.getName();
        String status;
        if (!deviceIsOnline) {
            status = "OFFLINE";
        } else {
            status = (inDanger != null && inDanger) ? "DANGER" : "SAFE";
        }
        String satellitesText = (satellites != null && satellites > 0) ? " (" + satellites + " satellites)" : "";
        marker.setTitle(title + " - " + status + satellitesText);
        
        String snippet = "Device: " + patient.getDeviceId() + "\n";
        snippet += "Status: " + (deviceIsOnline ? "🟢 ONLINE" : "🔴 OFFLINE") + "\n";
        snippet += "Age: " + patient.getAge() + " | Gender: " + patient.getGender() + "\n";
        if (deviceIsOnline) {
            snippet += "Location: " + String.format("%.8f, %.8f", lat, lon) + "\n";
        } else {
            snippet += "Location: Last known position\n";
        }
            
            // Add distance to safe zone if available
            if (safeZones.containsKey(patient.getDeviceId())) {
                // Get safe zone center (you might need to store this separately)
                // For now, we'll show the precise coordinates
                snippet += "GPS Precision: " + String.format("%.3fm accuracy", 
                    (satellites != null && satellites > 0) ? 5.0 : 50.0);
            }
            
        marker.setSnippet(snippet);
        
        // Set marker icon based on online/offline status first, then danger status
        if (!deviceIsOnline) {
            // Device offline - Gray/Offline icon
            marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_offline));
        } else if (satellites == null || satellites == 0) {
            // Device online but no GPS signal - Gray
            marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_offline));
        } else if (inDanger != null && inDanger) {
            // Device online and in danger - Red
            marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_danger));
        } else {
            // Device online and safe - Green
            marker.setIcon(getResources().getDrawable(R.drawable.patient_marker_safe));
        }
        
        // Set marker anchor
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // Improve marker appearance and interaction
        marker.setInfoWindow(null); // Use custom info window
        marker.setDraggable(false);
        marker.setFlat(false); // 3D effect for better visibility
        
        // Add marker to map
        try {
        mapView.getOverlays().add(marker);
        patientMarkers.put(patient.getDeviceId(), marker);
        
            // Refresh map using postInvalidate for better thread safety
            mapView.postInvalidate();
            
            // Only do initial centering once when first patient is added
            if (!initialCenteringDone && patientMarkers.size() == 1) {
                mapView.getController().animateTo(new GeoPoint(lat, lon));
                mapView.getController().setZoom(12.0); // Use stable initial zoom
                initialCenteringDone = true;
            }
            
            Log.d(TAG, "Updated marker for " + patient.getName() + " at " + lat + ", " + lon);
        } catch (Exception e) {
            Log.e(TAG, "Error adding marker to map: " + e.getMessage());
        }
    } catch (Exception e) {
        Log.e(TAG, "Error updating patient marker: " + e.getMessage());
    }
    }

    private void checkSafeZones(PatientModel patient, double lat, double lon) {
        Log.d(TAG, "Checking safe zones for patient: " + patient.getName());
        
        // Load safe zones for this patient with timeout
        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("zones").child(patient.getDeviceId());
            
        // Add timeout for Firebase operation
        Handler timeoutHandler = new Handler();
        Runnable timeoutRunnable = () -> {
            Log.w(TAG, "Safe zone check timeout for patient: " + patient.getName());
            // Try to load from local storage as fallback
            loadSafeZoneFromLocalStorage(patient, lat, lon);
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 second timeout
            
        zonesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                timeoutHandler.removeCallbacks(timeoutRunnable); // Cancel timeout
                Log.d(TAG, "Safe zone data received for patient: " + patient.getName());
                
                boolean foundSafeZone = false;
                boolean isInsideAnyZone = false;
                double closestZoneDistance = Double.MAX_VALUE;
                double closestZoneRadius = 0;
                
                // Check all safe zones - patient is safe if inside ANY zone
                for (DataSnapshot zoneSnapshot : dataSnapshot.getChildren()) {
                    String type = zoneSnapshot.child("type").getValue(String.class);
                    if ("safe".equals(type)) {
                        foundSafeZone = true;
                        Double safeLat = zoneSnapshot.child("latitude").getValue(Double.class);
                        Double safeLon = zoneSnapshot.child("longitude").getValue(Double.class);
                        Integer radius = zoneSnapshot.child("radius").getValue(Integer.class);
                        
                        if (safeLat != null && safeLon != null && radius != null) {
                            Log.d(TAG, "Safe zone found: " + safeLat + ", " + safeLon + " radius: " + radius + "m");
                            
                            // Draw safe zone
                            drawSafeZone(patient.getDeviceId(), safeLat, safeLon, radius);
                            
                            // Check if patient is outside safe zone using precise calculation
                            double distance = NetworkUtils.calculateDistancePrecise(lat, lon, safeLat, safeLon);
                            Log.d(TAG, "Distance to safe zone: " + distance + "m (radius: " + radius + "m)");
                            
                            if (distance <= radius) {
                                // Patient is inside this zone - they're safe!
                                isInsideAnyZone = true;
                                Log.d(TAG, "Patient is within safe zone (distance: " + distance + "m <= radius: " + radius + "m)");
                                // Continue drawing all zones but mark as safe
                            } else {
                                // Track the closest zone for alert details
                                if (distance < closestZoneDistance) {
                                    closestZoneDistance = distance;
                                    closestZoneRadius = radius;
                                }
                                Log.d(TAG, "Patient is outside this zone (distance: " + distance + "m > radius: " + radius + "m)");
                            }
                        }
                    }
                }
                
                // Send alert only if patient is outside ALL safe zones
                if (foundSafeZone && !isInsideAnyZone) {
                    Log.w(TAG, "🚨 PATIENT LEFT ALL SAFE ZONES! Closest zone distance: " + closestZoneDistance + "m, Radius: " + closestZoneRadius + "m");
                    // Show local notification + UI alert (background service handles history persistence)
                    showDangerAlert(patient, closestZoneDistance, (int)closestZoneRadius);
                } else if (foundSafeZone && isInsideAnyZone) {
                    Log.d(TAG, "Patient is safe - inside at least one safe zone");
                } else if (!foundSafeZone) {
                    Log.w(TAG, "No safe zone found for patient: " + patient.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.e(TAG, "Error loading safe zones: " + error.getMessage());
                // Try to load from local storage as fallback
                loadSafeZoneFromLocalStorage(patient, lat, lon);
            }
        });
    }
    
    private void loadSafeZoneFromLocalStorage(PatientModel patient, double lat, double lon) {
        Log.d(TAG, "Loading safe zone from local storage for patient: " + patient.getName());
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("safe_zones", MODE_PRIVATE);
            java.util.Map<String, ?> allPrefs = prefs.getAll();
            
            for (String key : allPrefs.keySet()) {
                if (key.endsWith("_data")) {
                    String zoneData = prefs.getString(key, "");
                    if (zoneData.contains("patientId=" + patient.getDeviceId())) {
                        // Parse the zone data (simplified parsing)
                        if (zoneData.contains("latitude=") && zoneData.contains("longitude=") && zoneData.contains("radius=")) {
                            // Extract values using simple parsing
                            String[] parts = zoneData.split(",");
                            double safeLat = 0, safeLon = 0;
                            int radius = 0;
                            
                            for (String part : parts) {
                                if (part.contains("latitude=")) {
                                    safeLat = Double.parseDouble(part.split("=")[1]);
                                } else if (part.contains("longitude=")) {
                                    safeLon = Double.parseDouble(part.split("=")[1]);
                                } else if (part.contains("radius=")) {
                                    radius = Integer.parseInt(part.split("=")[1]);
                                }
                            }
                            
                            if (safeLat != 0 && safeLon != 0 && radius > 0) {
                                Log.d(TAG, "Local safe zone loaded: " + safeLat + ", " + safeLon + " radius: " + radius + "m");
                                
                                // Draw safe zone
                                drawSafeZone(patient.getDeviceId(), safeLat, safeLon, radius);
                                
                                // Check if patient is outside safe zone using precise calculation
                                double distance = NetworkUtils.calculateDistancePrecise(lat, lon, safeLat, safeLon);
                                Log.d(TAG, "Distance to local safe zone: " + distance + "m (radius: " + radius + "m)");
                                
                                if (distance > radius) {
                                    showDangerAlert(patient, distance, radius);
                                }
                                return;
                            }
                        }
                    }
                }
            }
            Log.w(TAG, "No local safe zone found for patient: " + patient.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error loading local safe zone: " + e.getMessage());
        }
    }

    private void drawSafeZone(String deviceId, double lat, double lon, int radius) {
        // Remove existing safe zone if any
        if (safeZones.containsKey(deviceId)) {
            mapView.getOverlays().remove(safeZones.get(deviceId));
            safeZones.remove(deviceId);
        }

        // Wait for map to be ready
        if (!mapViewReady) {
            Log.w(TAG, "MapView not ready, retrying in 1 second...");
            new Handler().postDelayed(() -> {
                if (mapViewReady) {
                    drawSafeZone(deviceId, lat, lon, radius);
                }
            }, 1000);
            return;
        }

        try {
            // Create circle points using accurate geographic calculations (spherical trigonometry)
            // This matches the calculation method in MapActivity.java for consistency
            List<GeoPoint> circlePoints = new ArrayList<>();
            int numPoints = (radius <= 50) ? 144 : 72; // More points for small zones for smoother circles
            
            for (int i = 0; i < numPoints; i++) {
                double angle = Math.toRadians(i * 360.0 / numPoints);
                
                // Use accurate geographic circle calculation (same as MapActivity)
                double latRad = Math.toRadians(lat);
                double lonRad = Math.toRadians(lon);
                double angularDistance = radius / 6371000.0; // Earth's radius in meters
                
                double circleLat = Math.asin(
                    Math.sin(latRad) * Math.cos(angularDistance) +
                    Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(angle)
                );
                
                double circleLon = lonRad + Math.atan2(
                    Math.sin(angle) * Math.sin(angularDistance) * Math.cos(latRad),
                    Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(circleLat)
                );
                
                // Convert back to degrees
                circleLat = Math.toDegrees(circleLat);
                circleLon = Math.toDegrees(circleLon);
                
                circlePoints.add(new GeoPoint(circleLat, circleLon));
            }
            
            // Create polygon for the safe zone with enhanced visibility for small zones
            Polygon safeZone = new Polygon();
            safeZone.setPoints(circlePoints);
            
            // Enhanced colors for small zones (30m radius) - matching MapActivity
            if (radius <= 50) {
                safeZone.setFillColor(0x40FF5722); // More visible orange fill
                safeZone.setStrokeColor(0xFFFF5722); // Bright orange border
                safeZone.setStrokeWidth(5.0f); // Thicker border for small zones
            } else {
                safeZone.setFillColor(Color.argb(50, 0, 255, 0)); // Semi-transparent green
                safeZone.setStrokeColor(Color.GREEN);
                safeZone.setStrokeWidth(3.0f); // Standard border width
            }
            
            // Add to map
            mapView.getOverlays().add(safeZone);
            safeZones.put(deviceId, safeZone);
            
            // Add radius label
            addRadiusLabel(deviceId, lat, lon, radius);
            
            // Refresh map
            mapView.invalidate();
            
            Log.d(TAG, "Safe zone drawn for device " + deviceId + " at (" + lat + ", " + lon + ") with radius " + radius + "m");
            
        } catch (Exception e) {
            Log.e(TAG, "Error drawing safe zone: " + e.getMessage());
        }
    }

    private void addRadiusLabel(String deviceId, double lat, double lon, int radius) {
        try {
            // Create a marker to show the radius
            Marker radiusMarker = new Marker(mapView);
            radiusMarker.setPosition(new GeoPoint(lat, lon));
            radiusMarker.setTitle("Safe Zone: " + radius + "m radius");
            radiusMarker.setSnippet("Center point of safe zone");
            radiusMarker.setIcon(getResources().getDrawable(R.drawable.ic_my_location));
            radiusMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Add to map
            mapView.getOverlays().add(radiusMarker);
            
        } catch (Exception e) {
            Log.w(TAG, "Error adding radius label: " + e.getMessage());
        }
    }

    private void toggleDistanceMeasurement() {
        isMeasuringDistance = !isMeasuringDistance;
        
        if (isMeasuringDistance) {
            // Clear previous measurement
            clearMeasurement();
            
            // Show instruction
            Toast.makeText(this, "Tap two points on the map to measure distance", Toast.LENGTH_LONG).show();
            
            // Set up map click listener
            mapView.setOnClickListener(v -> handleMapClick());
        } else {
            // Clear measurement
            clearMeasurement();
            Toast.makeText(this, "Distance measurement disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMapClick() {
        if (!isMeasuringDistance) return;
        
        // Get click position (this is simplified - you'd need to implement proper click detection)
        // For now, we'll use the map center as an example
        org.osmdroid.api.IGeoPoint centerPoint = mapView.getMapCenter();
        GeoPoint clickPoint = new GeoPoint(centerPoint.getLatitude(), centerPoint.getLongitude());
        
        if (firstMeasurementPoint == null) {
            // First point
            firstMeasurementPoint = new Marker(mapView);
            firstMeasurementPoint.setPosition(clickPoint);
            firstMeasurementPoint.setTitle("Point 1");
            firstMeasurementPoint.setSnippet("Tap second point");
            firstMeasurementPoint.setIcon(getResources().getDrawable(R.drawable.ic_my_location));
            mapView.getOverlays().add(firstMeasurementPoint);
            
            Toast.makeText(this, "First point set. Tap second point.", Toast.LENGTH_SHORT).show();
        } else if (secondMeasurementPoint == null) {
            // Second point
            secondMeasurementPoint = new Marker(mapView);
            secondMeasurementPoint.setPosition(clickPoint);
            secondMeasurementPoint.setTitle("Point 2");
            secondMeasurementPoint.setSnippet("Distance calculated");
            secondMeasurementPoint.setIcon(getResources().getDrawable(R.drawable.ic_my_location));
            mapView.getOverlays().add(secondMeasurementPoint);
            
            // Draw line between points
            drawMeasurementLine();
            
            // Calculate and show distance
            calculateAndShowDistance();
            
            // Reset for next measurement
            isMeasuringDistance = false;
        }
        
        mapView.invalidate();
    }

    private void drawMeasurementLine() {
        if (firstMeasurementPoint != null && secondMeasurementPoint != null) {
            measurementLine = new Polyline();
            measurementLine.setPoints(Arrays.asList(
                firstMeasurementPoint.getPosition(),
                secondMeasurementPoint.getPosition()
            ));
            measurementLine.setColor(0xFFFF0000); // Red line
            measurementLine.setWidth(3);
            mapView.getOverlays().add(measurementLine);
        }
    }

    private void calculateAndShowDistance() {
        if (firstMeasurementPoint != null && secondMeasurementPoint != null) {
            GeoPoint p1 = firstMeasurementPoint.getPosition();
            GeoPoint p2 = secondMeasurementPoint.getPosition();
            
            double distance = NetworkUtils.calculateDistancePrecise(
                p1.getLatitude(), p1.getLongitude(),
                p2.getLatitude(), p2.getLongitude()
            );
            
            String breakdown = NetworkUtils.getDistanceBreakdown(
                p1.getLatitude(), p1.getLongitude(),
                p2.getLatitude(), p2.getLongitude()
            );
            
            String message = String.format("Distance: %.3fm\n%s", distance, breakdown);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "Distance measurement: " + message);
        }
    }

    private void clearMeasurement() {
        if (firstMeasurementPoint != null) {
            mapView.getOverlays().remove(firstMeasurementPoint);
            firstMeasurementPoint = null;
        }
        if (secondMeasurementPoint != null) {
            mapView.getOverlays().remove(secondMeasurementPoint);
            secondMeasurementPoint = null;
        }
        if (measurementLine != null) {
            mapView.getOverlays().remove(measurementLine);
            measurementLine = null;
        }
        mapView.invalidate();
    }

    private void showDangerAlert(PatientModel patient, double distance, int radius) {
        // Show notification using NotificationHelper
        NotificationHelper.showSafeZoneViolationAlert(this, patient, distance, radius);
        
        String message = "⚠️ ALERT: " + patient.getName() + " is outside safe zone!\n" +
                        "Distance: " + String.format("%.1f", distance) + "m (Safe: " + radius + "m)";
        
        // Show prominent alert
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.w(TAG, "🚨 DANGER ALERT: " + message);
        
        // Update status with danger indication
        if (statusText != null) {
            String currentStatus = statusText.getText().toString();
            String dangerStatus = "🚨 " + patient.getName() + " OUTSIDE SAFE ZONE! (" + 
                                String.format("%.1f", distance) + "m)";
            
            // Replace or add danger status
            if (currentStatus.contains("🚨")) {
                // Update existing danger status
                currentStatus = currentStatus.replaceAll("🚨.*?\\(", "🚨 " + patient.getName() + " OUTSIDE SAFE ZONE! (");
            } else {
                // Add danger status
                currentStatus += "\n\n" + dangerStatus;
            }
            
            statusText.setText(currentStatus);
        }
    }

    private void updateStatus() {
        // Count online and offline devices
        int onlineCount = 0;
        int offlineCount = 0;
        for (PatientModel patient : patientList) {
            if (patient.getDeviceId() != null) {
                boolean isOnline = deviceOnlineStatus.getOrDefault(patient.getDeviceId(), false);
                if (isOnline) {
                    onlineCount++;
                } else {
                    offlineCount++;
                }
            }
        }
        
        // Update header status (always visible at top)
        if (headerStatusText != null) {
            if (patientList.isEmpty()) {
                headerStatusText.setText("No patients");
            } else {
                headerStatusText.setText("🟢 " + onlineCount + " Online | 🔴 " + offlineCount + " Offline");
            }
        }
        
        // Update detailed status panel at bottom
        if (statusText != null) {
            StringBuilder statusBuilder = new StringBuilder();
            statusBuilder.append("Real-Time Patient Monitoring\n\n");
            statusBuilder.append("Active Patients: ").append(patientList.size()).append("\n");
            statusBuilder.append("🟢 Online Devices: ").append(onlineCount).append("\n");
            statusBuilder.append("🔴 Offline Devices: ").append(offlineCount).append("\n\n");
            
            for (PatientModel patient : patientList) {
                if (patient.getDeviceId() != null) {
                    boolean isOnline = deviceOnlineStatus.getOrDefault(patient.getDeviceId(), false);
                    String patientStatus = isOnline ? "🟢 Online" : "🔴 Offline";
                    statusBuilder.append("• ").append(patient.getName())
                            .append(" (").append(patient.getDeviceId()).append("): ")
                            .append(patientStatus).append("\n");
                }
            }
            
            statusText.setText(statusBuilder.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            mapViewReady = true; // Ensure map is ready when resuming
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
            mapViewReady = false; // Mark map as not ready when pausing
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop periodic status check
        stopPeriodicStatusCheck();
        
        // Remove all device listeners
        for (Map.Entry<String, ValueEventListener> entry : deviceListeners.entrySet()) {
            DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("devices").child(entry.getKey());
            deviceRef.removeEventListener(entry.getValue());
        }
        deviceListeners.clear();
        
        if (mapView != null) {
            mapView.onDetach();
        }
    }
} 