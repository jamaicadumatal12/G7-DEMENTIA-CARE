package com.example.dashboard.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.app.PendingIntent;
import android.content.Intent;
import com.example.dashboard.MainActivity;
import com.example.dashboard.R;
import com.example.dashboard.models.PatientModel;
import com.example.dashboard.models.LocationHistoryModel;
import com.example.dashboard.utils.FirebaseHelper;
import com.example.dashboard.utils.NotificationHelper;
import com.example.dashboard.utils.NetworkUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "location_tracking_service";
    private static final int NOTIFICATION_ID = 1001;
    
    private Map<String, ValueEventListener> deviceListeners = new HashMap<>();
    private List<PatientModel> patients = new ArrayList<>();
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Location tracking service created");
        try {
            createNotificationChannel();
            Log.d(TAG, "Notification channel created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Location tracking service onStartCommand called");
        
        try {
            if (!isServiceRunning) {
                Log.d(TAG, "Starting foreground service...");
                startForeground(NOTIFICATION_ID, createNotification());
                Log.d(TAG, "Foreground service started successfully");
                
                Log.d(TAG, "Loading patients and starting monitoring...");
                loadPatientsAndStartMonitoring();
                isServiceRunning = true;
                Log.d(TAG, "Service is now running");
            } else {
                Log.d(TAG, "Service is already running");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage());
            e.printStackTrace();
        }
        
        return START_STICKY; // Restart service if killed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Location tracking service destroyed");
        stopAllMonitoring();
        isServiceRunning = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for patient monitoring");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Patient Monitoring Active")
            .setContentText("Monitoring patients in background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }

    private void loadPatientsAndStartMonitoring() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.w(TAG, "No network available for patient monitoring");
            return;
        }

        FirebaseHelper.getPatientsReference()
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    patients.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        PatientModel patient = snapshot.getValue(PatientModel.class);
                        if (patient != null) {
                            patients.add(patient);
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + patients.size() + " patients for monitoring");
                    startMonitoringAllPatients();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load patients: " + databaseError.getMessage());
                }
            });
    }

    private void startMonitoringAllPatients() {
        for (PatientModel patient : patients) {
            startMonitoringPatient(patient);
        }
    }

    private void startMonitoringPatient(PatientModel patient) {
        String deviceId = patient.getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Log.w(TAG, "Patient " + patient.getName() + " has no device ID");
            return;
        }

        // Stop existing listener if any
        stopMonitoringPatient(patient);

        DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("devices")
            .child(deviceId);

        ValueEventListener listener = new ValueEventListener() {
            private long lastUpdateTime = 0;
            private boolean wasOffline = false;

            @Override
            public void onDataChange(DataSnapshot deviceSnapshot) {
                if (!deviceSnapshot.exists()) {
                    Log.w(TAG, "Device " + patient.getName() + " is offline");
                    handleDeviceOffline(patient);
                    return;
                }

                long currentTime = System.currentTimeMillis();
                lastUpdateTime = currentTime;

                // Check if device was offline and is now back online
                if (wasOffline) {
                    Log.d(TAG, "Device " + patient.getName() + " is back online");
                    NotificationHelper.showDeviceOnlineAlert(LocationTrackingService.this, patient);
                    wasOffline = false;
                }

                // Get device data
                Double latitude = deviceSnapshot.child("latitude").getValue(Double.class);
                Double longitude = deviceSnapshot.child("longitude").getValue(Double.class);
                Boolean inDanger = deviceSnapshot.child("inDanger").getValue(Boolean.class);
                Integer satellites = deviceSnapshot.child("satellites").getValue(Integer.class);
                Float hdop = deviceSnapshot.child("hdop").getValue(Float.class);

                Log.d(TAG, "Location update for " + patient.getName() + ": " + latitude + ", " + longitude + 
                      " (Danger: " + inDanger + ", Sats: " + satellites + ")");

                if (latitude != null && longitude != null) {
                    // Check GPS signal quality
                    if (satellites != null && satellites < 4) {
                        Log.w(TAG, "Low GPS signal for " + patient.getName() + ": " + satellites + " satellites");
                        NotificationHelper.showLowGPSSignalAlert(LocationTrackingService.this, patient);
                    }

                    // Check safe zones
                    checkSafeZones(patient, latitude, longitude);
                } else {
                    Log.w(TAG, "Invalid location data for " + patient.getName() + ": lat=" + latitude + ", lon=" + longitude);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Device monitoring cancelled for " + patient.getName() + ": " + error.getMessage());
            }

            private void handleDeviceOffline(PatientModel patient) {
                if (!wasOffline) {
                    NotificationHelper.showDeviceOfflineAlert(LocationTrackingService.this, patient);
                    wasOffline = true;
                }
            }
        };

        deviceRef.addValueEventListener(listener);
        deviceListeners.put(deviceId, listener);
        
        Log.d(TAG, "Started monitoring device " + deviceId + " for patient " + patient.getName());
    }

    private void stopMonitoringPatient(PatientModel patient) {
        String deviceId = patient.getDeviceId();
        if (deviceId != null && deviceListeners.containsKey(deviceId)) {
            DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("devices")
                .child(deviceId);
            
            deviceRef.removeEventListener(deviceListeners.get(deviceId));
            deviceListeners.remove(deviceId);
            
            Log.d(TAG, "Stopped monitoring device " + deviceId + " for patient " + patient.getName());
        }
    }

    private void stopAllMonitoring() {
        for (Map.Entry<String, ValueEventListener> entry : deviceListeners.entrySet()) {
            DatabaseReference deviceRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("devices")
                .child(entry.getKey());
            
            deviceRef.removeEventListener(entry.getValue());
        }
        deviceListeners.clear();
        Log.d(TAG, "Stopped all device monitoring");
    }

    private void checkSafeZones(PatientModel patient, double lat, double lon) {
        String deviceId = patient.getDeviceId();
        if (deviceId == null) {
            Log.w(TAG, "Cannot check safe zones: device ID is null for patient " + patient.getName());
            return;
        }

        Log.d(TAG, "Checking safe zones for patient " + patient.getName() + " at " + lat + ", " + lon);

        DatabaseReference zonesRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("zones")
            .child(deviceId);

        zonesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Safe zone data received for " + patient.getName() + ": " + dataSnapshot.exists());
                
                if (!dataSnapshot.exists()) {
                    Log.w(TAG, "No safe zones found for device " + deviceId);
                    return;
                }

                boolean foundSafeZone = false;
                boolean isInsideAnyZone = false;
                double closestZoneDistance = Double.MAX_VALUE;
                double closestZoneRadius = 0;
                
                // Check all safe zones - patient is safe if inside ANY zone
                for (DataSnapshot zoneSnapshot : dataSnapshot.getChildren()) {
                    String zoneType = zoneSnapshot.child("type").getValue(String.class);
                    Log.d(TAG, "Zone type: " + zoneType);
                    
                    if ("safe".equals(zoneType)) {
                        foundSafeZone = true;
                        Double zoneLat = zoneSnapshot.child("latitude").getValue(Double.class);
                        Double zoneLon = zoneSnapshot.child("longitude").getValue(Double.class);
                        Integer radius = zoneSnapshot.child("radius").getValue(Integer.class);

                        Log.d(TAG, "Safe zone data: lat=" + zoneLat + ", lon=" + zoneLon + ", radius=" + radius);

                        if (zoneLat != null && zoneLon != null && radius != null) {
                            // Use precise distance calculation for accurate safe zone checking
                            double distance = NetworkUtils.calculateDistancePrecise(lat, lon, zoneLat, zoneLon);
                            Log.d(TAG, "Distance to safe zone: " + distance + "m (radius: " + radius + "m)");
                            
                            if (distance <= radius) {
                                // Patient is inside this zone - they're safe!
                                isInsideAnyZone = true;
                                Log.d(TAG, "Patient is within safe zone (distance: " + distance + "m <= radius: " + radius + "m)");
                                break; // No need to check other zones if inside one
                            } else {
                                // Track the closest zone for alert details
                                if (distance < closestZoneDistance) {
                                    closestZoneDistance = distance;
                                    closestZoneRadius = radius;
                                }
                                Log.d(TAG, "Patient is outside this zone (distance: " + distance + "m > radius: " + radius + "m)");
                            }
                        } else {
                            Log.w(TAG, "Invalid safe zone data: lat=" + zoneLat + ", lon=" + zoneLon + ", radius=" + radius);
                        }
                    }
                }
                
                // Send alert only if patient is outside ALL safe zones
                if (foundSafeZone && !isInsideAnyZone) {
                    Log.w(TAG, "🚨 PATIENT LEFT ALL SAFE ZONES! Closest zone distance: " + closestZoneDistance + "m, Radius: " + closestZoneRadius + "m");
                    
                    // 1) Show high-priority notification
                    NotificationHelper.showSafeZoneViolationAlert(
                        LocationTrackingService.this, 
                        patient, 
                        closestZoneDistance, 
                        (int)closestZoneRadius
                    );

                    // 2) Persist this alert directly into /alerts/{patientId} for history
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
                                Log.d(TAG, "Alert saved to /alerts/" + patient.getId() + " from service"))
                            .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to save alert to /alerts: " + e.getMessage()));
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating/saving alert to /alerts: " + e.getMessage());
                    }
                } else if (foundSafeZone && isInsideAnyZone) {
                    Log.d(TAG, "Patient is safe - inside at least one safe zone");
                } else if (!foundSafeZone) {
                    Log.w(TAG, "No safe zones found for device " + deviceId);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to check safe zones for " + patient.getName() + ": " + error.getMessage());
            }
        });
    }

    public static void startService(Context context) {
        try {
            Log.d(TAG, "Attempting to start LocationTrackingService...");
            Intent intent = new Intent(context, LocationTrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Using startForegroundService for Android O+");
                context.startForegroundService(intent);
            } else {
                Log.d(TAG, "Using startService for older Android");
                context.startService(intent);
            }
            Log.d(TAG, "LocationTrackingService start command sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LocationTrackingService: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, LocationTrackingService.class);
        context.stopService(intent);
    }
    
    public static boolean isServiceRunning(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationTrackingService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if service is running: " + e.getMessage());
        }
        return false;
    }
} 