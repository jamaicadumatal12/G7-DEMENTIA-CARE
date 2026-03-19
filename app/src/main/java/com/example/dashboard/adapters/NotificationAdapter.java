package com.example.dashboard.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dashboard.R;
import com.example.dashboard.models.PatientModel;
import com.example.dashboard.models.LocationHistoryModel;
import com.example.dashboard.interfaces.PatientClickListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private Context context;
    private List<LocationHistoryModel> alerts;
    private Map<String, PatientModel> patientMap;
    private final PatientClickListener clickListener;
    private SimpleDateFormat dateFormat;

    public NotificationAdapter(Context context, PatientClickListener listener) {
        this.context = context;
        this.alerts = new ArrayList<>();
        this.patientMap = new HashMap<>();
        this.clickListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LocationHistoryModel alert = alerts.get(position);
        if (alert == null) return;
        
        PatientModel patient = patientMap.get(alert.getPatientId());
        
        String alertType = getAlertTypeText(alert.getEventType());
        String timeString = dateFormat.format(new Date(alert.getTimestamp()));
        
        // Determine patient name - handle dummy data specially
        String patientName;
        if (patient != null && patient.getName() != null) {
            patientName = patient.getName();
        } else if (alert.getPatientId() != null && alert.getPatientId().startsWith("dummy_")) {
            // Fallback for dummy data
            patientName = "Jessa";
        } else {
            patientName = "Unknown Patient";
        }
        
        // Build notification text
        String notificationContent = String.format("%s\n%s\nLocation: %.6f, %.6f\nTime: %s",
            patientName,
            alertType,
            alert.getLatitude(),
            alert.getLongitude(),
            timeString
        );
        
        // Always set text - ensure TextView exists and is visible
        if (holder.notificationText != null) {
            holder.notificationText.setText(notificationContent);
            holder.notificationText.setVisibility(View.VISIBLE);
            holder.notificationText.invalidate(); // Force redraw
        } else {
            android.util.Log.e("NotificationAdapter", "TextView is null when binding position: " + position);
        }

        // Set background color based on alert type
        if (holder.cardView != null) {
            int colorRes = getAlertTypeColor(alert.getEventType());
            try {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes));
            } catch (Exception e) {
                // Fallback to default color if there's an issue
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            }
            
            if (patient != null) {
                holder.cardView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onPatientClick(patient);
                    }
                });
            } else {
                holder.cardView.setOnClickListener(null);
            }
        }
    }

    private String getAlertTypeText(String eventType) {
        switch (eventType) {
            case "SAFE_ZONE_EXIT":
                return "⚠️ Patient has left safe zone!";
            case "DANGER_ZONE_ENTER":
                return "🚨 Patient has entered danger zone!";
            case "LOCATION_UPDATE":
                return "📍 Location updated";
            default:
                return "Alert: " + eventType;
        }
    }

    private int getAlertTypeColor(String eventType) {
        switch (eventType) {
            case "SAFE_ZONE_EXIT":
                return R.color.notification_orange;
            case "DANGER_ZONE_ENTER":
                return R.color.notification_red;
            case "LOCATION_UPDATE":
                return R.color.notification_blue;
            default:
                return R.color.notification_green;
        }
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public void setAlerts(List<LocationHistoryModel> newAlerts, List<PatientModel> patients) {
        this.alerts = newAlerts;
        this.patientMap.clear();
        for (PatientModel patient : patients) {
            if (patient != null && patient.getId() != null) {
                this.patientMap.put(patient.getId(), patient);
                // Also add by deviceId if different, for better matching
                if (patient.getDeviceId() != null && !patient.getDeviceId().equals(patient.getId())) {
                    this.patientMap.put(patient.getDeviceId(), patient);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public MaterialCardView cardView;
        public TextView notificationText;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            notificationText = itemView.findViewById(R.id.notificationText);
            
            // Ensure TextView is not null
            if (notificationText == null) {
                android.util.Log.e("NotificationAdapter", "TextView not found in layout!");
            }
        }
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.notification_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                showDeleteConfirmationDialog(position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteConfirmationDialog(int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        deleteButton.setOnClickListener(v -> {
            deleteItem(position);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteItem(int position) {
        if (position < 0 || position >= alerts.size()) return;

        LocationHistoryModel alert = alerts.get(position);
        if (alert == null) {
            alerts.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
            Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove from Firebase: both /location_history and /alerts
        String patientId = alert.getPatientId();
        String locationId = alert.getId();

        FirebaseDatabase db = FirebaseDatabase.getInstance("https://g7-dementia-care-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference rootRef = db.getReference();

        if (patientId != null && locationId != null) {
            // Delete from location_history/{patientId}/{locationId}
            rootRef.child("location_history")
                .child(patientId)
                .child(locationId)
                .removeValue();

            // Delete all alerts with matching 'id' under alerts/{patientId}
            rootRef.child("alerts")
                .child(patientId)
                .orderByChild("id")
                .equalTo(locationId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        android.util.Log.e("NotificationAdapter", "Failed to delete alert from Firebase: " + error.getMessage());
                    }
                });
        }

        alerts.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show();
    }
} 