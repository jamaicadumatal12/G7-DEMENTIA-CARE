package com.example.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.dashboard.models.PatientModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PatientLocationActivity extends AppCompatActivity {
    private static final String TAG = "PatientLocationActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private List<PatientModel> patientList;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_map);

        // Initialize variables
        patientList = new ArrayList<>();

        // Initialize views
        initializeViews();
        
        // Request location permission
        requestLocationPermission();
        
        // Load patients
        loadPatients();
        
        // Check if we should center on a specific patient
        checkCenterOnPatient();
    }

    private void checkCenterOnPatient() {
        String centerPatientId = getIntent().getStringExtra("center_on_patient");
        if (centerPatientId != null) {
            double centerLat = getIntent().getDoubleExtra("center_lat", 0);
            double centerLon = getIntent().getDoubleExtra("center_lon", 0);
            
            if (centerLat != 0 && centerLon != 0) {
                Log.d(TAG, "Centered on patient: " + centerPatientId + " at " + centerLat + ", " + centerLon);
                statusText.setText("Centered on patient: " + centerPatientId + "\nLocation: " + centerLat + ", " + centerLon);
            }
        }
    }

    private void initializeViews() {
        statusText = findViewById(R.id.statusText);
        if (statusText == null) {
            // If statusText doesn't exist in layout, create a simple one
            statusText = new TextView(this);
            statusText.setText("Patient Location Activity Loaded Successfully!");
            setContentView(statusText);
        }

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Setup refresh button
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                refreshButton.animate().rotationBy(360).setDuration(1000).start();
                loadPatients();
                Toast.makeText(this, "Refreshing patient locations...", Toast.LENGTH_SHORT).show();
            });
        }
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
                Toast.makeText(this, "Location permission is required for better map functionality", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadPatients() {
        Log.d(TAG, "Loading patients for location display");

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
                        Log.d(TAG, "Added patient: " + patient.getName() + " at " + patient.getLatitude() + ", " + patient.getLongitude());
                    }
                }
                
                updateStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading patients: " + databaseError.getMessage());
                Toast.makeText(PatientLocationActivity.this, 
                    "Error loading patients: " + databaseError.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatus() {
        if (statusText != null) {
            StringBuilder status = new StringBuilder();
            status.append("Patient Location Activity\n\n");
            status.append("Total Patients: ").append(patientList.size()).append("\n\n");
            
            for (PatientModel patient : patientList) {
                status.append("• ").append(patient.getName())
                      .append(" (").append(patient.getDeviceId()).append(")\n");
                status.append("  Location: ").append(patient.getLatitude())
                      .append(", ").append(patient.getLongitude()).append("\n");
                status.append("  Status: ").append(patient.isInDanger() ? "DANGER" : "SAFE").append("\n\n");
            }
            
            statusText.setText(status.toString());
        }
        
        Log.d(TAG, "Updated status with " + patientList.size() + " patients");
    }
} 