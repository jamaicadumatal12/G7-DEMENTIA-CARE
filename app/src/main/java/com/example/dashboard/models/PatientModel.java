package com.example.dashboard.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PatientModel {
    private String id;
    private String deviceId;
    private String userId;
    private String name;
    private int age;
    private String birthdate;
    private String gender;
    private double latitude;
    private double longitude;
    private long timestamp;
<<<<<<< HEAD
    private boolean inDanger;
    private float radius; // Add radius field for safe zone
    private double safeLat; // Safe zone center latitude
    private double safeLng; // Safe zone center longitude
    private int satellites; // GPS satellites count
    private float hdop; // Horizontal Dilution of Precision
=======
    private boolean isWandering;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

    // Required empty constructor for Firebase
    public PatientModel() {}

    // Constructor for device registration
    public PatientModel(String deviceId, String name) {
        this.deviceId = deviceId;
        this.id = deviceId; // Use device ID as the patient ID for consistency
        this.name = name;
        this.latitude = 0;
        this.longitude = 0;
        this.timestamp = System.currentTimeMillis();
        this.inDanger = false;
    }

    // Constructor with all fields
    public PatientModel(String deviceId, String name, int age, String birthdate, String gender, double latitude, double longitude) {
        this.deviceId = deviceId;
        this.id = deviceId; // Use device ID as the patient ID for consistency
        this.name = name;
        this.age = age;
        this.birthdate = birthdate;
        this.gender = gender;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
        this.inDanger = false;
    }

    // Constructor with all fields
    public PatientModel(String id, String name, int age, String birthdate, String gender, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.birthdate = birthdate;
        this.gender = gender;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { 
        this.deviceId = deviceId;
        this.id = deviceId; // Keep ID and deviceId in sync
    }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getBirthdate() { return birthdate; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
<<<<<<< HEAD
    
    public boolean isInDanger() { return inDanger; }
    public void setInDanger(boolean inDanger) { this.inDanger = inDanger; }
    
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }
    
    public double getSafeLat() { return safeLat; }
    public void setSafeLat(double safeLat) { this.safeLat = safeLat; }
    
    public double getSafeLng() { return safeLng; }
    public void setSafeLng(double safeLng) { this.safeLng = safeLng; }
    
    public int getSatellites() { return satellites; }
    public void setSatellites(int satellites) { this.satellites = satellites; }
    
    public float getHdop() { return hdop; }
    public void setHdop(float hdop) { this.hdop = hdop; }
=======

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isWandering() {
        return isWandering;
    }

    public void setWandering(boolean wandering) {
        isWandering = wandering;
    }
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
} 