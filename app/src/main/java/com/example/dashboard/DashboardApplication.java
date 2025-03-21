package com.example.dashboard;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import android.content.Context;
import org.osmdroid.config.Configuration;

public class DashboardApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(getApplicationContext());
            
            // Enable offline persistence
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            
            // Additional offline settings
            DatabaseReference.goOnline();
            FirebaseDatabase.getInstance().getReference().keepSynced(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize OSMdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
    }
} 