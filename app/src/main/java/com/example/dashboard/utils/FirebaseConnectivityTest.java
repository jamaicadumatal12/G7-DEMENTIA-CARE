package com.example.dashboard.utils;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.os.Handler;
import android.os.Looper;

public class FirebaseConnectivityTest {
    private static final String TAG = "FirebaseConnectivityTest";
    
    public interface ConnectivityCallback {
        void onSuccess();
        void onFailure(String error);
        void onTimeout();
    }
    
    public static void testConnection(ConnectivityCallback callback) {
        Log.d(TAG, "Testing Firebase connectivity...");
        
        DatabaseReference testRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("connection_test");
        String testValue = "test_" + System.currentTimeMillis();
        
        // Set timeout
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            Log.e(TAG, "Firebase connectivity test timeout");
            callback.onTimeout();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 second timeout
        
        testRef.setValue(testValue)
            .addOnSuccessListener(aVoid -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.d(TAG, "Firebase connectivity test: SUCCESS");
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.e(TAG, "Firebase connectivity test: FAILED - " + e.getMessage());
                callback.onFailure(e.getMessage());
            });
    }
    
    public static void testReadConnection(ConnectivityCallback callback) {
        Log.d(TAG, "Testing Firebase read connectivity...");
        
        DatabaseReference testRef = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("connection_test");
        
        // Set timeout
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            Log.e(TAG, "Firebase read test timeout");
            callback.onTimeout();
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000); // 10 second timeout
        
        testRef.get()
            .addOnSuccessListener(dataSnapshot -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.d(TAG, "Firebase read test: SUCCESS");
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                Log.e(TAG, "Firebase read test: FAILED - " + e.getMessage());
                callback.onFailure(e.getMessage());
            });
    }
} 