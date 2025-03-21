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
import com.example.dashboard.interfaces.PatientClickListener;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private Context context;
    private List<PatientModel> patients;
    private final PatientClickListener clickListener;

    public NotificationAdapter(Context context, PatientClickListener listener) {
        this.context = context;
        this.patients = new ArrayList<>();
        this.clickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PatientModel patient = patients.get(position);
        holder.patientName.setText(patient.getName());
        holder.patientAge.setText("Age: " + patient.getAge());
        holder.patientLocation.setText("Location: " + 
            String.format("%.6f, %.6f", patient.getLatitude(), patient.getLongitude()));
        
        // Set click listener on the entire card
        holder.cardView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPatientClick(patient);
            }
        });
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public void setPatients(List<PatientModel> newPatients) {
        this.patients = newPatients;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public MaterialCardView cardView;
        public TextView patientName;
        public TextView patientAge;
        public TextView patientLocation;
        public ImageButton moreButton;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            patientName = itemView.findViewById(R.id.patientName);
            patientAge = itemView.findViewById(R.id.patientAge);
            patientLocation = itemView.findViewById(R.id.patientLocation);
            moreButton = itemView.findViewById(R.id.moreButton);
            moreButton.setOnClickListener(v -> showPopupMenu(v, getAdapterPosition()));
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
        patients.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, getItemCount());
        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show();
    }
} 