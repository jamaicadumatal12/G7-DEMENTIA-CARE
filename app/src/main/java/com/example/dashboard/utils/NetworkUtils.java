package com.example.dashboard.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Calculate the distance between two geographic points using the Haversine formula
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = R * c;
        
        Log.d(TAG, String.format("Distance calculation: (%.6f, %.6f) to (%.6f, %.6f) = %.2fm", 
                                lat1, lon1, lat2, lon2, distance));
        
        return distance;
    }

    /**
     * Enhanced distance calculation for accurate safe zone checking
     * Uses Haversine formula consistently for accuracy across all distances
     * This ensures the distance calculation matches the spherical trigonometry
     * used for drawing safe zone circles on the map
     */
    public static double calculateDistancePrecise(double lat1, double lon1, double lat2, double lon2) {
        // Always use Haversine formula for consistency with circle drawing method
        // This ensures the visual circle on the map matches the actual radius being checked
        final double R = 6371000; // Earth's radius in meters
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = R * c;
        
        Log.d(TAG, String.format("Precise distance (Haversine): (%.8f, %.8f) to (%.8f, %.8f) = %.3fm", 
                                lat1, lon1, lat2, lon2, distance));
        return distance;
    }

    /**
     * Check if a point is within a circular safe zone with enhanced precision
     * @param pointLat Latitude of the point to check
     * @param pointLon Longitude of the point to check
     * @param zoneLat Latitude of the safe zone center
     * @param zoneLon Longitude of the safe zone center
     * @param radius Radius of the safe zone in meters
     * @return true if the point is within the safe zone
     */
    public static boolean isWithinSafeZone(double pointLat, double pointLon, 
                                         double zoneLat, double zoneLon, double radius) {
        double distance = calculateDistancePrecise(pointLat, pointLon, zoneLat, zoneLon);
        boolean isWithin = distance <= radius;
        
        Log.d(TAG, String.format("Safe zone check: distance=%.3fm, radius=%.3fm, within=%s", 
                                distance, radius, isWithin));
        
        return isWithin;
    }

    /**
     * Get distance with detailed breakdown for debugging
     */
    public static String getDistanceBreakdown(double lat1, double lon1, double lat2, double lon2) {
        double haversineDistance = calculateDistance(lat1, lon1, lat2, lon2);
        double preciseDistance = calculateDistancePrecise(lat1, lon1, lat2, lon2);
        
        return String.format("Haversine: %.3fm | Precise: %.3fm | Diff: %.3fm", 
                           haversineDistance, preciseDistance, Math.abs(haversineDistance - preciseDistance));
    }
} 