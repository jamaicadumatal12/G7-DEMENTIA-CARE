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
import com.example.dashboard.models.LocationHistoryModel;
import com.example.dashboard.interfaces.PatientClickListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import com.example.dashboard.utils.NetworkUtils;
import com.example.dashboard.utils.FirebaseHelper;

/**
 * NotificationActivity - Displays notification history for patient alerts
 * 
 * NOTE: This activity includes DUMMY DATA functionality for screenshot/documentation purposes.
 * When no real data is available, it automatically displays 3 sample notifications.
 * TODO: Remove dummy data methods (createDummyAlerts, createDummyPatients) after completing documentation.
 */
public class NotificationActivity extends AppCompatActivity implements PatientClickListener {
    private NotificationAdapter adapter;
    private static final String TAG = "NotificationActivity";

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

        // Load alerts from Firebase
        loadAlerts();
    }

    private void loadAlerts() {
        // Show loading indicator
        findViewById(R.id.loadingProgress).setVisibility(View.VISIBLE);
        findViewById(R.id.emptyView).setVisibility(View.GONE);
        
        if (!NetworkUtils.isNetworkAvailable(this)) {
            handleNoNetwork();
            return;
        }

        Log.d(TAG, "Starting to load alerts");

        // Get all patients first
        FirebaseHelper.getPatientsReference()
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<PatientModel> allPatients = new ArrayList<>();
                    for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                        PatientModel patient = patientSnapshot.getValue(PatientModel.class);
                        if (patient != null) {
                            allPatients.add(patient);
                        }
                    }
                    
                    if (allPatients.isEmpty()) {
                        // No patients → show empty state, no dummy data
                        runOnUiThread(() -> {
                            adapter.setAlerts(new ArrayList<>(), new ArrayList<>());
                            findViewById(R.id.loadingProgress).setVisibility(View.GONE);
                            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                        });
                        return;
                    }

                    // Load alerts for each patient
                    List<LocationHistoryModel> allAlerts = new ArrayList<>();
                    int[] loadedCount = {0};

                    for (PatientModel patient : allPatients) {
                        FirebaseHelper.getAlerts(patient.getId(), new FirebaseHelper.OnLocationHistoryListener() {
                            @Override
                            public void onHistoryLoaded(List<LocationHistoryModel> alerts) {
                                allAlerts.addAll(alerts);
                                loadedCount[0]++;

                                // If all patients' alerts are loaded
                                if (loadedCount[0] == allPatients.size()) {
                                    // Sort alerts by timestamp (newest first)
                                    Collections.sort(allAlerts, (a1, a2) -> 
                                        Long.compare(a2.getTimestamp(), a1.getTimestamp()));
                                    
                                    // Update UI on main thread
                                    runOnUiThread(() -> {
                                        List<LocationHistoryModel> finalAlerts = allAlerts;
                                        List<PatientModel> finalPatients = allPatients;

                                        adapter.setAlerts(finalAlerts, finalPatients);
                                        findViewById(R.id.loadingProgress).setVisibility(View.GONE);
                                        
                                        if (finalAlerts.isEmpty()) {
                                            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                                        } else {
                                            findViewById(R.id.emptyView).setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Error loading alerts: " + error);
                                loadedCount[0]++;

                                // If all attempts are complete (even with errors)
                                if (loadedCount[0] == allPatients.size()) {
                                    runOnUiThread(() -> {
                                        List<LocationHistoryModel> finalAlerts = allAlerts;
                                        List<PatientModel> finalPatients = allPatients;

                                        adapter.setAlerts(finalAlerts, finalPatients);
                                        findViewById(R.id.loadingProgress).setVisibility(View.GONE);
                                        
                                        if (finalAlerts.isEmpty()) {
                                            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                                        } else {
                                            findViewById(R.id.emptyView).setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error loading patients: " + error.getMessage());
                    handleError("Error loading patients: " + error.getMessage());
                }
            });
    }

    @Override
    public void onPatientClick(PatientModel patient) {
        // Create intent to open MapActivity with patient's location
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("patient_id", patient.getId());
        intent.putExtra("patient_name", patient.getName());
        intent.putExtra("patient_lat", patient.getLatitude());
        intent.putExtra("patient_lon", patient.getLongitude());
        startActivity(intent);
    }

    private void handleNoNetwork() {
        runOnUiThread(() -> {
            adapter.setAlerts(new ArrayList<>(), new ArrayList<>());
            findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            Toast.makeText(this, "No internet connection. Please check your network.", 
                 Toast.LENGTH_SHORT).show();
        });
    }

    private void handleError(String error) {
        runOnUiThread(() -> {
            adapter.setAlerts(new ArrayList<>(), new ArrayList<>());
            findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }

    private void updateUI(List<PatientModel> patientList) {
        List<LocationHistoryModel> alerts = new ArrayList<>();
        
        adapter.setAlerts(alerts, patientList);
        findViewById(R.id.loadingProgress).setVisibility(View.GONE);
        
        if (alerts.isEmpty()) {
            findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
            Log.d("NotificationActivity", "Showing empty view");
        } else {
            findViewById(R.id.emptyView).setVisibility(View.GONE);
            Log.d("NotificationActivity", "Showing patient list with " + patientList.size() + " items");
        }
    }
} 