package com.example.dashboard.models;

public enum AlertType {
    SAFE_ZONE_EXIT("Patient has left safe zone", true),
    LOW_BATTERY("Wearable device battery low", true),
    DEVICE_DISCONNECTED("Wearable device disconnected", true),
    LOCATION_UPDATE("Location updated", false),
    DEVICE_CONNECTED("Wearable device connected", false);

    private final String message;
    private final boolean isUrgent;

    AlertType(String message, boolean isUrgent) {
        this.message = message;
        this.isUrgent = isUrgent;
    }

    public String getMessage() {
        return message;
    }

    public boolean isUrgent() {
        return isUrgent;
    }
} 