package com.example.dashboard.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.dashboard.models.PatientModel;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;
import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
<<<<<<< HEAD
import com.example.dashboard.models.LocationHistoryModel;
// Firebase native callbacks
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.ServerValue;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private static FirebaseAuth auth;
    private static FirebaseDatabase firebaseDatabase;

    public static void initializeFirebase(Context context) {
        Log.d(TAG, "Initializing Firebase");
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.d(TAG, "Firebase App initialized");
            }
            
            auth = FirebaseAuth.getInstance();
            firebaseDatabase = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
            
            // Enable offline persistence
            try {
                firebaseDatabase.setPersistenceEnabled(true);
                Log.d(TAG, "Firebase persistence enabled");
            } catch (DatabaseException e) {
                Log.w(TAG, "Database persistence already enabled");
            }

            // Set keep sync
            firebaseDatabase.getReference("patients").keepSynced(true);
            
            Log.d(TAG, "Firebase fully initialized");
            
            // Test connection
            DatabaseReference connectedRef = firebaseDatabase.getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                    Log.d(TAG, "Firebase connection state: " + (connected ? "connected" : "disconnected"));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase connection listener cancelled: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage(), e);
        }
    }

    private static FirebaseAuth getAuth() {
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        return auth;
    }

    private static FirebaseDatabase getDatabase() {
        if (firebaseDatabase == null) {
            // Set the specific database URL directly
            firebaseDatabase = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
            try {
                firebaseDatabase.setPersistenceEnabled(true);
            } catch (DatabaseException e) {
                Log.w(TAG, "Database persistence already enabled");
            }
            Log.d(TAG, "Firebase Database initialized with URL: " + firebaseDatabase.getReference().toString());
        }
        return firebaseDatabase;
    }

    public static void savePatient(PatientModel patient, OnDatabaseListener listener) {
        Log.d(TAG, "Starting savePatient method");
        
        // Check if user is authenticated
        FirebaseUser currentUser = getAuth().getCurrentUser();
        if (currentUser == null) {
            String error = "User not authenticated";
            Log.e(TAG, error);
            listener.onError(error);
            return;
        }

        Log.d(TAG, "Current user ID: " + currentUser.getUid());
        
        // Validate patient object
        if (patient == null) {
            String error = "Patient object cannot be null";
            Log.e(TAG, error);
            listener.onError(error);
            return;
        }

        // Get database instance with correct URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference patientsRef = database.getReference().child("patients");
        Log.d(TAG, "Database reference obtained: " + patientsRef.toString());

        // Generate a new key if patient doesn't have one
        String patientId = patient.getId();
        if (patientId == null || patientId.isEmpty()) {
            patientId = patientsRef.push().getKey();
            patient.setId(patientId);
            Log.d(TAG, "Generated new patient ID: " + patientId);
        }

        // Add user ID to patient data for reference
        patient.setUserId(currentUser.getUid());
        
        final String finalPatientId = patientId;
        Log.d(TAG, "Attempting to save patient with ID: " + finalPatientId);

        // Create a map of patient data
        Map<String, Object> patientValues = new HashMap<>();
        patientValues.put("id", patient.getId());
        patientValues.put("deviceId", patient.getDeviceId());
        patientValues.put("userId", patient.getUserId());
        patientValues.put("name", patient.getName());
        patientValues.put("age", patient.getAge());
        patientValues.put("birthdate", patient.getBirthdate());
        patientValues.put("gender", patient.getGender());
        patientValues.put("latitude", patient.getLatitude());
        patientValues.put("longitude", patient.getLongitude());
        patientValues.put("radius", patient.getRadius());
        patientValues.put("safeLat", patient.getSafeLat());
        patientValues.put("safeLng", patient.getSafeLng());
        patientValues.put("timestamp", ServerValue.TIMESTAMP);

        // Validate device ID exists before saving patient
        String deviceId = patient.getDeviceId();
        if (deviceId == null || deviceId.trim().isEmpty()) {
            String error = "Device ID is required to register a patient";
            Log.e(TAG, error);
            listener.onError(error);
            return;
        }

        DatabaseReference deviceRef = database.getReference("devices").child(deviceId);

        // 1) Verify the device exists under /devices/{deviceId}
        deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnap) {
                if (!deviceSnap.exists()) {
                    String error = "Device ID not found at /devices/" + deviceId + ". Power the device or check ID.";
                    Log.e(TAG, error);
                    listener.onError(error);
                    return;
                }

                // 2) Verify Firebase connection then save patient
                DatabaseReference connectedRef = database.getReference(".info/connected");
                connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean connected = snapshot.getValue(Boolean.class);
                        if (!Boolean.TRUE.equals(connected)) {
                            String error = "No connection to Firebase";
                            Log.e(TAG, error);
                            listener.onError(error);
                            return;
                        }

                        // 3) Save patient
                        patientsRef.child(finalPatientId).setValue(patientValues)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Patient saved successfully to Firebase");

                                // 4) Create/Update mapping: /device_patient_mapping/{deviceId} = patientId
                                database.getReference("device_patient_mapping")
                                        .child(deviceId)
                                        .setValue(finalPatientId);

                                // 5) Verify by reading back
                                patientsRef.child(finalPatientId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            Log.d(TAG, "Patient data verified in database");
                                            listener.onSuccess();
                                        } else {
                                            String error = "Patient save verification failed";
                                            Log.e(TAG, error);
                                            listener.onError(error);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        String error = "Error verifying patient save: " + databaseError.getMessage();
                                        Log.e(TAG, error);
                                        listener.onError(error);
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                String error = "Error saving patient: " + e.getMessage();
                                Log.e(TAG, error, e);
                                listener.onError(error);
                            });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        String errorMsg = "Error checking connection: " + error.getMessage();
                        Log.e(TAG, errorMsg);
                        listener.onError(errorMsg);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String errorMsg = "Error checking device ID: " + error.getMessage();
                Log.e(TAG, errorMsg);
                listener.onError(errorMsg);
            }
        });
    }

    public static void addPatient(String userId, PatientModel patient, CompletionListener onCompleteListener) {
        DatabaseReference patientsRef = getDatabase().getReference()
            .child("users")
            .child(userId)
            .child("patients");
            
        // Generate a new key if patient doesn't have one
        String patientId = patient.getId();
        if (patientId == null || patientId.isEmpty()) {
            patientId = patientsRef.push().getKey();
            patient.setId(patientId);
        }

        patientsRef.child(patientId).setValue(patient, onCompleteListener);
=======

public class FirebaseHelper {
    private static FirebaseAuth auth;
    private static DatabaseReference database;
    private static FirebaseDatabase firebaseDatabase;
    private static final String TAG = "FirebaseHelper";
    private static boolean isInitialized = false;
    private static final String DATABASE_URL = "https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app";

    public static void initializeFirebase(Context context) {
        if (!isInitialized) {
            try {
                // Initialize Firebase App first
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context);
                    Log.d(TAG, "Firebase App initialized");
                }

                // Initialize Auth
                auth = FirebaseAuth.getInstance();
                
                // Initialize Database with specific URL
                firebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL);
                firebaseDatabase.setPersistenceEnabled(true);
                database = firebaseDatabase.getReference();
                
                // Add connection state listener
                DatabaseReference connectedRef = firebaseDatabase.getReference(".info/connected");
                connectedRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        Log.d(TAG, "Firebase connection state: " + (connected ? "connected" : "disconnected"));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase connection check failed: " + error.getMessage());
                    }
                });

                isInitialized = true;
                Log.d(TAG, "Firebase successfully initialized with database URL: " + DATABASE_URL);

            } catch (Exception e) {
                Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
                e.printStackTrace();
                isInitialized = false;
            }
        }
    }

    public static DatabaseReference getDatabase() {
        if (database == null) {
            try {
                firebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL);
                database = firebaseDatabase.getReference();
            } catch (Exception e) {
                Log.e(TAG, "Error getting database reference: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return database;
    }

    public static FirebaseAuth getAuth() {
        if (auth == null) {
            try {
                auth = FirebaseAuth.getInstance();
            } catch (Exception e) {
                Log.e(TAG, "Error getting auth instance: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return auth;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
    }

    // Update loginUser method
    public static void loginUser(String email, String password, OnAuthListener listener) {
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            listener.onError("Email and password cannot be empty");
            return;
        }

        try {
            if (getAuth() == null) {
                listener.onError("Firebase Authentication not initialized");
                return;
            }

            getAuth().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult != null && authResult.getUser() != null) {
                        Log.d(TAG, "Login successful for user: " + email);
                        listener.onSuccess(authResult.getUser());
                    } else {
                        Log.e(TAG, "Login failed: auth result or user is null");
                        listener.onError("Authentication failed");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Login error: " + e.getMessage(), e);
                    String errorMessage;
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        errorMessage = "Invalid email or password";
                    } else if (e instanceof FirebaseAuthInvalidUserException) {
                        errorMessage = "No account found with this email";
                    } else {
                        errorMessage = "Login failed: " + e.getMessage();
                    }
                    listener.onError(errorMessage);
                });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during login: " + e.getMessage(), e);
            listener.onError("Unexpected error occurred");
        }
    }

    public static void registerUser(String email, String password, String name, String address, OnAuthListener listener) {
        getAuth().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    // Send verification email
                    user.sendEmailVerification()
                        .addOnSuccessListener(aVoid -> {
                                // Save additional user info to database
<<<<<<< HEAD
                                DatabaseReference userRef = getDatabase().getReference().child("users").child(user.getUid());
=======
                                DatabaseReference userRef = getDatabase().child("users").child(user.getUid());
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
                                Map<String, Object> userInfo = new HashMap<>();
                                userInfo.put("name", name);
                                userInfo.put("email", email);
                                userInfo.put("address", address);
                                userInfo.put("emailVerified", false);
                                
                                userRef.setValue(userInfo)
                                    .addOnSuccessListener(unused -> listener.onSuccess(user))
                                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
                        })
                        .addOnFailureListener(e -> listener.onError("Failed to send verification email"));
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Database methods
<<<<<<< HEAD
    public static void updatePatient(String patientId, PatientModel patient, OnDatabaseListener listener) {
        getDatabase().getReference().child("patients").child(patientId).setValue(patient)
=======
    public static void savePatient(PatientModel patient, OnDatabaseListener listener) {
        // Check if user is authenticated
        if (auth.getCurrentUser() == null) {
            listener.onError("User not authenticated");
            return;
        }

        String patientId = getDatabase().child("patients").push().getKey();
        getDatabase().child("patients").child(patientId).setValue(patient)
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> {
                String error = "Error saving patient: " + e.getMessage();
                Log.e(TAG, error);
                listener.onError(error);
            });
    }

    public static void updatePatient(String patientId, PatientModel patient, OnDatabaseListener listener) {
        getDatabase().child("patients").child(patientId).setValue(patient)
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void deletePatient(String patientId, OnDatabaseListener listener) {
<<<<<<< HEAD
        getDatabase().getReference().child("patients").child(patientId).removeValue()
=======
        getDatabase().child("patients").child(patientId).removeValue()
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void searchPatients(String query, OnPatientsLoadedListener listener) {
<<<<<<< HEAD
        getDatabase().getReference().child("patients")
=======
        getDatabase().child("patients")
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
            .orderByChild("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<PatientModel> patients = new ArrayList<>();
                    for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                        PatientModel patient = patientSnapshot.getValue(PatientModel.class);
                        if (patient != null) {
                            patients.add(patient);
                        }
                    }
                    listener.onPatientsLoaded(patients);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onError(error.getMessage());
                }
            });
    }

    public static DatabaseReference getPatientsReference() {
        // Check if user is authenticated
<<<<<<< HEAD
        FirebaseUser currentUser = getAuth().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            return null;
        }
        return getDatabase().getReference().child("patients");
=======
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            return null;
        }
        return getDatabase().child("patients");
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
    }

    // Add this method to check if user is already logged in
    public static FirebaseUser getCurrentUser() {
        return getAuth().getCurrentUser();
    }

    public static void checkFirebaseConnection(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d(TAG, "Firebase not initialized, initializing now");
                FirebaseApp.initializeApp(context);
            }
            
<<<<<<< HEAD
            DatabaseReference connectedRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference(".info/connected");
=======
            DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    Log.d(TAG, "Firebase connection state: " + (connected ? "connected" : "disconnected"));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Firebase connection check failed: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase connection: " + e.getMessage());
        }
    }

    // Add this method to check authentication status
    public static boolean isUserAuthenticated() {
        return auth != null && auth.getCurrentUser() != null;
    }

    // Interfaces for callbacks
    public interface OnAuthListener {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    public interface OnDatabaseListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnPatientsLoadedListener {
        void onPatientsLoaded(List<PatientModel> patients);
        void onError(String error);
    }

    public static void saveLocationHistory(LocationHistoryModel location, OnDatabaseListener listener) {
        if (auth.getCurrentUser() == null) {
            listener.onError("User not authenticated");
            return;
        }

        DatabaseReference historyRef = getDatabase()
            .getReference()
            .child("location_history")
            .child(location.getPatientId())
            .push();
        
        location.setId(historyRef.getKey());
        
        historyRef.setValue(location)
            .addOnSuccessListener(aVoid -> {
                // If this is an alert, also update the alerts node
                if (location.isAlert()) {
                    DatabaseReference alertRef = getDatabase()
                        .getReference()
                        .child("alerts")
                        .child(location.getPatientId())
                        .push();
                    alertRef.setValue(location);
                }
                listener.onSuccess();
            })
            .addOnFailureListener(e -> {
                String error = "Error saving location history: " + e.getMessage();
                Log.e(TAG, error);
                listener.onError(error);
            });
    }

    public static void getLocationHistory(String patientId, long startTime, long endTime, OnLocationHistoryListener listener) {
        getDatabase()
            .getReference()
            .child("location_history")
            .child(patientId)
            .orderByChild("timestamp")
            .startAt(startTime)
            .endAt(endTime)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<LocationHistoryModel> history = new ArrayList<>();
                    for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                        LocationHistoryModel location = locationSnapshot.getValue(LocationHistoryModel.class);
                        if (location != null) {
                            history.add(location);
                        }
                    }
                    listener.onHistoryLoaded(history);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onError(error.getMessage());
                }
            });
    }

    public static void getAlerts(String patientId, OnLocationHistoryListener listener) {
        getDatabase()
            .getReference()
            .child("alerts")
            .child(patientId)
            .orderByChild("timestamp")
            .limitToLast(50) // Get last 50 alerts
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<LocationHistoryModel> alerts = new ArrayList<>();
                    for (DataSnapshot alertSnapshot : snapshot.getChildren()) {
                        LocationHistoryModel alert = alertSnapshot.getValue(LocationHistoryModel.class);
                        if (alert != null) {
                            alerts.add(alert);
                        }
                    }
                    listener.onHistoryLoaded(alerts);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onError(error.getMessage());
                }
            });
    }

    // Add new interface for location history callbacks
    public interface OnLocationHistoryListener {
        void onHistoryLoaded(List<LocationHistoryModel> history);
        void onError(String error);
    }

    // Method to fetch device data from Firebase
    public static void fetchDeviceData(String deviceId, OnDeviceDataListener listener) {
        if (deviceId == null || deviceId.isEmpty()) {
            listener.onError("Device ID is required");
            return;
        }

        DatabaseReference deviceRef = getDatabase()
            .getReference("devices")
            .child(deviceId);

        deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> deviceData = new HashMap<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        deviceData.put(child.getKey(), child.getValue());
                    }
                    listener.onDeviceDataLoaded(deviceData);
                } else {
                    listener.onError("Device not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Error fetching device data: " + error.getMessage());
            }
        });
    }

    public interface OnDeviceDataListener {
        void onDeviceDataLoaded(Map<String, Object> deviceData);
        void onError(String error);
    }

    public static void setupDeviceLocationTracking(String deviceId, OnDatabaseListener listener) {
        if (deviceId == null || deviceId.isEmpty()) {
            listener.onError("Device ID is required");
            return;
        }

        try {
            Log.d(TAG, "Setting up device tracking for device ID: " + deviceId);
            
            // Listen to device location updates
            DatabaseReference deviceRef = getDatabase()
                .getReference()
                .child("devices")
                .child(deviceId);

            deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d(TAG, "Device data snapshot: " + snapshot.getValue());
                    
                    if (!snapshot.exists()) {
                        Log.e(TAG, "Device not found in database: " + deviceId);
                        listener.onError("Device not found in database");
                        return;
                    }

                    try {
                        // Get device data with type checking
                        Object latObj = snapshot.child("latitude").getValue();
                        Object lonObj = snapshot.child("longitude").getValue();
                        Object timestampObj = snapshot.child("timestamp").getValue();
                        Object inDangerObj = snapshot.child("inDanger").getValue();

                        Log.d(TAG, "Raw values - lat: " + latObj + ", lon: " + lonObj + 
                              ", timestamp: " + timestampObj + ", inDanger: " + inDangerObj);

                        // Convert data with proper type checking
                        Double latitude = (latObj instanceof Number) ? ((Number) latObj).doubleValue() : null;
                        Double longitude = (lonObj instanceof Number) ? ((Number) lonObj).doubleValue() : null;
                        
                        // Handle timestamp that might be string or number
                        Long timestamp;
                        if (timestampObj instanceof Number) {
                            timestamp = ((Number) timestampObj).longValue();
                        } else if (timestampObj instanceof String) {
                            try {
                                timestamp = Long.parseLong((String) timestampObj);
                            } catch (NumberFormatException e) {
                                timestamp = System.currentTimeMillis();
                            }
                        } else {
                            timestamp = System.currentTimeMillis();
                        }
                        
                        Boolean inDanger = (inDangerObj instanceof Boolean) ? (Boolean) inDangerObj : false;

                        if (latitude == null || longitude == null) {
                            Log.e(TAG, "Invalid location data - lat: " + latitude + ", lon: " + longitude);
                            listener.onError("Invalid location data from device");
                            return;
                        }

                        Log.d(TAG, "Processed values - lat: " + latitude + ", lon: " + longitude + 
                              ", timestamp: " + timestamp + ", inDanger: " + inDanger);

                        // Update patient location
                        DatabaseReference patientRef = getDatabase()
                            .getReference()
                            .child("patients")
                            .child(deviceId);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("latitude", latitude);
                        updates.put("longitude", longitude);
                        updates.put("lastUpdated", timestamp);
                        updates.put("inDanger", inDanger);
                        updates.put("deviceId", deviceId);

                        Log.d(TAG, "Updating patient with data: " + updates);

                        patientRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated patient location");
                                listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update patient location: " + e.getMessage());
                                listener.onError("Failed to update patient location: " + e.getMessage());
                            });
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing device data: " + e.getMessage(), e);
                        listener.onError("Error processing device data: " + e.getMessage() + ". Please ensure the device is sending correct data format.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Device tracking cancelled: " + error.getMessage());
                    listener.onError("Device tracking cancelled: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Setup error: " + e.getMessage(), e);
            listener.onError("Setup error: " + e.getMessage());
        }
    }
} 