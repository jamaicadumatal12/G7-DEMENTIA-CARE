package com.example.dashboard.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.example.dashboard.MainActivity;
import com.example.dashboard.R;
import com.example.dashboard.models.PatientModel;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID_ALERTS = "dementia_care_alerts";
    private static final String CHANNEL_ID_GENERAL = "dementia_care_general";
    
    private static int notificationId = 1;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            
            // Alert channel for safe zone violations
            NotificationChannel alertChannel = new NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Critical alerts when patients leave safe zones");
            alertChannel.enableVibration(true);
            alertChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            
            // General channel for other notifications
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("General app notifications");
            
            notificationManager.createNotificationChannel(alertChannel);
            notificationManager.createNotificationChannel(generalChannel);
        }
    }

    public static void showSafeZoneViolationAlert(Context context, PatientModel patient, double distance, int radius) {
        String title = "🚨 Safety Alert: " + patient.getName();
        String message = String.format("%s has left the safe zone! Distance: %.1fm (Safe zone: %dm)", 
            patient.getName(), distance, radius);
        
        showNotification(context, title, message, CHANNEL_ID_ALERTS, true);
    }

    public static void showDeviceOfflineAlert(Context context, PatientModel patient) {
        String title = "⚠️ Device Offline: " + patient.getName();
        String message = "GPS device is not responding. Please check device status.";
        
        showNotification(context, title, message, CHANNEL_ID_ALERTS, true);
    }

    public static void showDeviceOnlineAlert(Context context, PatientModel patient) {
        String title = "✅ Device Online: " + patient.getName();
        String message = "GPS device is back online and tracking.";
        
        showNotification(context, title, message, CHANNEL_ID_GENERAL, false);
    }

    public static void showLowGPSSignalAlert(Context context, PatientModel patient) {
        String title = "📡 Poor GPS Signal: " + patient.getName();
        String message = "GPS signal is weak. Location accuracy may be reduced.";
        
        showNotification(context, title, message, CHANNEL_ID_GENERAL, false);
    }
    
    public static void showTestNotification(Context context) {
        String title = "🧪 Test Notification";
        String message = "This is a test notification to verify the system is working!";
        
        showNotification(context, title, message, CHANNEL_ID_ALERTS, true);
    }

    private static void showNotification(Context context, String title, String message, 
                                       String channelId, boolean isHighPriority) {
        try {
            Log.d(TAG, "Attempting to show notification: " + title);
            
            // Create intent to open MainActivity
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(isHighPriority ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(false)
                .setOnlyAlertOnce(false);

            // Apply user-selected alert mode (sound/vibrate/silent)
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String mode = prefs.getString(
                com.example.dashboard.AlertSettingsActivity.PREF_KEY_ALERT_MODE,
                com.example.dashboard.AlertSettingsActivity.MODE_SOUND_VIBRATE
            );

            if (com.example.dashboard.AlertSettingsActivity.MODE_VIBRATE_ONLY.equals(mode)) {
                builder.setVibrate(new long[]{0, 500, 200, 500});
                builder.setSound(null);
            } else if (com.example.dashboard.AlertSettingsActivity.MODE_SILENT.equals(mode)) {
                builder.setVibrate(null);
                builder.setSound(null);
                builder.setSilent(true);
            } else {
                // Default: sound + vibrate
                builder.setVibrate(new long[]{0, 500, 200, 500});
                if (isHighPriority) {
                    builder.setDefaults(NotificationCompat.DEFAULT_ALL);
                    builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
                }
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            
            // Check if notifications are enabled
            if (notificationManager.areNotificationsEnabled()) {
                int currentNotificationId = notificationId++;
                notificationManager.notify(currentNotificationId, builder.build());
                Log.d(TAG, "✅ Notification sent successfully! ID: " + currentNotificationId + ", Title: " + title);
                
                // Also show a toast for immediate feedback
                android.widget.Toast.makeText(context, "🚨 " + title, android.widget.Toast.LENGTH_LONG).show();
            } else {
                Log.w(TAG, "❌ Notifications are disabled for this app");
                // Show toast as fallback
                android.widget.Toast.makeText(context, "🚨 " + title + "\n" + message, android.widget.Toast.LENGTH_LONG).show();
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Security exception showing notification: " + e.getMessage());
            // Show toast as fallback
            android.widget.Toast.makeText(context, "🚨 " + title + "\n" + message, android.widget.Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error showing notification: " + e.getMessage());
            // Show toast as fallback
            android.widget.Toast.makeText(context, "🚨 " + title + "\n" + message, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    public static void cancelNotification(Context context, int notificationId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(notificationId);
    }
}