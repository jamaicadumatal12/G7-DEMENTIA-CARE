package com.example.dashboard.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LocationHistoryModel {
    private String id;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String eventType;
    private String patientId;
    private boolean isAlert;
    private String zoneId;

    // Required empty constructor for Firebase
    public LocationHistoryModel() {}

    // Constructor for location updates
    public LocationHistoryModel(String patientId, double latitude, double longitude) {
        this.patientId = patientId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
        this.eventType = "LOCATION_UPDATE";
        this.isAlert = false;
    }

    // Constructor for geofence alerts
    public LocationHistoryModel(String patientId, double latitude, double longitude, String zoneId, String eventType) {
        this.patientId = patientId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.isAlert = true;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public boolean isAlert() { return isAlert; }
    public void setAlert(boolean alert) { isAlert = alert; }

    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
} 