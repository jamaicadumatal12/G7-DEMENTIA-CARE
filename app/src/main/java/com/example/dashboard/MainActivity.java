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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.dashboard.utils.FirebaseHelper;
import androidx.annotation.NonNull;
import com.example.dashboard.utils.NetworkUtils;
import com.example.dashboard.utils.SessionManager;
import androidx.appcompat.app.ActionBarDrawerToggle;

public class MainActivity extends AppCompatActivity implements PatientListAdapter.OnPatientActionListener {

    private RecyclerView recyclerView;
    private PatientListAdapter adapter;
    private List<PatientModel> patientList;
    private EditText searchEditText;
    private Button cancelButton;
    private ImageButton addButton, updateButton, deleteButton, readButton;
    private ImageButton prevButton, nextButton;
    private TextView pageNumberTextView;
    private int currentPage = 1;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        deleteButton = findViewById(R.id.deleteButton);
        readButton = findViewById(R.id.readButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pageNumberTextView = findViewById(R.id.pageNumberTextView);

        // Initialize sample data with PatientModel
        patientList = new ArrayList<>();
        patientList.add(new PatientModel("Juan Dela Cruz", 71, "P001"));
        patientList.add(new PatientModel("Maria Clara", 56, "P002"));
        patientList.add(new PatientModel("Padre Damaso", 80, "P003"));
        // Add more patients as needed

        // Setup RecyclerView with new adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientListAdapter(patientList, this);
        recyclerView.setAdapter(adapter);

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

        // Pagination buttons (PLACEHOLDER logic - you MUST implement this)
        nextButton.setOnClickListener(v -> {
            currentPage++;
            pageNumberTextView.setText(String.valueOf(currentPage));
            // Implement actual pagination logic here (fetch data for the page)
            // Example:
            // List<Patient> pagedList = getPatientsForPage(currentPage);
            // adapter = new PatientAdapter(pagedList);
            // recyclerView.setAdapter(adapter);
        });

        prevButton.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                pageNumberTextView.setText(String.valueOf(currentPage));
                // Implement actual pagination logic here (fetch data for the page)
                // Example:
                // List<Patient> pagedList = getPatientsForPage(currentPage);
                // adapter = new PatientAdapter(pagedList);
                // recyclerView.setAdapter(adapter);
            }
        });

        // Load patients from Firebase
        loadPatientsFromFirebase();

        // Update add button click listener
        addButton.setOnClickListener(v -> {
            showAddPatientDialog();
        });

        updateButton.setOnClickListener(v -> {
            // Implement update patient logic (e.g., get selected item, open dialog, update DB/list)
        });

        deleteButton.setOnClickListener(v -> {
            // Implement delete patient logic (e.g., get selected items, delete from DB/list)
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

        ImageButton notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
        });
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
        // Implement refresh logic
        Toast.makeText(this, "Refreshing patient: " + patient.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(PatientModel patient) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Patient")
            .setMessage("Are you sure you want to delete " + patient.getName() + "?")
            .setPositiveButton("Yes", (dialog, which) -> {
                FirebaseHelper.getPatientsReference()
                    .orderByChild("id")
                    .equalTo(patient.getId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                                patientSnapshot.getRef().removeValue()
                                    .addOnSuccessListener(aVoid -> 
                                        Toast.makeText(MainActivity.this, 
                                            "Patient deleted successfully", 
                                            Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> 
                                        Toast.makeText(MainActivity.this, 
                                            "Error deleting patient: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MainActivity.this, 
                                "Error: " + error.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
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
        
        adapter = new PatientListAdapter(filteredList, this);
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
        TextInputEditText birthDateInput = dialog.findViewById(R.id.birthDateInput);
        AutoCompleteTextView genderInput = dialog.findViewById(R.id.genderInput);
        Button saveButton = dialog.findViewById(R.id.saveButton);

        // Setup gender dropdown
        String[] genders = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genders
        );
        genderInput.setAdapter(adapter);

        // Setup date picker
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            birthDateInput.setText(dateFormat.format(calendar.getTime()));
        };

        birthDateInput.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        // Handle save button click
        saveButton.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String age = ageInput.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || age.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String fullName = firstName + " " + lastName;
            String patientId = "P" + System.currentTimeMillis();
            PatientModel newPatient = new PatientModel(fullName, Integer.parseInt(age), patientId);

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
        });

        dialog.show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Perform logout
                // Clear any saved credentials/session
                // Navigate to login screen
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("No", null)
            .show();
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
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please check your network.", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseHelper.getPatientsReference()
            .orderByChild("timestamp")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    patientList.clear();
                    for (DataSnapshot patientSnapshot : snapshot.getChildren()) {
                        PatientModel patient = patientSnapshot.getValue(PatientModel.class);
                        if (patient != null) {
                            patientList.add(patient);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, 
                        "Error loading patients: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}