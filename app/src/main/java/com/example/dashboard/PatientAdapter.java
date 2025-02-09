package com.example.dashboard; // Make sure your package name is correct

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // Import TextView

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {
    private List<Patient> patientList;

    public PatientAdapter(List<Patient> patientList) {
        this.patientList = patientList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.patient_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient patient = patientList.get(position);
        holder.nameTextView.setText(patient.getName());
        holder.addressTextView.setText(patient.getAddress());
        holder.ageTextView.setText(String.valueOf(patient.getAge())); // Corrected
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView addressTextView;
        TextView ageTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            ageTextView = itemView.findViewById(R.id.ageTextView);
        }
    }
}