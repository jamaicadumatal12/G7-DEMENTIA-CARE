package com.example.dashboard;

import android.app.Application;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
<<<<<<< HEAD
=======
import com.google.firebase.database.DatabaseReference;
import android.content.Context;
import org.osmdroid.config.Configuration;
import android.util.Log;
import com.example.dashboard.utils.FirebaseHelper;
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5

public class DashboardApplication extends Application {
    private static final String TAG = "DashboardApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
<<<<<<< HEAD
        // Initialize Firebase with regional URL
        FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/").setPersistenceEnabled(true);
        Log.d(TAG, "Firebase initialized with persistence enabled (asia-southeast1 region)");
=======
        try {
            // Initialize Firebase first, before any other Firebase operations
            FirebaseHelper.initializeFirebase(this);
            
            // Check initialization status
            if (FirebaseApp.getApps(this).size() > 0) {
                Log.d(TAG, "Firebase initialized successfully");
                
                // Test database connection
                FirebaseHelper.checkFirebaseConnection(this);
            } else {
                Log.e(TAG, "Firebase not initialized properly");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            e.printStackTrace();
        }

        // Initialize OSMdroid configuration
        try {
            Context ctx = getApplicationContext();
            Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OSMdroid: " + e.getMessage());
        }
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
    }
}