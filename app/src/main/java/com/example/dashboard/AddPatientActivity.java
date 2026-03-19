package com.example.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.util.Log;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.dashboard.models.PatientModel;
import com.example.dashboard.utils.FirebaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class AddPatientActivity extends AppCompatActivity {
    private static final String TAG = "AddPatientActivity";
    private TextInputEditText deviceIdInput, patientNameInput;
    private MaterialButton registerButton;
    private FirebaseDatabase database;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        // Initialize Firebase Database with correct URL
        database = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
        
        // Initialize views
        initializeViews();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering patient...");
        progressDialog.setCancelable(false);

        // Set click listener for register button
        registerButton.setOnClickListener(v -> registerPatient());
    }

    private void initializeViews() {
        deviceIdInput = findViewById(R.id.deviceIdInput);
        patientNameInput = findViewById(R.id.patientNameInput);
        registerButton = findViewById(R.id.registerButton);
        
        // Check if required views are found
        if (deviceIdInput == null || patientNameInput == null || registerButton == null) {
            Log.e(TAG, "Required views not found in layout!");
            Toast.makeText(this, "Error: Required views not found. Please check layout file.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Add debug button to check Firebase data
        MaterialButton debugButton = findViewById(R.id.debugButton);
        if (debugButton != null) {
            debugButton.setOnClickListener(v -> checkFirebaseData());
        }

        // Add device ID validation and suggestions
        deviceIdInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String deviceId = s.toString().trim();
                if (deviceId.length() == 9 && deviceId.startsWith("G7T")) {
                    // Check if this device exists in Firebase
                    checkDeviceExists(deviceId);
                }
            }
        });
    }

    private void checkFirebaseData() {
        String deviceId = deviceIdInput.getText().toString().trim();
        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, "Please enter a device ID first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Checking Firebase data...");
        progressDialog.show();

        // Check devices path
        DatabaseReference deviceRef = database.getReference("devices").child(deviceId);
        deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                StringBuilder debugInfo = new StringBuilder();
                debugInfo.append("Device ID: ").append(deviceId).append("\n");
                debugInfo.append("Device exists: ").append(deviceSnapshot.exists()).append("\n");
                
                if (deviceSnapshot.exists()) {
                    debugInfo.append("Device data:\n");
                    for (DataSnapshot child : deviceSnapshot.getChildren()) {
                        debugInfo.append("  ").append(child.getKey()).append(": ").append(child.getValue()).append("\n");
                    }
                } else {
                    debugInfo.append("No device data found in /devices/").append(deviceId).append("\n");
                }

                // Check patients path
                DatabaseReference patientRef = database.getReference("patients").child(deviceId);
                patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot patientSnapshot) {
                        debugInfo.append("Patient exists: ").append(patientSnapshot.exists()).append("\n");
                        
                        progressDialog.dismiss();
                        
                        // Show debug info in a dialog
                        new androidx.appcompat.app.AlertDialog.Builder(AddPatientActivity.this)
                            .setTitle("Firebase Debug Info")
                            .setMessage(debugInfo.toString())
                            .setPositiveButton("OK", null)
                            .show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressDialog.dismiss();
                        Toast.makeText(AddPatientActivity.this, "Error checking patient data: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(AddPatientActivity.this, "Error checking device data: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkDeviceExists(String deviceId) {
        DatabaseReference deviceRef = database.getReference("devices").child(deviceId);
        deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                if (deviceSnapshot.exists()) {
                    deviceIdInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        getResources().getColor(R.color.green_500)));
                    Toast.makeText(AddPatientActivity.this, 
                        "✅ Device " + deviceId + " found and active!", Toast.LENGTH_SHORT).show();
                } else {
                    deviceIdInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        getResources().getColor(R.color.red_500)));
                    Toast.makeText(AddPatientActivity.this, 
                        "⚠️ Device " + deviceId + " not found. Make sure device is powered on and connected.", 
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                deviceIdInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.gray_500)));
            }
        });
    }

    private void registerPatient() {
        String deviceId = deviceIdInput.getText().toString().trim();
        String patientName = patientNameInput.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(deviceId) || TextUtils.isEmpty(patientName)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate device ID format (should be G7T followed by 6 characters)
        if (!deviceId.matches("G7T[A-Z0-9]{6}")) {
            Toast.makeText(this, "Invalid device ID format. Should be G7T followed by 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Get current user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // First check if device exists and is sending data
        DatabaseReference deviceRef = database.getReference("devices").child(deviceId);
        deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                Log.d(TAG, "Device data snapshot: " + deviceSnapshot.toString());
                Log.d(TAG, "Device exists: " + deviceSnapshot.exists());
                
                if (!deviceSnapshot.exists()) {
                    // Device not found - let's create a placeholder device entry
                    Log.d(TAG, "Device not found, creating placeholder entry");
                    createPlaceholderDevice(deviceId, patientName, userId);
                    return;
                }

                try {
                    // Create patient data
                    Map<String, Object> patientData = new HashMap<>();
                    patientData.put("deviceId", deviceId);
                    patientData.put("name", patientName);
                    patientData.put("userId", userId);
                    patientData.put("age", 0);
                    patientData.put("id", deviceId); // Ensure id is set to deviceId
                    patientData.put("birthdate", ""); // Add default values
                    patientData.put("gender", ""); // Add default values

                    // Get device data with proper type handling
                    try {
                        // Handle latitude (stored as String)
                        Object latValue = deviceSnapshot.child("latitude").getValue();
                        if (latValue != null) {
                            if (latValue instanceof String) {
                                patientData.put("latitude", Double.parseDouble((String) latValue));
                            } else if (latValue instanceof Double) {
                                patientData.put("latitude", (Double) latValue);
                            }
                        }

                        // Handle longitude (stored as String)
                        Object lngValue = deviceSnapshot.child("longitude").getValue();
                        if (lngValue != null) {
                            if (lngValue instanceof String) {
                                patientData.put("longitude", Double.parseDouble((String) lngValue));
                            } else if (lngValue instanceof Double) {
                                patientData.put("longitude", (Double) lngValue);
                            }
                        }

                        // Handle timestamp (stored as Long)
                        Object timeValue = deviceSnapshot.child("timestamp").getValue();
                        if (timeValue != null) {
                            if (timeValue instanceof Long) {
                                patientData.put("timestamp", timeValue);
                            } else if (timeValue instanceof String) {
                                patientData.put("timestamp", Long.parseLong((String) timeValue));
                            } else if (timeValue instanceof Integer) {
                                patientData.put("timestamp", ((Integer) timeValue).longValue());
                            }
                        }

                        // Handle inDanger (stored as Boolean)
                        Object dangerValue = deviceSnapshot.child("inDanger").getValue();
                        patientData.put("inDanger", dangerValue != null ? (Boolean) dangerValue : false);

                        Log.d(TAG, "Prepared patient data: " + patientData.toString());

                        // Save to patients node
                        database.getReference("patients")
                            .child(deviceId)
                            .setValue(patientData)
                            .addOnSuccessListener(aVoid -> {
                                // After successful save to patients, add to user's patients list
                                database.getReference("users")
                                    .child(userId)
                                    .child("patients")
                                    .child(deviceId)
                                    .setValue(true)
                                    .addOnSuccessListener(aVoid2 -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddPatientActivity.this, 
                                            "Patient registered successfully", 
                                            Toast.LENGTH_SHORT).show();
                                        
                                        // Return result to MainActivity
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("deviceId", deviceId);
                                        resultIntent.putExtra("patientName", patientName);
                                        setResult(RESULT_OK, resultIntent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Log.e(TAG, "Error updating user's patients list", e);
                                        Toast.makeText(AddPatientActivity.this,
                                            "Error updating user's patients: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    });
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Log.e(TAG, "Error saving patient data", e);
                                Toast.makeText(AddPatientActivity.this,
                                    "Error saving patient: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            });
                    } catch (NumberFormatException e) {
                        throw new Exception("Error converting data types: " + e.getMessage());
                    }

                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error creating patient data", e);
                    Toast.makeText(AddPatientActivity.this,
                        "Error creating patient data: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(AddPatientActivity.this,
                    "Database error: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createPlaceholderDevice(String deviceId, String patientName, String userId) {
        // Create a placeholder device entry with current GPS coordinates
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("latitude", 8.158371); // Use coordinates from your device display
        deviceData.put("longitude", 125.123023);
        deviceData.put("satellites", 8);
        deviceData.put("hdop", 8.50);
        deviceData.put("inDanger", false);
        deviceData.put("timestamp", System.currentTimeMillis());

        database.getReference("devices").child(deviceId).setValue(deviceData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Placeholder device created successfully");
                // Now create the patient
                createPatientFromDevice(deviceId, patientName, userId, deviceData);
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e(TAG, "Error creating placeholder device", e);
                Toast.makeText(AddPatientActivity.this,
                    "Error creating device entry: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
    }

    private void createPatientFromDevice(String deviceId, String patientName, String userId, Map<String, Object> deviceData) {
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("deviceId", deviceId);
        patientData.put("name", patientName);
        patientData.put("userId", userId);
        patientData.put("age", 0);
        patientData.put("id", deviceId); // Ensure id is set to deviceId
        patientData.put("birthdate", ""); // Add default values
        patientData.put("gender", ""); // Add default values
        patientData.put("latitude", deviceData.get("latitude"));
        patientData.put("longitude", deviceData.get("longitude"));
        patientData.put("timestamp", deviceData.get("timestamp"));
        patientData.put("inDanger", deviceData.get("inDanger"));

        // Save to patients node
        database.getReference("patients")
            .child(deviceId)
            .setValue(patientData)
            .addOnSuccessListener(aVoid -> {
                // Add to user's patients list
                database.getReference("users")
                    .child(userId)
                    .child("patients")
                    .child(deviceId)
                    .setValue(true)
                    .addOnSuccessListener(aVoid2 -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddPatientActivity.this, 
                            "Patient registered successfully (device created)", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Return result to MainActivity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("deviceId", deviceId);
                        resultIntent.putExtra("patientName", patientName);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error updating user's patients list", e);
                        Toast.makeText(AddPatientActivity.this,
                            "Error updating user's patients: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    });
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Log.e(TAG, "Error saving patient data", e);
                Toast.makeText(AddPatientActivity.this,
                    "Error saving patient: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
} 