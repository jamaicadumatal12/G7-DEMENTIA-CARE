package com.example.dashboard.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dashboard.R;
import com.example.dashboard.PatientDetailsActivity;
import com.example.dashboard.MapActivity;
import com.example.dashboard.models.PatientModel;
import java.util.List;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.ViewHolder> {
    private List<PatientModel> patients;
    private OnPatientActionListener listener;
    private Context context;

    public PatientListAdapter(Context context, List<PatientModel> patients, OnPatientActionListener listener) {
        this.context = context;
        this.patients = patients;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            PatientModel patient = patients.get(position);
            
            // Add null checks
            if (holder.patientName != null && patient.getName() != null) {
                holder.patientName.setText(patient.getName());
            }
            if (holder.patientAge != null) {
                holder.patientAge.setText("Age: " + patient.getAge());
            }
            if (holder.patientLocation != null) {
                holder.patientLocation.setText("Location: " + 
                    String.format("%.6f, %.6f", patient.getLatitude(), patient.getLongitude()));
            }

            // Set click listeners
            if (holder.btnNotes != null) {
                holder.btnNotes.setOnClickListener(v -> {
                    if (listener != null) listener.onNotesClick(patient);
                });
            }
            if (holder.btnRefresh != null) {
                holder.btnRefresh.setOnClickListener(v -> {
                    if (listener != null) listener.onRefreshClick(patient);
                });
            }
            if (holder.btnDelete != null) {
                holder.btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(patient);
                });
            }
<<<<<<< HEAD
            
            // Change item click to open map activity (temporary fix)
            holder.itemView.setOnClickListener(v -> {
                if (context != null) {
                    try {
                        Intent intent = new Intent(context, MapActivity.class);
                        intent.putExtra("patient_id", patient.getId());
                        intent.putExtra("patient_name", patient.getName());
                        intent.putExtra("patient_lat", patient.getLatitude());
                        intent.putExtra("patient_lon", patient.getLongitude());
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Log.e("PatientListAdapter", "Error opening MapActivity: " + e.getMessage());
                        Toast.makeText(context, "Error opening map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
=======
>>>>>>> b5d2a787b9a45a98a510a50dd5229195138620a5
        } catch (Exception e) {
            Log.e("PatientListAdapter", "Error binding view holder: " + e.getMessage());
        }
    }

    private void showDeleteConfirmationDialog(Context context, PatientModel patient) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirmation, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView titleText = dialogView.findViewById(android.R.id.text1);
        TextView messageText = dialogView.findViewById(android.R.id.text2);
        if (titleText != null) titleText.setText("Delete Patient");
        if (messageText != null) messageText.setText("Are you sure you want to delete " + patient.getName() + "?");

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        deleteButton.setOnClickListener(v -> {
            listener.onDeleteClick(patient);
            dialog.dismiss();
            Toast.makeText(context, "Patient deleted", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName;
        TextView patientAge;
        TextView patientLocation;
        ImageButton btnNotes, btnRefresh, btnDelete;

        public ViewHolder(View view) {
            super(view);
            patientName = view.findViewById(R.id.patientName);
            patientAge = view.findViewById(R.id.patientAge);
            patientLocation = view.findViewById(R.id.patientLocation);
            btnNotes = view.findViewById(R.id.btnNotes);
            btnRefresh = view.findViewById(R.id.btnRefresh);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }

    public interface OnPatientActionListener {
        void onNotesClick(PatientModel patient);
        void onRefreshClick(PatientModel patient);
        void onDeleteClick(PatientModel patient);
        void onUpdateClick(PatientModel patient);
    }

    public void updatePatients(List<PatientModel> newPatients) {
        this.patients = newPatients;
        notifyDataSetChanged();
    }
} 