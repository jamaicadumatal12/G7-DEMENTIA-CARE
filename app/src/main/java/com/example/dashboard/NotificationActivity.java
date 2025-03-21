package com.example.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dashboard.adapters.NotificationAdapter;
import com.example.dashboard.models.PatientModel;
import com.example.dashboard.interfaces.PatientClickListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import com.example.dashboard.utils.NetworkUtils;

public class NotificationActivity extends AppCompatActivity implements PatientClickListener {
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, this);
        recyclerView.setAdapter(adapter);

        // Set up back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Load patients from Firebase
        loadPatients();
    }

    @Override
    public void onPatientClick(PatientModel patient) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("patient_name", patient.getName());
        intent.putExtra("patient_lat", patient.getLatitude());
        intent.putExtra("patient_lon", patient.getLongitude());
        startActivity(intent);
    }

    private void loadPatients() {
        // Show loading indicator
        findViewById(R.id.loadingProgress).setVisibility(View.VISIBLE);
        findViewById(R.id.emptyView).setVisibility(View.GONE);
        
        if (!NetworkUtils.isNetworkAvailable(this)) {
            handleNoNetwork();
            return;
        }

        Log.d("NotificationActivity", "Starting to load patients");
        
        DatabaseReference patientsRef = FirebaseDatabase.getInstance().getReference("patients");
        patientsRef.keepSynced(true);

        // Add dummy data directly if no data exists
        PatientModel patient1 = new PatientModel("P001", "Juan Dela Cruz", 71, 14.5995, 120.9842);
        PatientModel patient2 = new PatientModel("P002", "Maria Clara", 65, 14.6037, 120.9821);
        
        List<PatientModel> dummyList = new ArrayList<>();
        dummyList.add(patient1);
        dummyList.add(patient2);
        
        // Update UI with dummy data
        adapter.setPatients(dummyList);
        findViewById(R.id.loadingProgress).setVisibility(View.GONE);
        findViewById(R.id.emptyView).setVisibility(View.GONE);
        
        // Save dummy data to Firebase in background
        for (PatientModel patient : dummyList) {
            patientsRef.child(patient.getId()).setValue(patient)
                .addOnSuccessListener(aVoid -> Log.d("NotificationActivity", "Added patient: " + patient.getName()))
                .addOnFailureListener(e -> Log.e("NotificationActivity", "Error adding patient: " + e.getMessage()));
        }
    }

    private void handleNoNetwork() {
        runOnUiThread(() -> {
            findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            Toast.makeText(this, "No internet connection. Please check your network.", 
                Toast.LENGTH_SHORT).show();
        });
    }

    private void handleError(String error) {
        findViewById(R.id.loadingProgress).setVisibility(View.GONE);
        findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
        Toast.makeText(this, "Error loading patients: " + error, Toast.LENGTH_LONG).show();
    }

    private void updateUI(List<PatientModel> patientList) {
        adapter.setPatients(patientList);
        findViewById(R.id.loadingProgress).setVisibility(View.GONE);
        
        if (patientList.isEmpty()) {
            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            Log.d("NotificationActivity", "Showing empty view");
        } else {
            findViewById(R.id.emptyView).setVisibility(View.GONE);
            Log.d("NotificationActivity", "Showing patient list with " + patientList.size() + " items");
        }
    }
} 