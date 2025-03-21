package com.example.dashboard.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PatientModel {
    private String id;
    private String name;
    private int age;
    private double latitude;
    private double longitude;
    private long timestamp;

    // Required empty constructor for Firebase
    public PatientModel() {}

    // Constructor for basic patient info
    public PatientModel(String name, int age, String id) {
        this.name = name;
        this.age = age;
        this.id = id;
        this.latitude = 0.0;  // Default values
        this.longitude = 0.0;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor with location
    public PatientModel(String id, String name, int age, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 