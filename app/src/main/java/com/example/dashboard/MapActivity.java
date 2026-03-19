package com.example.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
<<<<<<< HEAD
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
=======
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
<<<<<<< HEAD
import android.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.dashboard.models.LocationHistoryModel;
import com.example.dashboard.utils.FirebaseHelper;
import com.example.dashboard.utils.NotificationHelper;
import com.example.dashboard.models.AlertType;
import android.view.View;
import android.app.ProgressDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import com.example.dashboard.models.PatientModel;
=======
import androidx.preference.PreferenceManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
<<<<<<< HEAD
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import android.os.Handler;
import android.content.Context;
=======
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import com.example.dashboard.models.PatientLocation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import java.util.ArrayList;
import java.util.List;
import org.osmdroid.api.IMapController;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import android.location.Location;
import java.util.HashMap;
import java.util.Map;
import com.example.dashboard.models.PatientModel;
import com.example.dashboard.utils.SafeZoneManager;
import android.view.View;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "MapActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
<<<<<<< HEAD
    private static final int SAFE_ZONE_COLOR = Color.argb(50, 76, 175, 80); // Semi-transparent green
    private static final int SAFE_ZONE_STROKE_COLOR = Color.argb(255, 76, 175, 80); // Solid green
    private static final int MIN_RADIUS = 10; // Minimum 10 meters for precise zones
    private static final int DEFAULT_RADIUS = 30; // 30 meters default for dementia care
    private static final int MAX_RADIUS = 5000; // 5km maximum radius
    private static final int CIRCLE_POINTS = 72; // More points for smoother circle (every 5 degrees)

    private MapView mapView;
    private IMapController mapController;
    private GeoPoint selectedLocation;
    private Marker currentLocationMarker;
    private Marker patientMarker;
    private int currentRadius = DEFAULT_RADIUS;
    private TextView radiusText;
    private TextView coordinatesText;
    private TextView deviceStatusText;
    private TextView zoomLevelText;
    private ImageButton zoomInButton;
    private ImageButton zoomOutButton;
    private Button saveZoneButton;
    private SeekBar radiusSeekBar;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private FirebaseAuth mAuth;

    // Store current patient info so we can re-center on their device when button is tapped
    private String currentPatientId;
    private String currentPatientName;
    private double currentPatientLat;
    private double currentPatientLon;
=======
    private static final String TAG = "MapActivity";
    private static final double SAFE_ZONE_RADIUS = 100.0; // meters
    private MapView map;
    private IMapController mapController;
    private Polygon safeZonePolygon;
    private boolean isEditMode = false;
    private Marker patientMarker;
    private Handler locationUpdateHandler;
    private static final int UPDATE_INTERVAL = 10000; // 10 seconds
    private GeoPoint safeZoneCenter;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        
        // CRITICAL TEST LOG - This should definitely appear
        Log.e(TAG, "🔥🔥🔥 MAPACTIVITY ONCREATE STARTED 🔥🔥🔥");
        Toast.makeText(this, "MapActivity Started!", Toast.LENGTH_LONG).show();

<<<<<<< HEAD
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "Firebase Auth initialized");
        
        // Check current authentication status
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Already authenticated. User ID: " + currentUser.getUid());
            Toast.makeText(this, "Already authenticated: " + currentUser.getUid().substring(0, 8) + "...", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "No current user, starting anonymous authentication...");
            Toast.makeText(this, "Starting authentication...", Toast.LENGTH_SHORT).show();
            
            mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    Log.d(TAG, "Anonymous sign-in successful. User ID: " + user.getUid());
                    Toast.makeText(this, "Authentication successful: " + user.getUid().substring(0, 8) + "...", Toast.LENGTH_SHORT).show();
                });
        }

        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Initialize location services
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialize views
        initializeViews();
        requestLocationPermission();

        // Initialize map with enhanced configuration for small safe zones
        mapView = findViewById(R.id.map);
        if (mapView == null) {
            Log.e(TAG, "MapView not found in layout! Cannot initialize map.");
            Toast.makeText(this, "Error: Map view not found. Please check layout file.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        mapController = mapView.getController();
        if (mapController == null) {
            Log.e(TAG, "MapController is null! Cannot initialize map.");
            Toast.makeText(this, "Error: Map controller not available.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Enhanced map configuration for small zones
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        
        // Enhanced zoom levels for small safe zones
        mapView.setMinZoomLevel(6.0);
        mapView.setMaxZoomLevel(19.0); // Higher max zoom for small areas
        
        // Use hardware acceleration for better performance
        mapView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);
        mapView.setTilesScaledToDpi(true);
        mapView.setUseDataConnection(true);
        
        // Enable map interaction
        mapView.setClickable(true);
        mapView.setFocusable(true);
        mapView.setEnabled(true);
        
        mapController.setZoom(15.0);
        
        Log.d(TAG, "Map initialized with zoom: " + mapView.getZoomLevelDouble());
        Log.d(TAG, "Map center: " + mapView.getMapCenter().getLatitude() + ", " + mapView.getMapCenter().getLongitude());
        
        // Setup map listener for zoom updates
        if (mapView != null) {
            mapView.setMapListener(new org.osmdroid.events.MapListener() {
                @Override
                public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
=======
        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize map first
        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(false); // Disable built-in zoom controls

        // Then check permissions
        if (checkAndRequestPermissions()) {
            setupMap();
        }
    }

    private boolean checkAndRequestPermissions() {
        boolean allPermissionsGranted = true;
        List<String> permissionsNeeded = new ArrayList<>();

        // Check each permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            allPermissionsGranted = false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            allPermissionsGranted = false;
        }

        // For Android 9 (API 28) and below
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                allPermissionsGranted = false;
            }
        }

        // Request permissions if needed
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
            return false;
        }

        return allPermissionsGranted;
    }

    private void setupMap() {
        try {
            Log.d("MapActivity", "Setting up map");

            // Get patient data from intent
            String patientName = getIntent().getStringExtra("patient_name");
            double patientLat = getIntent().getDoubleExtra("patient_lat", 0.0);
            double patientLon = getIntent().getDoubleExtra("patient_lon", 0.0);

            // Set zoom level and center
            mapController = map.getController();
            mapController.setZoom(15.0);
            
            // Create location point
            GeoPoint startPoint = new GeoPoint(patientLat, patientLon);
            mapController.setCenter(startPoint);

            // Initialize patient marker only once
            patientMarker = new Marker(map);
            patientMarker.setPosition(startPoint);
            patientMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            patientMarker.setTitle(patientName);
            patientMarker.setIcon(getResources().getDrawable(R.drawable.ic_patient_location));
            map.getOverlays().add(patientMarker);

            // Add a default MapEventsOverlay
            MapEventsOverlay defaultEventsOverlay = new MapEventsOverlay(this, new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
                    return false;
                }

                @Override
<<<<<<< HEAD
                public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                    updateZoomLevel();
                    return false;
                }
            });
        }
        
        // Check if patient data was passed
        currentPatientId = getIntent().getStringExtra("patient_id");
        currentPatientName = getIntent().getStringExtra("patient_name");
        currentPatientLat = getIntent().getDoubleExtra("patient_lat", 0);
        currentPatientLon = getIntent().getDoubleExtra("patient_lon", 0);
        
        // Log received data
        Log.d(TAG, "=== MAPACTIVITY INTENT DATA ===");
        Log.d(TAG, "patient_id: " + currentPatientId);
        Log.d(TAG, "patient_name: " + currentPatientName);
        Log.d(TAG, "patient_lat: " + currentPatientLat);
        Log.d(TAG, "patient_lon: " + currentPatientLon);
        Log.d(TAG, "=== END INTENT DATA ===");
        
        // Add test authentication status
        Log.d(TAG, "=== AUTHENTICATION STATUS CHECK ===");
        Log.d(TAG, "Firebase Auth instance: " + (mAuth != null ? "OK" : "NULL"));
        Log.d(TAG, "Current user: " + (mAuth.getCurrentUser() != null ? "EXISTS" : "NULL"));
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User ID: " + mAuth.getCurrentUser().getUid());
        }
        Log.d(TAG, "=== END AUTHENTICATION CHECK ===");
        
        // Test Firebase connection
        Log.d(TAG, "Testing Firebase connection...");
        DatabaseReference testRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("connection_test");
        testRef.setValue("test_" + System.currentTimeMillis())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Firebase connection test: SUCCESS");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Firebase connection test: FAILED - " + e.getMessage());
            });
        
        // If we know the patient/device ID, load its latest location from Firebase
        // and start listening for live updates from /devices/{deviceId}
        if (currentPatientId != null && !currentPatientId.isEmpty()) {
            Log.d(TAG, "MapActivity received patient_id: " + currentPatientId);
            // 1) Load last known location from /patients/{patientId}
            loadPatientLocationFromFirebase(currentPatientId);
            // 2) Start real-time listener on /devices/{deviceId} (use deviceId from patient or patientId itself)
            // Try to get deviceId from patient data, fallback to patientId
            setupDeviceStatusListener(currentPatientId);
        } else {
            Log.w(TAG, "MapActivity started without patient_id extra - using default location");
            // Fallback: center on default city (Davao) for manual safe zone selection
            GeoPoint davao = new GeoPoint(7.0707, 125.6087);
            mapController.setCenter(davao);
            mapController.setZoom(12.0);
            
            // Show message to user
            Toast.makeText(this, "No patient selected. Tap on map to set safe zone location.", Toast.LENGTH_LONG).show();
        }
        
        // Set up map click listener
        setupMapClickListener();
    }

    private void initializeViews() {
        radiusText = findViewById(R.id.radiusText);
        coordinatesText = findViewById(R.id.coordinatesText);
        deviceStatusText = findViewById(R.id.deviceStatusText);
        zoomLevelText = findViewById(R.id.zoomLevelText);
        zoomInButton = findViewById(R.id.zoomInButton);
        zoomOutButton = findViewById(R.id.zoomOutButton);
        saveZoneButton = findViewById(R.id.saveZoneButton);
        
        // Debug zoom controls
        if (zoomInButton != null) {
            Log.d(TAG, "Zoom In button found");
        } else {
            Log.e(TAG, "Zoom In button NOT found");
        }
        if (zoomOutButton != null) {
            Log.d(TAG, "Zoom Out button found");
        } else {
            Log.e(TAG, "Zoom Out button NOT found");
        }

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.w(TAG, "Back button not found in layout");
        }

        // Initially disable save button until location is selected
        if (saveZoneButton != null) {
            saveZoneButton.setEnabled(false);
        }

        // Setup zoom controls
        setupZoomControls();

        // Setup location button
        FloatingActionButton myLocationButton = findViewById(R.id.myLocationButton);
        if (myLocationButton == null) {
            Log.w(TAG, "My location button not found in layout");
        } else {
            myLocationButton.setOnClickListener(v -> {
                // If we have a known patient device location, center map on the patient
                if (currentPatientId != null && currentPatientLat != 0 && currentPatientLon != 0) {
                    Log.d(TAG, "Centering map on patient device location for " + currentPatientName);
                    showPatientLocation(currentPatientId, currentPatientName, currentPatientLat, currentPatientLon);
                } else if (selectedLocation != null && mapController != null) {
                    // Fallback: center on selected safe zone location
                    Log.d(TAG, "Centering map on selected safe zone location");
                    mapController.setCenter(selectedLocation);
                } else {
                    // Fallback: use caregiver's current location
                    Log.d(TAG, "No patient location available, using caregiver location");
                    getCurrentLocation();
                }
            });
        }
        
        // Debug: Log current map state
        Log.d(TAG, "MapActivity initialized successfully");
        Log.d(TAG, "Zoom controls setup complete");

        radiusSeekBar = findViewById(R.id.radiusSeekBar);
        if (radiusSeekBar != null) {
            radiusSeekBar.setMax(MAX_RADIUS);
            radiusSeekBar.setProgress(DEFAULT_RADIUS);
            updateRadiusText(DEFAULT_RADIUS);

            radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentRadius = Math.max(MIN_RADIUS, progress);
                updateRadiusText(currentRadius);
                if (selectedLocation != null && mapView != null) {
                    drawSafeZone(selectedLocation, currentRadius);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        } else {
            Log.w(TAG, "Radius seek bar not found in layout");
        }

        if (saveZoneButton != null) {
            saveZoneButton.setOnClickListener(v -> {
            Log.e(TAG, "🔥🔥🔥 SAVE BUTTON CLICKED 🔥🔥🔥");
            Toast.makeText(this, "Save button clicked!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Save button clicked");
            Log.d(TAG, "Firebase Auth instance: " + (mAuth != null ? "OK" : "NULL"));
            Log.d(TAG, "Current user: " + (mAuth.getCurrentUser() != null ? "EXISTS" : "NULL"));
            if (mAuth.getCurrentUser() != null) {
                Log.d(TAG, "User ID: " + mAuth.getCurrentUser().getUid());
            }
            saveZone();
            });
        } else {
            Log.w(TAG, "Save zone button not found in layout");
        }
        
        // Update initial zoom level
        updateZoomLevel();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            setupLocationUpdates();
=======
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            });
            map.getOverlays().add(0, defaultEventsOverlay);

            // Set up other components
            setupSafeZoneDrawing();
            setupPatientTracking();
            setupMapControls();
            setupMapSettings();

            map.invalidate();
        } catch (Exception e) {
            Log.e("MapActivity", "Error setting up map: " + e.getMessage());
            Toast.makeText(this, "Error loading map", Toast.LENGTH_LONG).show();
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
        }
    }

    private void setupSafeZoneDrawing() {
        Button editSafeZoneButton = findViewById(R.id.editSafeZoneButton);
        editSafeZoneButton.setOnClickListener(v -> {
            try {
                isEditMode = !isEditMode;
                if (isEditMode) {
                    // Clear existing overlays except markers
                    List<org.osmdroid.views.overlay.Overlay> overlays = new ArrayList<>(map.getOverlays());
                    map.getOverlays().clear();
                    // Keep the patient marker
                    map.getOverlays().add(patientMarker);
                    
                    // Add new MapEventsOverlay for edit mode
                    MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
                        @Override
                        public boolean singleTapConfirmedHelper(GeoPoint p) {
                            if (isEditMode && p != null) {
                                Log.d(TAG, "Tap received at: " + p.getLatitude() + ", " + p.getLongitude());
                                drawSafeZone(p);
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public boolean longPressHelper(GeoPoint p) {
                            return false;
                        }
                    });
                    map.getOverlays().add(0, eventsOverlay);
                    
                    Toast.makeText(this, "Tap anywhere to draw safe zone", Toast.LENGTH_SHORT).show();
                    editSafeZoneButton.setText("Done");
                } else {
                    editSafeZoneButton.setText("Edit Safe Zone");
                    saveSafeZone();
                }
                map.invalidate();
            } catch (Exception e) {
                Log.e(TAG, "Error in setupSafeZoneDrawing: " + e.getMessage(), e);
                isEditMode = false;
                editSafeZoneButton.setText("Edit Safe Zone");
                Toast.makeText(this, "Error entering edit mode", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawSafeZone(GeoPoint center) {
        if (center == null || map == null) return;

        try {
            Log.d(TAG, "Drawing safe zone at: " + center.getLatitude() + ", " + center.getLongitude());
            
            // Remove existing safe zone if any
            if (safeZonePolygon != null) {
                map.getOverlays().remove(safeZonePolygon);
            }

            // Create new safe zone
            safeZonePolygon = new Polygon();  // Don't pass map reference
            List<GeoPoint> circlePoints = createCirclePoints(center, SAFE_ZONE_RADIUS);
            
            safeZonePolygon.setPoints(circlePoints);
            safeZonePolygon.setFillColor(0x3300FF00);  // Semi-transparent green
            safeZonePolygon.setStrokeColor(0xFF00FF00); // Solid green
            safeZonePolygon.setStrokeWidth(5);
            
            // Disable polygon interaction
            safeZonePolygon.setEnabled(false);

            // Add the polygon to the map
            map.getOverlays().add(safeZonePolygon);
            safeZoneCenter = center;
            
            map.invalidate();
            Log.d(TAG, "Safe zone drawn successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in drawSafeZone: " + e.getMessage(), e);
            Toast.makeText(this, "Error drawing safe zone", Toast.LENGTH_SHORT).show();
        }
    }

    private List<GeoPoint> createCirclePoints(GeoPoint center, double radiusMeters) {
        List<GeoPoint> circlePoints = new ArrayList<>();
        try {
            final int segments = 60;
            final double earthRadius = 6371000;

            double lat = Math.toRadians(center.getLatitude());
            double lon = Math.toRadians(center.getLongitude());
            double angularDistance = radiusMeters / earthRadius;

            for (int i = 0; i <= segments; i++) {
                double bearing = i * 2 * Math.PI / segments;
                
                double lat2 = Math.asin(
                    Math.sin(lat) * Math.cos(angularDistance) +
                    Math.cos(lat) * Math.sin(angularDistance) * Math.cos(bearing)
                );
                
                double lon2 = lon + Math.atan2(
                    Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(lat),
                    Math.cos(angularDistance) - Math.sin(lat) * Math.sin(lat2)
                );

                circlePoints.add(new GeoPoint(
                    Math.toDegrees(lat2), 
                    Math.toDegrees(lon2)
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating circle points: " + e.getMessage());
            // Fallback to a simple triangle if circle creation fails
            circlePoints.add(center);
            circlePoints.add(new GeoPoint(center.getLatitude() + 0.001, center.getLongitude() + 0.001));
            circlePoints.add(new GeoPoint(center.getLatitude() - 0.001, center.getLongitude() + 0.001));
            circlePoints.add(center); // Close the polygon
        }
        return circlePoints;
    }

    private void setupPatientTracking() {
        locationUpdateHandler = new Handler();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updatePatientLocation();
                locationUpdateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }

    private void updatePatientLocation() {
        String patientId = getIntent().getStringExtra("patient_id");
        DatabaseReference locationRef = FirebaseDatabase.getInstance()
            .getReference("patient_locations")
            .child(patientId);

        locationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PatientLocation location = snapshot.getValue(PatientLocation.class);
                if (location != null) {
                    GeoPoint patientPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    patientMarker.setPosition(patientPoint);
                    map.invalidate();

                    // Check if patient is outside safe zone
                    checkSafeZoneViolation(patientPoint);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapActivity", "Error getting location: " + error.getMessage());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void setupMapClickListener() {
        // Check if mapView is initialized
        if (mapView == null) {
            Log.e(TAG, "Cannot setup map click listener: mapView is null");
            return;
        }
        
        // Enable multi-touch gestures for zoom
        mapView.setMultiTouchControls(true);
        
        mapView.setOnTouchListener((v, event) -> {
            // Check if touch is on zoom controls or other UI elements
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                // Get touch coordinates
                float x = event.getX();
                float y = event.getY();
                
                // Check if touch is on zoom controls (right side of screen)
                if (x > mapView.getWidth() - 100) {
                    return false; // Let zoom controls handle it
                }
                
                // Check if touch is on bottom panel
                if (y > mapView.getHeight() - 200) {
                    return false; // Let bottom panel handle it
                }
                
                // Check if touch is on top bar
                if (y < 200) {
                    return false; // Let top bar handle it
                }
                
                // Single tap for location selection
                if (event.getPointerCount() == 1) {
                    try {
                        GeoPoint clickPoint = (GeoPoint) mapView.getProjection().fromPixels((int) x, (int) y);
                        selectedLocation = clickPoint;
                        
                        // Clear existing safe-zone overlays but KEEP patient/device markers
                        // so the caregiver can still see the patient position.
                        java.util.List<Overlay> toRemove = new java.util.ArrayList<>();
                        for (Overlay overlay : mapView.getOverlays()) {
                            if (overlay != patientMarker && overlay != currentLocationMarker) {
                                toRemove.add(overlay);
                            }
                        }
                        mapView.getOverlays().removeAll(toRemove);
                        
                        // Add marker for selected location
                        Marker marker = new Marker(mapView);
                        marker.setPosition(clickPoint);
                        marker.setTitle("Selected Location");
                        mapView.getOverlays().add(marker);
                        
                        // Draw safe zone
                        drawSafeZone(clickPoint, currentRadius);
                        
                        coordinatesText.setText(String.format("Location: %.6f, %.6f", clickPoint.getLatitude(), clickPoint.getLongitude()));
                        saveZoneButton.setEnabled(true);
                        
                        mapView.invalidate();
                        return true;
                    } catch (Exception e) {
                        Log.w(TAG, "Error handling map click: " + e.getMessage());
                        return false;
                    }
                }
            }
            return false; // Allow other touch events (zoom, pan) to work
        });
    }

    private void drawSafeZone(GeoPoint center, int radius) {
        // Check if mapView is initialized
        if (mapView == null) {
            Log.e(TAG, "Cannot draw safe zone: mapView is null");
            return;
        }
        
        // Create a polygon to represent the safe zone circle with enhanced visibility
        Polygon polygon = new Polygon();
        
        // Enhanced colors for small zones (30m radius)
        if (radius <= 50) {
            polygon.setFillColor(0x40FF5722); // More visible orange fill
            polygon.setStrokeColor(0xFFFF5722); // Bright orange border
            polygon.setStrokeWidth(5); // Thicker border for small zones
        } else {
            polygon.setFillColor(0x334CAF50); // Semi-transparent green
            polygon.setStrokeColor(0xFF4CAF50); // Solid green
            polygon.setStrokeWidth(3); // Standard border width
        }
        
        // Create circle points using accurate geographic calculations
        List<GeoPoint> circlePoints = new ArrayList<>();
        int numPoints = (radius <= 50) ? 144 : 72; // More points for small zones for smoother circles
        
        for (int i = 0; i < numPoints; i++) {
            double angle = Math.toRadians(i * 360.0 / numPoints);
            
            // Use accurate geographic circle calculation
            double lat = center.getLatitude();
            double lon = center.getLongitude();
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
        
        polygon.setPoints(circlePoints);
        mapView.getOverlays().add(polygon);
        
        // Add radius label for small zones
        if (radius <= 50) {
            addRadiusLabel(center, radius);
        }
        
        Log.d(TAG, "Drew enhanced safe zone at " + center.getLatitude() + ", " + center.getLongitude() + " with radius " + radius + "m");
    }

    private void addRadiusLabel(GeoPoint center, int radius) {
        try {
            // Create a marker to show the radius
            Marker radiusMarker = new Marker(mapView);
            radiusMarker.setPosition(center);
            radiusMarker.setTitle("Safe Zone: " + radius + "m radius");
            radiusMarker.setSnippet("Center point of safe zone");
            android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_my_location);
            if (icon != null) {
                radiusMarker.setIcon(icon);
            }
            radiusMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Add to map
            mapView.getOverlays().add(radiusMarker);
            
        } catch (Exception e) {
            Log.w(TAG, "Error adding radius label: " + e.getMessage());
        }
    }

    private void updateRadiusText(int radius) {
        String unit;
        float displayValue;
        if (radius >= 1000) {
            unit = "km";
            displayValue = radius / 1000f;
        } else {
            unit = "meters";
            displayValue = radius;
        }
        String formattedRadius = String.format(Locale.getDefault(), "%.1f %s", displayValue, unit);
        radiusText.setText(String.format(Locale.getDefault(), "Radius: %s", formattedRadius));
    }

    private void setupZoomControls() {
        if (zoomInButton == null || zoomOutButton == null) {
            Log.e(TAG, "Zoom control buttons not found");
            return;
        }
        
        zoomInButton.setOnClickListener(v -> {
            if (mapView == null || mapController == null) {
                Log.e(TAG, "Cannot zoom in: mapView or mapController is null");
                return;
            }
            Log.d(TAG, "Zoom In button clicked");
            double currentZoom = mapView.getZoomLevelDouble();
            Log.d(TAG, "Current zoom: " + currentZoom + ", Max zoom: " + mapView.getMaxZoomLevel());
            if (currentZoom < mapView.getMaxZoomLevel()) {
                mapController.zoomIn();
                updateZoomLevel();
                Log.d(TAG, "Zoomed in to: " + mapView.getZoomLevelDouble());
            } else {
                Log.d(TAG, "Already at max zoom level");
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            if (mapView == null || mapController == null) {
                Log.e(TAG, "Cannot zoom out: mapView or mapController is null");
                return;
            }
            Log.d(TAG, "Zoom Out button clicked");
            double currentZoom = mapView.getZoomLevelDouble();
            Log.d(TAG, "Current zoom: " + currentZoom + ", Min zoom: " + mapView.getMinZoomLevel());
            if (currentZoom > mapView.getMinZoomLevel()) {
                mapController.zoomOut();
                updateZoomLevel();
                Log.d(TAG, "Zoomed out to: " + mapView.getZoomLevelDouble());
            } else {
                Log.d(TAG, "Already at min zoom level");
            }
        });
    }

    private void updateZoomLevel() {
        if (zoomLevelText != null && mapView != null) {
            double zoomLevel = mapView.getZoomLevelDouble();
            zoomLevelText.setText(String.format("Zoom: %.1f", zoomLevel));
        }
    }

    private void saveZone() {
        Log.d(TAG, "=== SAVE ZONE METHOD STARTED ===");
        
        if (selectedLocation == null) {
            Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get patientId from intent or use currentPatientId (set in onCreate)
        // Make it effectively final for lambda expression
        String intentPatientId = getIntent().getStringExtra("patient_id");
        final String patientId = (intentPatientId != null && !intentPatientId.isEmpty()) 
            ? intentPatientId 
            : (currentPatientId != null && !currentPatientId.isEmpty() ? currentPatientId : null);
        
        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not found. Please select a patient first.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Cannot save zone: patientId is null or empty");
            return;
        }
        
        Log.d(TAG, "Saving zone for patientId: " + patientId);
        
        // Check network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        // Force authentication check and retry if needed
        Log.d(TAG, "Checking authentication status...");
        if (mAuth.getCurrentUser() == null) {
            Log.d(TAG, "No authenticated user, attempting to sign in...");
            Toast.makeText(this, "Signing in to Firebase...", Toast.LENGTH_SHORT).show();
            
            mAuth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Authentication successful, retrying save...");
                    Toast.makeText(this, "Authentication successful, saving...", Toast.LENGTH_SHORT).show();
                    // Retry the save operation
                    saveZoneInternal(patientId);
                });
            return;
        } else {
            Log.d(TAG, "User is authenticated: " + mAuth.getCurrentUser().getUid());
        }
        
        // Proceed with save
        saveZoneInternal(patientId);
    }
    
    private void saveZoneInternal(String patientId) {

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving safe zone...");
        progressDialog.setCancelable(true);
        progressDialog.show();
        
        // Add timeout handler with longer timeout
        Handler timeoutHandler = new Handler();
        Runnable timeoutRunnable = () -> {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                Toast.makeText(this, "Save operation timed out. Please try again.", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); // 30 second timeout

        // Simplified save operation - directly save without fetching existing data
        Log.d(TAG, "Starting simplified save operation");
        Log.d(TAG, "Patient ID: " + patientId);
        
        // Since patients are stored under deviceId, we need to find the patient by deviceId
        // First, let's try to use the patientId as deviceId (common case)
        DatabaseReference patientRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("patients").child(patientId);
            
        // SIMPLIFIED APPROACH: Direct save without patient lookup
        Log.d(TAG, "Using simplified approach - direct save");
        
        // Use the patientId as deviceId (common case)
        String deviceId = patientId;
        Log.d(TAG, "Using device ID: " + deviceId);
        
        // Create zone data
        Map<String, Object> zoneData = new HashMap<>();
        zoneData.put("type", "safe");
        zoneData.put("latitude", selectedLocation.getLatitude());
        zoneData.put("longitude", selectedLocation.getLongitude());
        zoneData.put("radius", currentRadius);
        zoneData.put("timestamp", System.currentTimeMillis());
        zoneData.put("patientId", patientId);
        zoneData.put("deviceId", deviceId);

        // Save directly to zones using the device ID
        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("zones")
            .child(deviceId);

        Log.d(TAG, "Saving safe zone data: " + zoneData.toString());
        Log.d(TAG, "Firebase reference path: " + zonesRef.toString());
        Log.d(TAG, "Starting Firebase write operation...");
        
        // Perform the actual save directly
        performActualSave(zonesRef, zoneData, timeoutHandler, timeoutRunnable, progressDialog);


    }
    
    private void searchForPatientByDeviceId(String deviceId, Handler timeoutHandler, Runnable timeoutRunnable, ProgressDialog progressDialog) {
        Log.d(TAG, "Searching for patient with device ID: " + deviceId);
        
        DatabaseReference patientsRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("patients");
        patientsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean found = false;
                for (DataSnapshot patientSnapshot : dataSnapshot.getChildren()) {
                    String patientDeviceId = patientSnapshot.child("deviceId").getValue(String.class);
                    if (deviceId.equals(patientDeviceId)) {
                        found = true;
                        Log.d(TAG, "Found patient with device ID: " + deviceId);
                        
                        // Create zone data
                        Map<String, Object> zoneData = new HashMap<>();
                        zoneData.put("type", "safe");
                        zoneData.put("latitude", selectedLocation.getLatitude());
                        zoneData.put("longitude", selectedLocation.getLongitude());
                        zoneData.put("radius", currentRadius);
                        zoneData.put("timestamp", System.currentTimeMillis());
                        zoneData.put("patientId", patientSnapshot.getKey());
                        zoneData.put("deviceId", deviceId);

                        // Save directly to zones using the correct device ID
                        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("zones")
                            .child(deviceId);

                        Log.d(TAG, "Saving safe zone data: " + zoneData.toString());
                        Log.d(TAG, "Firebase reference path: " + zonesRef.toString());
                        
                        // Perform the actual save
                        performActualSave(zonesRef, zoneData, timeoutHandler, timeoutRunnable, progressDialog);
                        break;
                    }
                }
                
                if (!found) {
                    Log.e(TAG, "Patient with device ID " + deviceId + " not found in any patients");
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    progressDialog.dismiss();
                    Toast.makeText(MapActivity.this, "Patient with device ID " + deviceId + " not found", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to search for patient: " + error.getMessage());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                progressDialog.dismiss();
                Toast.makeText(MapActivity.this, "Failed to search for patient: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void performActualSave(DatabaseReference zonesRef, Map<String, Object> zoneData, 
                                  Handler timeoutHandler, Runnable timeoutRunnable, ProgressDialog progressDialog) {
        Log.d(TAG, "🔥🔥🔥 PERFORMING ACTUAL SAVE 🔥🔥🔥");
        Log.d(TAG, "Firebase path: " + zonesRef.toString());
        Log.d(TAG, "Zone data: " + zoneData.toString());
        Log.d(TAG, "User authenticated: " + (mAuth.getCurrentUser() != null));
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User ID: " + mAuth.getCurrentUser().getUid());
        }
        
        zonesRef.push().setValue(zoneData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "🔥🔥🔥 FIREBASE WRITE SUCCESS 🔥🔥🔥");
                timeoutHandler.removeCallbacks(timeoutRunnable);
                progressDialog.dismiss();
                Log.d(TAG, "Safe zone saved successfully");
                Toast.makeText(this, "Safe zone saved successfully", Toast.LENGTH_SHORT).show();
                
                // Return to previous screen
                finish();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "🔥🔥🔥 FIREBASE WRITE FAILED 🔥🔥🔥");
                Log.e(TAG, "Error saving zone: " + e.getMessage());
                Log.e(TAG, "Error type: " + e.getClass().getSimpleName());
                Log.e(TAG, "Error details: " + e.toString());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                progressDialog.dismiss();
                Toast.makeText(this, 
                    "Error saving zone: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            });
    }
    
    private void saveToLocalStorage(Map<String, Object> zoneData) {
        Log.d(TAG, "🔥🔥🔥 SAVING TO LOCAL STORAGE 🔥🔥🔥");
        try {
            // Save to SharedPreferences as backup
            android.content.SharedPreferences prefs = getSharedPreferences("safe_zones", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            
            String zoneKey = "zone_" + System.currentTimeMillis();
            editor.putString(zoneKey + "_data", zoneData.toString());
            editor.putLong(zoneKey + "_timestamp", System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "Local storage save successful: " + zoneKey);
            Toast.makeText(this, "Safe zone saved locally. Will sync to Firebase when connection is restored.", Toast.LENGTH_LONG).show();
            
            // Return to previous screen
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Local storage save failed: " + e.getMessage());
            Toast.makeText(this, "Failed to save locally: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        // Show loading message
        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show();

        // Try to get last known location first
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                GeoPoint currentLocation = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                updateCurrentLocationMarker(currentLocation);
                mapController.animateTo(currentLocation);
                Toast.makeText(this, "Location found!", Toast.LENGTH_SHORT).show();
            } else {
                // If last location is null, try to request location updates
                requestLocationUpdate();
            }
        } else {
            Toast.makeText(this, "GPS not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // Request single location update
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        if (location != null) {
                            GeoPoint currentLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                            updateCurrentLocationMarker(currentLocation);
                            mapController.animateTo(currentLocation);
                            Toast.makeText(MapActivity.this, "Location updated!", Toast.LENGTH_SHORT).show();
                            // Stop location updates after getting the first location
                            locationManager.removeUpdates(this);
                        }
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {}

                    @Override
                    public void onProviderDisabled(String provider) {}
                }, null);

                // Set a timeout for location request
                new Handler().postDelayed(() -> {
                    Toast.makeText(this, "Location request timed out. Please tap on the map to set location manually.", Toast.LENGTH_LONG).show();
                }, 15000); // 15 second timeout
            } else {
                Toast.makeText(this, "GPS not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location permission denied. Please tap on the map to set location manually.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateCurrentLocationMarker(GeoPoint location) {
        if (currentLocationMarker == null) {
            currentLocationMarker = new Marker(mapView);
            currentLocationMarker.setPosition(location);
            currentLocationMarker.setTitle("Current Location");
            mapView.getOverlays().add(currentLocationMarker);
        } else {
            currentLocationMarker.setPosition(location);
        }
        
        mapController.animateTo(location);
    }

    private void setupLocationUpdates() {
        // Setup location updates using Android native location manager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (locationListener == null) {
                    locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (location != null) {
                                updateCurrentLocationMarker(new GeoPoint(location.getLatitude(), location.getLongitude()));
                            }
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {}

                        @Override
                        public void onProviderEnabled(String provider) {}

                        @Override
                        public void onProviderDisabled(String provider) {}
                    };
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            }
        }
    }

    private void showPatientLocation(String patientId, String patientName, double lat, double lon) {
        // Check if map is initialized
        if (mapView == null || mapController == null) {
            Log.e(TAG, "Cannot show patient location: mapView or mapController is null");
            return;
        }
        
        // Center map on patient location
        GeoPoint patientLocation = new GeoPoint(lat, lon);
        mapController.setCenter(patientLocation);
        mapController.setZoom(16.0); // Closer zoom for patient view
        
        // Create or update patient marker
        if (patientMarker != null) {
            mapView.getOverlays().remove(patientMarker);
        }
        patientMarker = new Marker(mapView);
        patientMarker.setPosition(patientLocation);
        patientMarker.setTitle(patientName + " Location");
        patientMarker.setSnippet("Device: " + patientId + "\nCoordinates: " + lat + ", " + lon);
        
        // Default marker icon (will be updated by status listener)
        // Use ContextCompat to avoid deprecated method
        android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.patient_marker_safe);
        if (icon != null) {
            patientMarker.setIcon(icon);
        }
        
        // Add marker to map
        mapView.getOverlays().add(patientMarker);
        mapView.invalidate();
        
        // Update coordinates text
        if (coordinatesText != null) {
            coordinatesText.setText("Patient Location: " + String.format("%.6f, %.6f", lat, lon));
        }
        
        Log.d(TAG, "Showing patient location: " + patientName + " at " + lat + ", " + lon);
    }

    /**
     * Listen to this patient's device in /devices/{deviceId} and update
     * the status text + marker icon to show ONLINE or OFFLINE.
     */
    private void setupDeviceStatusListener(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            Log.w(TAG, "Cannot setup device status listener: deviceId is null or empty");
            return;
        }

        Log.d(TAG, "Setting up device status listener for deviceId: " + deviceId);
        DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("devices")
                .child(deviceId);

        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "Device status update received for " + deviceId + ": exists=" + snapshot.exists());
                boolean isOnline = false;

                if (!snapshot.exists()) {
                    // No data for this device → clearly offline
                    isOnline = false;
                    Log.w(TAG, "No /devices entry found for " + deviceId);
                    
                    // Still try to show patient location from /patients if available
                    if (currentPatientLat != 0 && currentPatientLon != 0) {
                        String name = (currentPatientName != null && !currentPatientName.isEmpty())
                                ? currentPatientName
                                : deviceId;
                        showPatientLocation(deviceId, name, currentPatientLat, currentPatientLon);
                    }
                } else {
                    // Our ESP8266 sends timestamp as millis() (device uptime),
                    // which is not comparable to System.currentTimeMillis().
                    // So for ONLINE/OFFLINE here we only check:
                    //   - node exists
                    //   - satellites > 0 (GPS has a signal)
                    Integer sats = snapshot.child("satellites").getValue(Integer.class);
                    boolean goodGps = (sats != null && sats > 0);
                    isOnline = goodGps;

                    // ALSO: read the latest latitude/longitude from /devices
                    // so the patient marker always uses live device data,
                    // even if the intent extras were 0 when MapActivity started.
                    Double lat = snapshot.child("latitude").getValue(Double.class);
                    Double lon = snapshot.child("longitude").getValue(Double.class);
                    Log.d(TAG, "Device snapshot lat=" + lat + ", lon=" + lon + ", sats=" + sats);

                    if (lat != null && lon != null && lat != 0 && lon != 0) {
                        currentPatientLat = lat;
                        currentPatientLon = lon;

                        // Fallback to deviceId if name wasn't passed
                        String name = (currentPatientName != null && !currentPatientName.isEmpty())
                                ? currentPatientName
                                : deviceId;

                        showPatientLocation(deviceId, name, lat, lon);
                    } else {
                        // Device exists but no valid coordinates yet
                        Log.w(TAG, "Device " + deviceId + " exists but has no valid coordinates yet");
                        // Use patient location from /patients if available
                        if (currentPatientLat != 0 && currentPatientLon != 0) {
                            String name = (currentPatientName != null && !currentPatientName.isEmpty())
                                    ? currentPatientName
                                    : deviceId;
                            showPatientLocation(deviceId, name, currentPatientLat, currentPatientLon);
                        }
                    }
                }

                // Update status text in the Set Safe Zone card
                if (deviceStatusText != null) {
                    if (isOnline) {
                        deviceStatusText.setText("Status: 🟢 Online");
                        deviceStatusText.setTextColor(androidx.core.content.ContextCompat.getColor(MapActivity.this, R.color.green_500));
                    } else {
                        deviceStatusText.setText("Status: 🔴 Offline");
                        deviceStatusText.setTextColor(androidx.core.content.ContextCompat.getColor(MapActivity.this, R.color.red_500));
                    }
                }

                // Update the patient marker icon if it exists
                if (patientMarker != null && mapView != null) {
                    android.graphics.drawable.Drawable icon = null;
                    if (isOnline) {
                        icon = androidx.core.content.ContextCompat.getDrawable(MapActivity.this, R.drawable.patient_marker_safe);
                    } else {
                        icon = androidx.core.content.ContextCompat.getDrawable(MapActivity.this, R.drawable.patient_marker_offline);
                    }
                    if (icon != null) {
                        patientMarker.setIcon(icon);
                        mapView.invalidate();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Device status listener cancelled: " + error.getMessage());
            }
        });
    }

    /**
     * One-time load of last known patient location from /patients/{patientId}.
     * This makes sure we can draw the marker immediately, even before
     * /devices/{deviceId} sends a fresh update.
     */
    private void loadPatientLocationFromFirebase(String patientId) {
        if (patientId == null || patientId.isEmpty()) {
            Log.w(TAG, "Cannot load patient location: patientId is null or empty");
            return;
        }
        
        Log.d(TAG, "Loading patient location from Firebase for patientId: " + patientId);
        DatabaseReference patientRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("patients")
                .child(patientId);

        patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "No /patients entry found for " + patientId + " – cannot preload location");
                    // Still try to get deviceId from patient data and check /devices
                    String deviceId = snapshot.child("deviceId").getValue(String.class);
                    if (deviceId != null && !deviceId.isEmpty() && !deviceId.equals(patientId)) {
                        Log.d(TAG, "Found deviceId " + deviceId + " in patient data, checking /devices");
                        setupDeviceStatusListener(deviceId);
                    }
                    return;
                }

                // Get deviceId from patient data (might be different from patientId)
                String deviceId = snapshot.child("deviceId").getValue(String.class);
                if (deviceId == null || deviceId.isEmpty()) {
                    deviceId = patientId; // Fallback to patientId
                }
                
                // Update currentPatientName if not set
                String name = snapshot.child("name").getValue(String.class);
                if (name != null && !name.isEmpty()) {
                    currentPatientName = name;
                }

                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lon = snapshot.child("longitude").getValue(Double.class);

                Log.d(TAG, "Patient snapshot lat=" + lat + ", lon=" + lon + " for " + patientId + ", deviceId=" + deviceId);

                if (lat != null && lon != null && lat != 0 && lon != 0) {
                    currentPatientLat = lat;
                    currentPatientLon = lon;
                    String displayName = (currentPatientName != null && !currentPatientName.isEmpty())
                            ? currentPatientName
                            : patientId;
                    showPatientLocation(deviceId, displayName, lat, lon);
                } else {
                    Log.w(TAG, "Patient location is missing or zero for " + patientId + ", will wait for device data");
                    // Still setup device listener to get live updates
                    setupDeviceStatusListener(deviceId);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to load patient location: " + error.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MapActivity onResume called");
        if (mapView != null) {
            mapView.onResume();
            Log.d(TAG, "MapView resumed successfully");
            Log.d(TAG, "Current zoom level: " + mapView.getZoomLevelDouble());
        } else {
            Log.e(TAG, "MapView is null in onResume");
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates when pausing
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (mapView != null) {
            mapView.onPause();
        }
    }
<<<<<<< HEAD
    
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
=======

    private void setupMapControls() {
        // Add zoom controls
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // Add compass
        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        // Add scale bar
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(map);
        scaleBarOverlay.setAlignBottom(true);
        scaleBarOverlay.setAlignRight(true);
        map.getOverlays().add(scaleBarOverlay);

        // Add location button
        FloatingActionButton locationButton = findViewById(R.id.locationButton);
        locationButton.setOnClickListener(v -> centerOnPatient());
    }

    private void setupMapSettings() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.settingsButton));
        popup.getMenuInflater().inflate(R.menu.map_settings_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_change_radius) {
                showRadiusDialog();
                return true;
            } else if (itemId == R.id.action_map_type) {
                showMapTypeDialog();
                return true;
            }
            return false;
        });
    }

    private void saveSafeZone() {
        if (safeZonePolygon != null) {
            String patientId = getIntent().getStringExtra("patient_id");
            DatabaseReference safeZoneRef = FirebaseDatabase.getInstance()
                .getReference("safe_zones")
                .child(patientId);
            
            // Save the center point and radius
            GeoPoint center = safeZonePolygon.getBounds().getCenter();
            Map<String, Object> safeZone = new HashMap<>();
            safeZone.put("latitude", center.getLatitude());
            safeZone.put("longitude", center.getLongitude());
            safeZone.put("radius", SAFE_ZONE_RADIUS);
            
            safeZoneRef.setValue(safeZone);
        }
    }

    private void centerOnPatient() {
        if (patientMarker != null) {
            map.getController().animateTo(patientMarker.getPosition());
        }
    }

    private void showRadiusDialog() {
        // Implement radius change dialog
    }

    private void showMapTypeDialog() {
        // Implement map type change dialog
    }

    private void checkSafeZoneViolation(GeoPoint patientPoint) {
        if (safeZonePolygon != null) {
            GeoPoint center = safeZonePolygon.getBounds().getCenter();
            boolean isInSafeZone = SafeZoneManager.isInSafeZone(
                patientPoint.getLatitude(), patientPoint.getLongitude(),
                center.getLatitude(), center.getLongitude(),
                SAFE_ZONE_RADIUS
            );

            if (!isInSafeZone) {
                String patientId = getIntent().getStringExtra("patient_id");
                DatabaseReference patientRef = FirebaseDatabase.getInstance()
                    .getReference("patients")
                    .child(patientId);

                patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        PatientModel patient = snapshot.getValue(PatientModel.class);
                        if (patient != null) {
                            SafeZoneManager.checkAndNotify(MapActivity.this, patient,
                                patientPoint.getLatitude(), patientPoint.getLongitude());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("MapActivity", "Error getting patient data: " + error.getMessage());
                    }
                });
            }
        }
    }

    // Add this method to properly clean up the map when activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (map != null) {
            map.onDetach();
            map = null;
        }
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
    }
} 