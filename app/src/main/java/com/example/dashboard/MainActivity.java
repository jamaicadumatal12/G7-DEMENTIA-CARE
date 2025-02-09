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

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PatientAdapter adapter;
    private List<Patient> patientList;
    private EditText searchEditText;
    private Button cancelButton;
    private ImageButton addButton, updateButton, deleteButton, readButton;
    private ImageButton prevButton, nextButton;
    private TextView pageNumberTextView;
    private int currentPage = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        cancelButton = findViewById(R.id.cancelButton);
        addButton = findViewById(R.id.addButton);
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        readButton = findViewById(R.id.readButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        pageNumberTextView = findViewById(R.id.pageNumberTextView);

        // Sample data (REPLACE with your actual data source)
        patientList = new ArrayList<>();
        patientList.add(new Patient("Juan Dela Cruz", "Sumpong", 71));
        patientList.add(new Patient("Maria Clara", "Sumpong", 56));
        patientList.add(new Patient("Padre Damaso", "Kalasungay", 80));
        patientList.add(new Patient("Ibarra", "Brgy. 3", 63));
        patientList.add(new Patient("Sisa", "Brgy. 5", 66));
        patientList.add(new Patient("Yolanda", "Laguitas", 60));
        patientList.add(new Patient("Basillo", "San Jose", 60));
        // ... more patients

        // RecyclerView setup
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PatientAdapter(patientList); // Pass the patientList here
        recyclerView.setAdapter(adapter);

        // Cancel button
        cancelButton.setOnClickListener(v -> {
            searchEditText.setText("");
            adapter = new PatientAdapter(patientList); // Reset with original list
            recyclerView.setAdapter(adapter);
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

        // CRUD button examples (PLACEHOLDER logic - you MUST implement this)
        addButton.setOnClickListener(v -> {
            // Implement add patient logic (e.g., open a dialog, get data, add to DB/list)
        });

        updateButton.setOnClickListener(v -> {
            // Implement update patient logic (e.g., get selected item, open dialog, update DB/list)
        });

        deleteButton.setOnClickListener(v -> {
            // Implement delete patient logic (e.g., get selected items, delete from DB/list)
        });

        readButton.setOnClickListener(v -> {
            // Implement read patient details logic (e.g., open a detail view)
        });
    }


    // Example pagination method (you'll need to adapt this to your data source)
    // private List<Patient> getPatientsForPage(int page) {
    //     int pageSize = 10; // Number of items per page
    //     int offset = (page - 1) * pageSize;
    //     // Implement your database query or API call with offset and limit
    //     // to fetch the data for the current page.
    //     return new ArrayList<>(); // Replace with actual data
    // }
}