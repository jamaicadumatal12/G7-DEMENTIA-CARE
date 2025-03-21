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

public class FirebaseHelper {
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final DatabaseReference database = FirebaseDatabase.getInstance().getReference();

    // Authentication methods
    public static void loginUser(String email, String password, OnAuthListener listener) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                listener.onSuccess(authResult.getUser());
            })
            .addOnFailureListener(e -> {
                listener.onError(e.getMessage());
            });
    }

    public static void registerUser(String email, String password, String name, String address, OnAuthListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    // Send verification email
                    user.sendEmailVerification()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Save additional user info to database
                                DatabaseReference userRef = database.child("users").child(user.getUid());
                                Map<String, Object> userInfo = new HashMap<>();
                                userInfo.put("name", name);
                                userInfo.put("email", email);
                                userInfo.put("address", address);
                                userInfo.put("emailVerified", false);
                                
                                userRef.setValue(userInfo)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess(user))
                                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
                            } else {
                                listener.onError("Failed to send verification email");
                            }
                        });
                }
            })
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    // Database methods
    public static void savePatient(PatientModel patient, OnDatabaseListener listener) {
        String patientId = database.child("patients").push().getKey();
        database.child("patients").child(patientId).setValue(patient)
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void updatePatient(String patientId, PatientModel patient, OnDatabaseListener listener) {
        database.child("patients").child(patientId).setValue(patient)
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void deletePatient(String patientId, OnDatabaseListener listener) {
        database.child("patients").child(patientId).removeValue()
            .addOnSuccessListener(aVoid -> listener.onSuccess())
            .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static void searchPatients(String query, OnPatientsLoadedListener listener) {
        database.child("patients")
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
        return database.child("patients");
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
} 