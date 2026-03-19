package com.example.dashboard; // Replace with your actual package name

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.view.ViewGroup;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.app.AlertDialog;
import android.view.Gravity;
import androidx.core.view.GravityCompat;
import com.example.dashboard.adapters.PatientListAdapter;
import com.example.dashboard.models.PatientModel;
<<<<<<< HEAD
import com.example.dashboard.models.LocationHistoryModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
=======
import com.google.firebase.auth.FirebaseAuth;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
import com.example.dashboard.utils.FirebaseHelper;
import com.example.dashboard.utils.NetworkUtils;
import com.example.dashboard.utils.SessionManager;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.util.Log;
<<<<<<< HEAD
import java.util.Map;
import android.text.TextUtils;
import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import com.google.android.material.button.MaterialButton;
import android.view.Window;
import java.util.HashMap;
import com.example.dashboard.services.LocationTrackingService;
import com.example.dashboard.utils.NotificationHelper;
import androidx.core.app.NotificationManagerCompat;
import android.os.Handler;

=======
import com.google.firebase.database.DatabaseReference;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

public class MainActivity extends AppCompatActivity implements PatientListAdapter.OnPatientActionListener {

    private static final String TAG = "MainActivity";
<<<<<<< HEAD
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_LOCATION_REQUEST_CODE = 1002;
    private static final int ADD_PATIENT_REQUEST_CODE = 1003;
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

    private RecyclerView recyclerView;
    private PatientListAdapter adapter;
    private List<PatientModel> patientList;
    private EditText searchEditText;
    private Button cancelButton;
    private ImageButton addButton, updateButton, readButton;
    private ImageButton prevButton, nextButton;
    private TextView pageNumberTextView;
    private int currentPage = 1;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private SessionManager sessionManager;
    private Dialog activeUpdateDialog;

    // Track last known safe/unsafe status per device to avoid duplicate alerts & history spam
    private Map<String, Boolean> deviceSafeStatus = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is logged in
        if (FirebaseHelper.getCurrentUser() == null) {
            Log.d(TAG, "No user logged in, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
<<<<<<< HEAD
        
        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this);
        
        // Check notification permissions
        checkNotificationPermissions();
        
        // Start background monitoring service
        LocationTrackingService.startService(this);
        
        // Background monitoring service starts automatically
        // Notifications will be sent when patients leave safe zones
        Log.d(TAG, "Starting LocationTrackingService...");
        LocationTrackingService.startService(this);
        
        // Check service status on startup
        boolean serviceRunning = LocationTrackingService.isServiceRunning(this);
        Log.d(TAG, "LocationTrackingService running on startup: " + serviceRunning);
        if (!serviceRunning) {
            Log.w(TAG, "Service not running, attempting to start...");
            LocationTrackingService.startService(this);
        }
        
        // Test Firebase connectivity on startup
        testFirebaseConnectivity();
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

        setContentView(R.layout.activity_main);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Remove content insets to allow search field to expand fully
        toolbar.setContentInsetsAbsolute(0, 0);
        if (toolbar.getNavigationIcon() != null) {
            toolbar.setContentInsetStartWithNavigation(0);
        }

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        
        // Setup ActionBarDrawerToggle
        actionBarDrawerToggle = new ActionBarDrawerToggle(
            this, 
            drawerLayout, 
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // Setup navigation header
        View headerView = navigationView.getHeaderView(0);
        TextView userEmailText = headerView.findViewById(R.id.userEmail);
        userEmailText.setText(sessionManager.getUserEmail());

        // Setup navigation item selection
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                showLogoutDialog();
            } else if (id == R.id.nav_settings) {
                // Open alert settings screen
                Intent settingsIntent = new Intent(MainActivity.this, AlertSettingsActivity.class);
                startActivity(settingsIntent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        ImageButton clearButton = findViewById(R.id.clearButton);
        addButton = findViewById(R.id.addButton);
        updateButton = findViewById(R.id.updateButton);
        readButton = findViewById(R.id.readButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pageNumberTextView = findViewById(R.id.pageNumberTextView);

        // Initialize patient list
        patientList = new ArrayList<>();

        // Setup RecyclerView with adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientListAdapter(this, patientList, this);
        recyclerView.setAdapter(adapter);

        // Load patients from Firebase
        loadPatientsFromFirebase();

        // Setup search and clear functionality
        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            filterPatients("");
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                runOnUiThread(() -> {
                    clearButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                });
                filterPatients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Pagination buttons
        nextButton.setOnClickListener(v -> {
            int totalPages = (int) Math.ceil(patientList.size() / 10.0);
            if (currentPage < totalPages) {
                currentPage++;
                updatePageControls();
            }
        });

        prevButton.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePageControls();
            }
        });

        // Update add button click listener
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddPatientActivity.class);
            startActivityForResult(intent, ADD_PATIENT_REQUEST_CODE);
        });

        updateButton.setOnClickListener(v -> {
            if (patientList.isEmpty()) {
                Toast.makeText(this, "No patients available to update", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show patient selection dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Patient to Update");

            // Create list of patient names
            String[] patientNames = new String[patientList.size()];
            for (int i = 0; i < patientList.size(); i++) {
                patientNames[i] = patientList.get(i).getName();
            }

            builder.setItems(patientNames, (dialog, which) -> {
                PatientModel selectedPatient = patientList.get(which);
                showUpdatePatientDialog(selectedPatient);
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        });

        readButton.setOnClickListener(v -> {
            // Show all patients in the PatientDetailsActivity
            if (!patientList.isEmpty()) {
                StringBuilder allPatientsInfo = new StringBuilder();
                for (PatientModel patient : patientList) {
                    allPatientsInfo.append("Patient ID: ").append(patient.getId())
                            .append("\nName: ").append(patient.getName())
                            .append("\nAge: ").append(patient.getAge())
                            .append("\n\n");
                }

                Intent intent = new Intent(this, PatientDetailsActivity.class);
                intent.putExtra("show_all_patients", true);
                intent.putExtra("all_patients_info", allPatientsInfo.toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "No patients registered", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup notification button
        ImageButton notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        // Setup refresh button
        ImageButton refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> {
            // Show refresh animation
            refreshButton.animate().rotationBy(360).setDuration(1000).start();
            
            // Reload patients from Firebase
            loadPatientsFromFirebase();
            
            Toast.makeText(this, "Refreshing patient data...", Toast.LENGTH_SHORT).show();
        });

        // Map functionality removed - background monitoring handles alerts automatically

        // Initialize OpenStreetMap

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verify authentication state on start
        if (FirebaseHelper.getCurrentUser() == null) {
            Log.d(TAG, "User session expired, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPatientsFromFirebase();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verify authentication state on start
        if (FirebaseHelper.getCurrentUser() == null) {
            Log.d(TAG, "User session expired, redirecting to LoginActivity");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // Implement OnPatientActionListener methods
    @Override
    public void onNotesClick(PatientModel patient) {
        Intent intent = new Intent(this, PatientDetailsActivity.class);
        intent.putExtra("patient_name", patient.getName());
        intent.putExtra("patient_age", patient.getAge());
        intent.putExtra("patient_id", patient.getId());
        startActivity(intent);
    }

    @Override
    public void onRefreshClick(PatientModel patient) {
        // Create a safe zone for this patient based on their current location
        if (patient.getLatitude() != 0.0 && patient.getLongitude() != 0.0) {
            Log.d(TAG, "Creating safe zone for " + patient.getName() + " at current location: " + 
                  patient.getLatitude() + ", " + patient.getLongitude());
            
            // Create a 30-meter safe zone around the patient's current location
            createSafeZoneForPatient(patient, patient.getLatitude(), patient.getLongitude(), 30);
        } else {
            Log.w(TAG, "Cannot create safe zone: patient " + patient.getName() + " has no location data");
            Toast.makeText(this, "Patient " + patient.getName() + " has no location data. Please wait for GPS signal.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeleteClick(PatientModel patient) {
        if (patient == null) {
            Toast.makeText(this, "Error: Patient data is null", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Debug logging
        Log.d(TAG, "Delete clicked for patient");
        Log.d(TAG, "Patient ID: " + patient.getId());
        Log.d(TAG, "Device ID: " + patient.getDeviceId());
        Log.d(TAG, "Patient Name: " + patient.getName());
        
        // Prefer explicit, robust deletion that works even if 'id' field is missing.
        String tmpPatientId = patient.getId();
        if (tmpPatientId == null || tmpPatientId.isEmpty()) {
            // Fallback to deviceId, because in this app id == deviceId by design
            tmpPatientId = patient.getDeviceId();
            Log.d(TAG, "Using deviceId as patientId: " + tmpPatientId);
        }

        if (tmpPatientId == null || tmpPatientId.isEmpty()) {
            Log.e(TAG, "ERROR: Both patient.getId() and patient.getDeviceId() are null or empty!");
                    Toast.makeText(this, "Error: Patient ID is missing", Toast.LENGTH_SHORT).show();
                    return;
                }

        final String patientId = tmpPatientId;
        String patientName = patient.getName();
        if (patientName == null || patientName.isEmpty()) {
            patientName = "Patient " + patientId; // Fallback name
        }
        
        Log.d(TAG, "Final patientId for deletion: " + patientId);
        
        new AlertDialog.Builder(this)
            .setTitle("Delete Patient")
            .setMessage("Are you sure you want to delete " + patientName + "?")
            .setPositiveButton("Yes", (dialog, which) -> {
                Log.d(TAG, "User confirmed deletion for patientId: " + patientId);
                
                // Make effectively final copies for use inside lambdas
                String deviceId = patient.getDeviceId();

                FirebaseDatabase db = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                final String userId = currentUser != null ? currentUser.getUid() : null;

                Log.d(TAG, "Attempting to delete patient from Firebase path: /patients/" + patientId);
                
                // Show loading indicator
                Toast.makeText(MainActivity.this, "Deleting patient...", Toast.LENGTH_SHORT).show();
                
                // Perform deletion directly
                performDeletion(db, patientId, userId, deviceId);
            })
            .setNegativeButton("No", null)
            .show();
    }

    // Update your filter method to work with PatientModel
    private void filterPatients(String searchText) {
        List<PatientModel> filteredList = new ArrayList<>();
        
        if (searchText.isEmpty()) {
            filteredList.addAll(patientList);
        } else {
            String searchLower = searchText.toLowerCase();
            for (PatientModel patient : patientList) {
                if (patient.getName().toLowerCase().contains(searchLower)) {
                    filteredList.add(patient);
                }
            }
        }
        
        adapter = new PatientListAdapter(this, filteredList, this);
        recyclerView.setAdapter(adapter);
    }

    // Example pagination method (you'll need to adapt this to your data source)
    // private List<Patient> getPatientsForPage(int page) {
    //     int pageSize = 10; // Number of items per page
    //     int offset = (page - 1) * pageSize;
    //     // Implement your database query or API call with offset and limit
    //     // to fetch the data for the current page.
    //     return new ArrayList<>(); // Replace with actual data
    // }

    // Add this method to show the add patient dialog
    private void showAddPatientDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_patient);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextInputEditText firstNameInput = dialog.findViewById(R.id.firstNameInput);
        TextInputEditText lastNameInput = dialog.findViewById(R.id.lastNameInput);
        TextInputEditText ageInput = dialog.findViewById(R.id.ageInput);
        TextInputEditText birthdateInput = dialog.findViewById(R.id.birthdateInput);
        AutoCompleteTextView genderInput = dialog.findViewById(R.id.genderInput);
<<<<<<< HEAD
        TextInputEditText deviceIdInput = dialog.findViewById(R.id.deviceIdInput);
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
        TextInputEditText latitudeInput = dialog.findViewById(R.id.latitudeInput);
        TextInputEditText longitudeInput = dialog.findViewById(R.id.longitudeInput);
        Button saveButton = dialog.findViewById(R.id.saveButton);

        // Setup Gender Dropdown
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
            this,
            R.layout.dropdown_item, // Create this layout
            genders
        );
        genderInput.setAdapter(genderAdapter);

        // Make sure the gender field is not editable
        genderInput.setKeyListener(null);

        // Setup Birthdate Picker
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            birthdateInput.setText(dateFormat.format(calendar.getTime()));

            // Calculate age
            int age = calculateAge(calendar.getTimeInMillis());
            ageInput.setText(String.valueOf(age));
        };

        birthdateInput.setOnClickListener(v -> {
            new DatePickerDialog(MainActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        saveButton.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String age = ageInput.getText().toString().trim();
            String birthdate = birthdateInput.getText().toString().trim();
            String gender = genderInput.getText().toString().trim();
<<<<<<< HEAD
            String deviceId = deviceIdInput.getText().toString().trim();
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
            String latitude = latitudeInput.getText().toString().trim();
            String longitude = longitudeInput.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || age.isEmpty() || 
<<<<<<< HEAD
                birthdate.isEmpty() || gender.isEmpty() || deviceId.isEmpty() ||
=======
                birthdate.isEmpty() || gender.isEmpty() || 
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
                latitude.isEmpty() || longitude.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double lat = Double.parseDouble(latitude);
                double lon = Double.parseDouble(longitude);
                
                String fullName = firstName + " " + lastName;
<<<<<<< HEAD
                
                PatientModel newPatient = new PatientModel(
                    deviceId, 
=======
                String patientId = "P" + System.currentTimeMillis();
                
                PatientModel newPatient = new PatientModel(
                    patientId, 
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
                    fullName, 
                    Integer.parseInt(age),
                    birthdate,
                    gender,
                    lat,
                    lon
                );
<<<<<<< HEAD
                newPatient.setDeviceId(deviceId);
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

                FirebaseHelper.savePatient(newPatient, new FirebaseHelper.OnDatabaseListener() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, 
                            "Patient added successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, 
                            "Error adding patient: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for latitude and longitude", 
                    Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private int calculateAge(long birthdateMillis) {
        Calendar birthdate = Calendar.getInstance();
        birthdate.setTimeInMillis(birthdateMillis);
        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - birthdate.get(Calendar.YEAR);
        
        // Adjust age if birthday hasn't occurred this year
        if (today.get(Calendar.DAY_OF_YEAR) < birthdate.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        
        return age;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Perform logout
                sessionManager.logout();
                
                // Navigate to login screen
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("No", null)
            .show();
    }
    
    private void performDeletion(FirebaseDatabase db, String patientId, String userId, String deviceId) {
        // Add timeout handler
        android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final boolean[] operationCompleted = {false};
        
        Runnable timeoutRunnable = () -> {
            if (!operationCompleted[0]) {
                Log.e(TAG, "Deletion timeout - operation took too long");
                operationCompleted[0] = true;
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Deletion timeout - trying alternative method...",
                            Toast.LENGTH_SHORT).show();
                    
                    // Try alternative: remove from local list immediately and sync later
                    removePatientFromLocalList(patientId);
                    
                    // Try fire-and-forget deletion (no callback waiting)
                    DatabaseReference patientRef = db.getReference("patients").child(patientId);
                    patientRef.removeValue(); // Fire and forget
                    
                    if (userId != null) {
                        db.getReference("users").child(userId).child("patients").child(patientId).removeValue();
                    }
                    // KEEP device data and safe zones - device continues to work even if patient is unregistered
                    // This allows the device to be re-registered later with the same device ID
                    if (deviceId != null && !deviceId.isEmpty()) {
                        Log.d(TAG, "Preserving device data and safe zones for deviceId: " + deviceId);
                    }
                    db.getReference("alerts").child(patientId).removeValue();
                    db.getReference("location_history").child(patientId).removeValue();
                    
                    Toast.makeText(MainActivity.this,
                            "Patient deletion initiated (may take a moment to sync)",
                            Toast.LENGTH_SHORT).show();
                });
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 8000); // 8 second timeout
        
        // First, check if patient exists
        DatabaseReference patientRef = db.getReference("patients").child(patientId);
        patientRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Patient exists in Firebase, proceeding with deletion");
                    // Patient exists, proceed with deletion
                    deletePatientFromFirebase(db, patientId, userId, deviceId, timeoutHandler, timeoutRunnable, operationCompleted);
                } else {
                    Log.w(TAG, "Patient does not exist in Firebase, removing from local list only");
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    operationCompleted[0] = true;
                    runOnUiThread(() -> {
                        removePatientFromLocalList(patientId);
                        Toast.makeText(MainActivity.this,
                                "Patient not found in database (removed from local list)",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking if patient exists: " + error.getMessage());
                timeoutHandler.removeCallbacks(timeoutRunnable);
                // Proceed with deletion anyway
                deletePatientFromFirebase(db, patientId, userId, deviceId, timeoutHandler, timeoutRunnable, operationCompleted);
            }
        });
    }
    
    private void deletePatientFromFirebase(FirebaseDatabase db, String patientId, String userId, String deviceId,
                                           android.os.Handler timeoutHandler, Runnable timeoutRunnable, boolean[] operationCompleted) {
        DatabaseReference patientRef = db.getReference("patients").child(patientId);
        Log.d(TAG, "Attempting removeValue() at path: " + patientRef.toString());
        
        patientRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                if (!operationCompleted[0]) {
                    operationCompleted[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    Log.d(TAG, "Successfully deleted patient from /patients/" + patientId);
                    
                    // 2) Remove from user's patients list (fire-and-forget)
                    if (userId != null) {
                        Log.d(TAG, "Deleting from /users/" + userId + "/patients/" + patientId);
                        db.getReference("users")
                            .child(userId)
                            .child("patients")
                            .child(patientId)
                            .removeValue();
                    }

                    // 3) KEEP device data and safe zones - device continues to work even if patient is unregistered
                    // This allows the device to be re-registered later with the same device ID
                    if (deviceId != null && !deviceId.isEmpty()) {
                        Log.d(TAG, "Preserving device data and safe zones for deviceId: " + deviceId);
                        Log.d(TAG, "Device will continue to send data. Safe zones will remain for potential re-registration.");
                    }

                    // 4) Remove alerts and location history (fire-and-forget)
                    Log.d(TAG, "Deleting alerts and location_history for patientId: " + patientId);
                    db.getReference("alerts").child(patientId).removeValue();
                    db.getReference("location_history").child(patientId).removeValue();

                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Patient unregistered successfully. Device data and safe zones preserved.",
                                Toast.LENGTH_LONG).show();
                        
                        // Refresh local list so UI updates immediately
                        loadPatientsFromFirebase();
                    });
                }
            })
            .addOnFailureListener(e -> {
                if (!operationCompleted[0]) {
                    operationCompleted[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    Log.e(TAG, "Failed to delete patient: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Error deleting patient: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
    }
    
    private void removePatientFromLocalList(String patientId) {
        // Remove from local list immediately
        patientList.removeIf(patient -> patientId.equals(patient.getId()) || patientId.equals(patient.getDeviceId()));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadPatientsFromFirebase() {
<<<<<<< HEAD
        Log.d(TAG, "Starting to load patients from Firebase");
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No user logged in");
            return;
        }
        
        Log.d(TAG, "Loading all patients");

        DatabaseReference patientsRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("patients");
            
        patientsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<PatientModel> patientsList = new ArrayList<>();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    PatientModel patient = snapshot.getValue(PatientModel.class);
                    if (patient != null) {
                        // Ensure patient ID is set - use snapshot key if id is missing
                        String patientId = patient.getId();
                        if (patientId == null || patientId.isEmpty()) {
                            // Use snapshot key as patient ID (this is the Firebase key)
                            patientId = snapshot.getKey();
                            patient.setId(patientId);
                        }
                        
                        // Also ensure deviceId is set if missing (fallback to patientId)
                        if (patient.getDeviceId() == null || patient.getDeviceId().isEmpty()) {
                            patient.setDeviceId(patientId);
                        }
                        
                        // Ensure name is set (fallback to deviceId if name is missing)
                        if (patient.getName() == null || patient.getName().isEmpty()) {
                            patient.setName("Patient " + patientId);
                        }
                        
                        // Set up real-time device location listener for each patient
                        setupDeviceLocationListener(patient);
                        patientsList.add(patient);
                    }
                }
                
                // Update UI with patients list
                updatePatientsList(patientsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading patients: " + databaseError.getMessage());
                Log.e(TAG, "Error code: " + databaseError.getCode());
                Log.e(TAG, "Error details: " + databaseError.getDetails());
                Toast.makeText(MainActivity.this, 
                    "Error loading patients: " + databaseError.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDeviceLocationListener(PatientModel patient) {
        if (patient.getDeviceId() == null) return;
        
        DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("devices").child(patient.getDeviceId());
            
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot deviceSnapshot) {
                try {
                    // Update patient location from device
                    Object latObj = deviceSnapshot.child("latitude").getValue();
                    Object lonObj = deviceSnapshot.child("longitude").getValue();
                    Long timestamp = deviceSnapshot.child("timestamp").getValue(Long.class);
                    Boolean inDanger = deviceSnapshot.child("inDanger").getValue(Boolean.class);
                    Integer satellites = deviceSnapshot.child("satellites").getValue(Integer.class);
                    Float hdop = deviceSnapshot.child("hdop").getValue(Float.class);
                    
                    // Handle both string and number types from ESP8266
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
                        Log.d(TAG, "Real-time update for " + patient.getDeviceId() + 
                              ": LAT=" + latitude + ", LNG=" + longitude + 
                              ", DANGER=" + inDanger + ", SAT=" + satellites);
                        
                        // Make variables final for lambda
                        final Double finalLat = latitude;
                        final Double finalLon = longitude;
                        final Boolean finalInDanger = inDanger;
                        final Integer finalSatellites = satellites;
                        final Float finalHdop = hdop;
                        
                        // Check safe zones immediately when we receive location data
                        Log.d(TAG, "🔍 About to check safe zones for " + patient.getName() + " at " + finalLat + ", " + finalLon);
                        checkSafeZones(patient, finalLat, finalLon);
                        
                        // Update patient data in Firebase
                        DatabaseReference patientRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("patients").child(patient.getDeviceId());
                            
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("latitude", finalLat);
                        updates.put("longitude", finalLon);
                        if (timestamp != null) updates.put("timestamp", timestamp);
                        if (finalInDanger != null) updates.put("inDanger", finalInDanger);
                        if (finalSatellites != null) updates.put("satellites", finalSatellites);
                        if (finalHdop != null) updates.put("hdop", finalHdop);
                        
                        patientRef.updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Update UI immediately
                                updatePatientInList(patient.getDeviceId(), finalLat, finalLon, finalInDanger, finalSatellites, finalHdop);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating patient location: " + e.getMessage()));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in device location listener: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Device location listener cancelled: " + error.getMessage());
            }
        });
    }

    private void updatePatientInList(String deviceId, double latitude, double longitude, Boolean inDanger, Integer satellites, Float hdop) {
        // Find and update the patient in the current list
        for (PatientModel patient : patientList) {
            if (deviceId.equals(patient.getDeviceId())) {
                patient.setLatitude(latitude);
                patient.setLongitude(longitude);
                if (inDanger != null) patient.setInDanger(inDanger);
                if (satellites != null) patient.setSatellites(satellites);
                if (hdop != null) patient.setHdop(hdop);
                patient.setTimestamp(System.currentTimeMillis());
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "UI updated for patient: " + patient.getName());
                });
                break;
            }
        }
    }

    private void updatePatientsList(List<PatientModel> newPatients) {
        // Update the main list
        patientList.clear();
        patientList.addAll(newPatients);
        
        // Reset to first page when new data is loaded
        currentPage = 1;
        
        // Update the adapter with the new data
        adapter.notifyDataSetChanged();
        
        // Update pagination and empty state
        updatePageControls();
        updateEmptyView();
        
        // Update map markers

        
        Log.d(TAG, "Patient list updated with " + newPatients.size() + " patients");
    }

    private void updateEmptyView() {
        // Find the empty view
        View emptyView = findViewById(R.id.emptyView);
        if (emptyView != null) {
            // Show empty view if list is empty, hide otherwise
            emptyView.setVisibility(patientList.isEmpty() ? View.VISIBLE : View.GONE);
            
            // Update recycler view visibility opposite to empty view
            RecyclerView recyclerView = findViewById(R.id.recyclerView);
            if (recyclerView != null) {
                recyclerView.setVisibility(patientList.isEmpty() ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't sign out here as it will cause issues with navigation
        // FirebaseAuth.getInstance().signOut();
        // SessionManager sessionManager = new SessionManager(this);
        // sessionManager.logout();
    }

    private void showUpdatePatientDialog(PatientModel patient) {
        if (patient == null) {
            Toast.makeText(this, "Error: No patient selected", Toast.LENGTH_SHORT).show();
            return;
        }

        activeUpdateDialog = new Dialog(this);
        Dialog dialog = activeUpdateDialog;  // Use local variable for clarity
        dialog.setContentView(R.layout.dialog_update_patient);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
=======
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!FirebaseHelper.isUserAuthenticated()) {
            // Redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        DatabaseReference patientsRef = FirebaseHelper.getPatientsReference();
        if (patientsRef == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            return;
        }

        patientsRef.orderByChild("timestamp")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    patientList.clear();
                    for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                        PatientModel patient = patientSnapshot.getValue(PatientModel.class);
                        if (patient != null) {
                            patient.setId(patientSnapshot.getKey()); // Ensure ID is set
                            patientList.add(patient);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    
                    // Update empty state if needed
                    updateEmptyState();
                }
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

        // Initialize views
        TextInputEditText firstNameInput = dialog.findViewById(R.id.firstNameInput);
        TextInputEditText lastNameInput = dialog.findViewById(R.id.lastNameInput);
        TextInputEditText ageInput = dialog.findViewById(R.id.ageInput);
        TextInputEditText birthdateInput = dialog.findViewById(R.id.birthdateInput);
        AutoCompleteTextView genderInput = dialog.findViewById(R.id.genderInput);
        TextInputEditText latitudeInput = dialog.findViewById(R.id.latitudeInput);
        TextInputEditText longitudeInput = dialog.findViewById(R.id.longitudeInput);
        MaterialButton getCurrentLocationButton = dialog.findViewById(R.id.getCurrentLocationButton);
        MaterialButton pickLocationButton = dialog.findViewById(R.id.pickLocationButton);
        MaterialButton updateButton = dialog.findViewById(R.id.updateButton);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancelButton);

        // Setup Gender Dropdown
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
            this,
            R.layout.dropdown_item,
            genders
        );
        genderInput.setAdapter(genderAdapter);
        genderInput.setKeyListener(null);

        // Setup Birthdate Picker
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            birthdateInput.setText(dateFormat.format(calendar.getTime()));

            // Calculate and set age
            int age = calculateAge(calendar.getTimeInMillis());
            ageInput.setText(String.valueOf(age));
        };

        birthdateInput.setOnClickListener(v -> {
            new DatePickerDialog(MainActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Fill in patient data
        String[] names = patient.getName().split(" ", 2);
        firstNameInput.setText(names[0]);
        if (names.length > 1) lastNameInput.setText(names[1]);
        ageInput.setText(String.valueOf(patient.getAge()));
        birthdateInput.setText(patient.getBirthdate());
        genderInput.setText(patient.getGender(), false);
        latitudeInput.setText(String.valueOf(patient.getLatitude()));
        longitudeInput.setText(String.valueOf(patient.getLongitude()));

        // Setup location buttons
        getCurrentLocationButton.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getCurrentLocation((latitude, longitude) -> {
                    latitudeInput.setText(String.valueOf(latitude));
                    longitudeInput.setText(String.valueOf(longitude));
                });
            }
        });

        pickLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("pick_location", true);
            intent.putExtra("current_lat", patient.getLatitude());
            intent.putExtra("current_lon", patient.getLongitude());
            startActivityForResult(intent, PICK_LOCATION_REQUEST_CODE);
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        updateButton.setOnClickListener(v -> {
            // Check network connectivity
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection. Please check your network settings.", 
                    Toast.LENGTH_LONG).show();
                return;
            }

            // Validate inputs
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String birthdate = birthdateInput.getText().toString().trim();
            String ageStr = ageInput.getText().toString().trim();
            String gender = genderInput.getText().toString().trim();
            String latitudeStr = latitudeInput.getText().toString().trim();
            String longitudeStr = longitudeInput.getText().toString().trim();

            if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || 
                TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(birthdate) ||
                TextUtils.isEmpty(gender) || TextUtils.isEmpty(latitudeStr) || 
                TextUtils.isEmpty(longitudeStr)) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int age = Integer.parseInt(ageStr);
                double latitude = Double.parseDouble(latitudeStr);
                double longitude = Double.parseDouble(longitudeStr);

                // Show progress dialog
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Updating patient data...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // Create updated patient object
                String fullName = firstName + " " + lastName;
                PatientModel updatedPatient = new PatientModel(
                    patient.getId(),
                    fullName,
                    age,
                    birthdate,
                    gender,
                    latitude,
                    longitude
                );
                updatedPatient.setUserId(patient.getUserId());

                // Update in Firebase
                FirebaseHelper.updatePatient(patient.getId(), updatedPatient, new FirebaseHelper.OnDatabaseListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, 
                                "Patient updated successfully", Toast.LENGTH_SHORT).show();
                            loadPatientsFromFirebase(); // Refresh the list
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Error")
                                .setMessage("Failed to update patient: " + error)
                                .setPositiveButton("Retry", (d, which) -> updateButton.performClick())
                                .setNegativeButton("Cancel", null)
                                .show();
                        });
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for age and location", 
                    Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnDismissListener(dialogInterface -> activeUpdateDialog = null);
        dialog.show();
    }

    // Helper method to check network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    // Helper method to check location permission
    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    // Helper method to get current location
    private void getCurrentLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                callback.onLocationReceived(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            } else {
                Toast.makeText(this, "Could not get location. Please try again.", 
                    Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "GPS not available", Toast.LENGTH_SHORT).show();
        }
    }

    // Interface for location callback
    private interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
    }
    
    private void testFirebaseConnectivity() {
        Log.d(TAG, "Testing Firebase connectivity on startup...");
        
        DatabaseReference testRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("connection_test");
        String testValue = "startup_test_" + System.currentTimeMillis();
        
        // Set timeout
        android.os.Handler timeoutHandler = new android.os.Handler();
        Runnable timeoutRunnable = () -> {
            Log.e(TAG, "Firebase connectivity test timeout on startup");
            Toast.makeText(this, "Firebase connection timeout - some features may not work", Toast.LENGTH_LONG).show();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 second timeout
        
        testRef.setValue(testValue)
            .addOnSuccessListener(aVoid -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.d(TAG, "Firebase connectivity test: SUCCESS on startup");
            })
            .addOnFailureListener(e -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.e(TAG, "Firebase connectivity test: FAILED on startup - " + e.getMessage());
                Toast.makeText(this, "Firebase connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

<<<<<<< HEAD
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == ADD_PATIENT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String deviceId = data.getStringExtra("deviceId");
                String patientName = data.getStringExtra("patientName");
                Log.d(TAG, "Patient registered successfully - Device ID: " + deviceId + ", Name: " + patientName);
                
                // Refresh the patients list
                loadPatientsFromFirebase();
                
                // Show success message
                Toast.makeText(this, "Patient " + patientName + " registered successfully", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Add patient cancelled or failed");
            }
            // Always reload patients list to ensure it's up to date
            loadPatientsFromFirebase();
        } else if (requestCode == PICK_LOCATION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            double latitude = data.getDoubleExtra("picked_latitude", 0);
            double longitude = data.getDoubleExtra("picked_longitude", 0);
            
            if (activeUpdateDialog != null && activeUpdateDialog.isShowing()) {
                TextInputEditText latitudeInput = activeUpdateDialog.findViewById(R.id.latitudeInput);
                TextInputEditText longitudeInput = activeUpdateDialog.findViewById(R.id.longitudeInput);
                if (latitudeInput != null && longitudeInput != null) {
                    latitudeInput.setText(String.valueOf(latitude));
                    longitudeInput.setText(String.valueOf(longitude));
                }
            } else {
                Log.w(TAG, "Update dialog not found or not showing");
            }
        }
    }

    @Override
    public void onUpdateClick(PatientModel patient) {
        showUpdatePatientDialog(patient);
    }

    private void updatePageControls() {
        // Update empty state
        updateEmptyView();

        // Update pagination controls
        int totalPages = (int) Math.ceil(patientList.size() / 10.0); // 10 items per page
        pageNumberTextView.setText(String.format("%d/%d", currentPage, Math.max(1, totalPages)));
        
        // Enable/disable navigation buttons
        prevButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
        
        // Update the adapter with the current page's items
        int startIndex = (currentPage - 1) * 10;
        int endIndex = Math.min(startIndex + 10, patientList.size());
        
        List<PatientModel> currentPageItems = new ArrayList<>();
        if (!patientList.isEmpty() && startIndex < patientList.size()) {
            currentPageItems = patientList.subList(startIndex, endIndex);
        }
        
        adapter = new PatientListAdapter(this, currentPageItems, this);
        recyclerView.setAdapter(adapter);
    }
    
    private void checkSafeZones(PatientModel patient, double lat, double lon) {
        Log.d(TAG, "🚀 checkSafeZones method called for " + patient.getName());
        
        String deviceId = patient.getDeviceId();
        if (deviceId == null) {
            Log.w(TAG, "Cannot check safe zones: device ID is null for patient " + patient.getName());
            return;
        }

        Log.d(TAG, "Checking safe zones for patient " + patient.getName() + " at " + lat + ", " + lon);
        Log.d(TAG, "🔍 Device ID: " + deviceId);
        Log.d(TAG, "🔍 Firebase path: /zones/" + deviceId);

        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("zones")
            .child(deviceId);

        zonesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "📡 Safe zone data received for " + patient.getName() + ": " + dataSnapshot.exists());
                Log.d(TAG, "📡 Number of zones found: " + dataSnapshot.getChildrenCount());
                
                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "❌ No safe zones found for device " + deviceId);
                    Log.d(TAG, "💡 Consider creating a test safe zone for device " + deviceId);
                    return;
                }

                boolean foundSafeZone = false;
                boolean isInsideAnyZone = false;
                double closestZoneDistance = Double.MAX_VALUE;
                double closestZoneRadius = 0;
                
                // Check all safe zones - patient is safe if inside ANY zone
                for (DataSnapshot zoneSnapshot : dataSnapshot.getChildren()) {
                    String zoneKey = zoneSnapshot.getKey();
                    String zoneType = zoneSnapshot.child("type").getValue(String.class);
                    Log.d(TAG, "🔍 Checking zone " + zoneKey + " with type: " + zoneType);
                    
                    if ("safe".equals(zoneType)) {
                        foundSafeZone = true;
                        Double zoneLat = zoneSnapshot.child("latitude").getValue(Double.class);
                        Double zoneLon = zoneSnapshot.child("longitude").getValue(Double.class);
                        Integer radius = zoneSnapshot.child("radius").getValue(Integer.class);

                        Log.d(TAG, "✅ Safe zone found: lat=" + zoneLat + ", lon=" + zoneLon + ", radius=" + radius + "m");

                        if (zoneLat != null && zoneLon != null && radius != null) {
                            // Use precise distance calculation for accurate safe zone checking
                            double distance = NetworkUtils.calculateDistancePrecise(lat, lon, zoneLat, zoneLon);
                            Log.d(TAG, "📏 Distance to safe zone: " + distance + "m (radius: " + radius + "m)");
                            
                            if (distance <= radius) {
                                // Patient is inside this zone - they're safe!
                                isInsideAnyZone = true;
                                Log.d(TAG, "✅ Patient is within safe zone (distance: " + distance + "m <= radius: " + radius + "m)");
                                break; // No need to check other zones if inside one
                            } else {
                                // Track the closest zone for alert details
                                if (distance < closestZoneDistance) {
                                    closestZoneDistance = distance;
                                    closestZoneRadius = radius;
                                }
                                Log.d(TAG, "⚠️ Patient is outside this zone (distance: " + distance + "m > radius: " + radius + "m)");
                            }
                        } else {
                            Log.w(TAG, "❌ Invalid safe zone data: lat=" + zoneLat + ", lon=" + zoneLon + ", radius=" + radius);
                        }
                    } else {
                        Log.d(TAG, "⏭️ Skipping zone " + zoneKey + " (type: " + zoneType + ")");
                    }
                }
                
                // Determine previous safe/unsafe state for this device
                Boolean wasSafe = deviceSafeStatus.get(deviceId);
                if (wasSafe == null) {
                    // Default to safe until first violation
                    wasSafe = true;
                }

                // If patient is currently outside all safe zones, ALWAYS show a notification
                if (foundSafeZone && !isInsideAnyZone) {
                    Log.w(TAG, "🚨 PATIENT LEFT ALL SAFE ZONES! Closest zone distance: " + closestZoneDistance + "m, Radius: " + closestZoneRadius + "m");

                    // 1) Show the notification every time checkSafeZones runs
                    NotificationHelper.showSafeZoneViolationAlert(
                        MainActivity.this,
                        patient,
                        closestZoneDistance,
                        (int) closestZoneRadius
                    );

                    // 2) Only write ONE history record when transitioning from SAFE -> OUTSIDE
                    if (wasSafe) {
                        try {
                            FirebaseDatabase db = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
                            DatabaseReference alertRef = db.getReference("alerts")
                                .child(patient.getId())
                                .push();

                            LocationHistoryModel alert = new LocationHistoryModel(
                                patient.getId(),
                                lat,
                                lon,
                                "safe_zone_violation",
                                "SAFE_ZONE_EXIT"
                            );
                            alert.setId(alertRef.getKey());

                            alertRef.setValue(alert)
                                .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Alert saved to /alerts/" + patient.getId() + " from MainActivity"))
                                .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to save alert to /alerts from MainActivity: " + e.getMessage()));
                        } catch (Exception e) {
                            Log.e(TAG, "Error creating/saving alert to /alerts from MainActivity: " + e.getMessage());
                        }

                        // Mark as currently unsafe to avoid duplicate history entries
                        deviceSafeStatus.put(deviceId, false);
                    }

                } else if (foundSafeZone && isInsideAnyZone) {
                    Log.d(TAG, "✅ Patient is safe - inside at least one safe zone");
                    // Reset state so next time they leave we both notify & save one history entry
                    deviceSafeStatus.put(deviceId, true);
                } else if (!foundSafeZone) {
                    Log.w(TAG, "❌ No safe zone found for patient " + patient.getName());
                    Log.d(TAG, "💡 Available zones: " + dataSnapshot.getChildrenCount() + " total zones");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "❌ Failed to check safe zones for " + patient.getName() + ": " + error.getMessage());
                Log.e(TAG, "❌ Error code: " + error.getCode() + ", Details: " + error.getDetails());
            }
        });
    }
    
    private void createTestSafeZone() {
        Log.d(TAG, "Creating test safe zone for G7T55ZGF5");
        
        // Create a test safe zone directly
        Map<String, Object> zoneData = new HashMap<>();
        zoneData.put("type", "safe");
        zoneData.put("latitude", 8.158371); // Use coordinates from your device
        zoneData.put("longitude", 125.123023);
        zoneData.put("radius", 30);
        zoneData.put("timestamp", System.currentTimeMillis());
        zoneData.put("patientId", "G7T55ZGF5");
        zoneData.put("deviceId", "G7T55ZGF5");

        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("zones")
            .child("G7T55ZGF5");

        zonesRef.push().setValue(zoneData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Test safe zone created successfully");
                Toast.makeText(this, "Test safe zone created! Check logs for safe zone checks.", Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to create test safe zone: " + e.getMessage());
                Toast.makeText(this, "Failed to create test safe zone: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    private void createSafeZoneForPatient(PatientModel patient, double centerLat, double centerLon, int radius) {
        Log.d(TAG, "Creating safe zone for patient " + patient.getName() + " at " + centerLat + ", " + centerLon + " with radius " + radius + "m");
        
        String deviceId = patient.getDeviceId();
        if (deviceId == null) {
            Log.e(TAG, "Cannot create safe zone: device ID is null for patient " + patient.getName());
            Toast.makeText(this, "Error: Patient has no device ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create safe zone data
        Map<String, Object> zoneData = new HashMap<>();
        zoneData.put("type", "safe");
        zoneData.put("latitude", centerLat);
        zoneData.put("longitude", centerLon);
        zoneData.put("radius", radius);
        zoneData.put("timestamp", System.currentTimeMillis());
        zoneData.put("patientId", patient.getDeviceId());
        zoneData.put("deviceId", deviceId);

        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("zones")
            .child(deviceId);

        zonesRef.push().setValue(zoneData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Safe zone created successfully for " + patient.getName());
                Toast.makeText(this, "Safe zone created for " + patient.getName() + "!", Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to create safe zone for " + patient.getName() + ": " + e.getMessage());
                Toast.makeText(this, "Failed to create safe zone: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    private void checkNotificationPermissions() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "❌ Notifications are disabled!");
            Toast.makeText(this, "⚠️ Notifications are disabled! Please enable them in Settings.", Toast.LENGTH_LONG).show();
            
            // Show dialog to guide user
            new AlertDialog.Builder(this)
                .setTitle("Enable Notifications")
                .setMessage("This app needs notifications to alert you when patients leave safe zones. Please enable notifications in your device settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
        } else {
            Log.d(TAG, "✅ Notifications are enabled");
        }
    }


=======
    private void updateEmptyState() {
        if (patientList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseAuth.getInstance().signOut();
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.logout();
    }
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
}