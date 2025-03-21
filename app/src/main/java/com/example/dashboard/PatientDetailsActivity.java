package com.example.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class PatientDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView nameTextView = findViewById(R.id.patientName);
        TextView ageTextView = findViewById(R.id.patientAge);
        TextView notesTextView = findViewById(R.id.patientNotes);

        boolean showAllPatients = getIntent().getBooleanExtra("show_all_patients", false);

        if (showAllPatients) {
            // Display all patients
            String allPatientsInfo = getIntent().getStringExtra("all_patients_info");
            nameTextView.setVisibility(View.GONE);
            ageTextView.setVisibility(View.GONE);
            notesTextView.setText("Registered Patients:\n\n" + allPatientsInfo);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("All Patients");
            }
        } else {
            // Display single patient details
            String name = getIntent().getStringExtra("patient_name");
            int age = getIntent().getIntExtra("patient_age", 0);
            String id = getIntent().getStringExtra("patient_id");

            nameTextView.setText("Name: " + name);
            ageTextView.setText("Age: " + age);
            notesTextView.setText("Patient ID: " + id + "\n\nNotes:\nThis is where patient notes and additional information will be displayed.");
        }
    }
} 