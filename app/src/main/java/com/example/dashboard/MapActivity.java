package com.example.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OSMdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_map);

        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize map first
        map = findViewById(R.id.mapView);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);

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

            Log.d("MapActivity", "Patient data received: " + patientName + " at " + patientLat + "," + patientLon);

            // Set zoom level and center
            map.getController().setZoom(15.0);
            
            // Create location point
            GeoPoint startPoint = new GeoPoint(patientLat, patientLon);
            map.getController().setCenter(startPoint);

            // Add marker
            Marker marker = new Marker(map);
            marker.setPosition(startPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(patientName);
            map.getOverlays().add(marker);
            
            // Set up location button
            FloatingActionButton myLocationButton = findViewById(R.id.myLocationButton);
            myLocationButton.setOnClickListener(v -> {
                map.getController().animateTo(startPoint);
                map.getController().setZoom(15.0);
            });

            // Force a redraw
            map.invalidate();
            
        } catch (Exception e) {
            Log.e("MapActivity", "Error setting up map: " + e.getMessage());
            Toast.makeText(this, "Error loading map: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                setupMap();
            } else {
                // Show a more helpful message and don't finish the activity
                Toast.makeText(this, 
                    "Please grant location permissions to view the map", 
                    Toast.LENGTH_LONG).show();
                // Give user another chance to grant permissions
                checkAndRequestPermissions();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }
} 