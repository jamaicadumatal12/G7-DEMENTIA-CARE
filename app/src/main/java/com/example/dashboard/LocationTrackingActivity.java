package com.example.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.card.MaterialCardView;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.api.IMapController;

public class LocationTrackingActivity extends AppCompatActivity {
    private static final String TAG = "LocationTracking";
    
    private TextView latitudeText, longitudeText, satellitesText, hdopText, statusText;
    private MaterialCardView statusCard;
    private String deviceId;
    private DatabaseReference deviceRef;
    private MapView mapView;
    private IMapController mapController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_tracking);

        // Get device ID from intent
        deviceId = getIntent().getStringExtra("device_id");
        if (deviceId == null) {
            finish();
            return;
        }

        // Initialize views
        initializeViews();
        setupToolbar();

        // Initialize Firebase reference
        deviceRef = FirebaseDatabase.getInstance()
            .getReference("devices")
            .child(deviceId);

        // Start listening for location updates
        startLocationTracking();

        // Initialize OSMDroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Initialize map
        mapView = findViewById(R.id.map_view);
        mapController = mapView.getController();
        mapController.setZoom(15.0);
        
        // Set initial location (Davao)
        GeoPoint davao = new GeoPoint(7.0707, 125.6087);
        mapController.setCenter(davao);
    }

    private void initializeViews() {
        latitudeText = findViewById(R.id.latitudeText);
        longitudeText = findViewById(R.id.longitudeText);
        satellitesText = findViewById(R.id.satellitesText);
        hdopText = findViewById(R.id.hdopText);
        statusText = findViewById(R.id.statusText);
        statusCard = findViewById(R.id.statusCard);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Location Tracking - " + deviceId);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void startLocationTracking() {
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    updateStatus("Device offline", false);
                    return;
                }

                // Get location data
                Double latitude = dataSnapshot.child("latitude").getValue(Double.class);
                Double longitude = dataSnapshot.child("longitude").getValue(Double.class);
                Integer satellites = dataSnapshot.child("satellites").getValue(Integer.class);
                Double hdop = dataSnapshot.child("hdop").getValue(Double.class);
                Boolean inDanger = dataSnapshot.child("inDanger").getValue(Boolean.class);

                // Update UI
                if (latitude != null && longitude != null) {
                    latitudeText.setText(String.format("Latitude: %.6f", latitude));
                    longitudeText.setText(String.format("Longitude: %.6f", longitude));
                }

                if (satellites != null) {
                    satellitesText.setText(String.format("Satellites: %d", satellites));
                }

                if (hdop != null) {
                    hdopText.setText(String.format("HDOP: %.2f", hdop));
                }

                // Update status
                if (inDanger != null && inDanger) {
                    updateStatus("⚠️ Patient Outside Safe Zone!", true);
                } else {
                    updateStatus("Patient Within Safe Zone", false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                updateStatus("Error: " + databaseError.getMessage(), true);
            }
        });
    }

    private void updateStatus(String status, boolean isWarning) {
        statusText.setText(status);
        statusCard.setCardBackgroundColor(getResources().getColor(
            isWarning ? R.color.red_500 : R.color.green_500
        ));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceRef != null) {
            deviceRef.removeEventListener((ValueEventListener)null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
} 