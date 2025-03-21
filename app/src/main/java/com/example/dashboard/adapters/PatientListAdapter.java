package com.example.dashboard.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import com.example.dashboard.models.PatientModel;
import java.util.List;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.ViewHolder> {
    private List<PatientModel> patients;
    private OnPatientActionListener listener;
    private Context context;

    public PatientListAdapter(List<PatientModel> patients, OnPatientActionListener listener) {
        this.patients = patients;
        this.listener = listener;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PatientModel patient = patients.get(position);
        holder.patientName.setText(patient.getName());
        holder.patientAge.setText("Age " + patient.getAge());

        holder.btnNotes.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, PatientDetailsActivity.class);
            intent.putExtra("patient_name", patient.getName());
            intent.putExtra("patient_age", patient.getAge());
            intent.putExtra("patient_id", patient.getId());
            context.startActivity(intent);
        });
        
        holder.btnRefresh.setOnClickListener(v -> listener.onRefreshClick(patient));
        holder.btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(v.getContext(), patient));
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
        ImageButton btnNotes, btnRefresh, btnDelete;

        public ViewHolder(View view) {
            super(view);
            patientName = view.findViewById(R.id.patientName);
            patientAge = view.findViewById(R.id.patientAge);
            btnNotes = view.findViewById(R.id.btnNotes);
            btnRefresh = view.findViewById(R.id.btnRefresh);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }

    public interface OnPatientActionListener {
        void onNotesClick(PatientModel patient);
        void onRefreshClick(PatientModel patient);
        void onDeleteClick(PatientModel patient);
    }
} 