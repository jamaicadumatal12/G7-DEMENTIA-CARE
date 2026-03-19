package com.example.dashboard.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.dashboard.MainActivity;
import com.example.dashboard.R;
import com.example.dashboard.utils.NotificationHelper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        
        // TODO: Send this token to your server to associate with the user
        // FirebaseHelper.updateUserFCMToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Handle data payload
        Map<String, String> data = remoteMessage.getData();
        if (data.size() > 0) {
            Log.d(TAG, "Message data payload: " + data);
            handleDataMessage(data);
        }

        // Handle notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification payload: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        String patientId = data.get("patientId");
        String patientName = data.get("patientName");
        String message = data.get("message");
        
        if (type == null) return;

        switch (type) {
            case "safe_zone_violation":
                // Handle safe zone violation
                showSafeZoneViolationNotification(patientName, message);
                break;
            case "device_offline":
                // Handle device offline
                showDeviceOfflineNotification(patientName, message);
                break;
            case "device_online":
                // Handle device back online
                showDeviceOnlineNotification(patientName, message);
                break;
            case "low_gps_signal":
                // Handle low GPS signal
                showLowGPSSignalNotification(patientName, message);
                break;
            default:
                Log.w(TAG, "Unknown notification type: " + type);
        }
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        // Handle simple notification messages
        showGeneralNotification(notification.getTitle(), notification.getBody());
    }

    private void showSafeZoneViolationNotification(String patientName, String message) {
        String title = "🚨 Safety Alert: " + patientName;
        showNotification(title, message, "dementia_care_alerts", true);
    }

    private void showDeviceOfflineNotification(String patientName, String message) {
        String title = "⚠️ Device Offline: " + patientName;
        showNotification(title, message, "dementia_care_alerts", true);
    }

    private void showDeviceOnlineNotification(String patientName, String message) {
        String title = "✅ Device Online: " + patientName;
        showNotification(title, message, "dementia_care_general", false);
    }

    private void showLowGPSSignalNotification(String patientName, String message) {
        String title = "📡 Poor GPS Signal: " + patientName;
        showNotification(title, message, "dementia_care_general", false);
    }

    private void showGeneralNotification(String title, String message) {
        showNotification(title, message, "dementia_care_general", false);
    }

    private void showNotification(String title, String message, String channelId, boolean isHighPriority) {
        try {
            // Create intent to open MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(isHighPriority ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setCategory(NotificationCompat.CATEGORY_ALARM);

            if (isHighPriority) {
                builder.setDefaults(NotificationCompat.DEFAULT_ALL);
            }

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                Log.d(TAG, "FCM notification sent: " + title);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing FCM notification: " + e.getMessage());
        }
    }
} 