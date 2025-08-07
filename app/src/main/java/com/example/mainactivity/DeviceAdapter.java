package com.example.mainactivity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.mainactivity.Database.entity.TipsEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DeviceAdapter extends ArrayAdapter<String> {
    private final MainActivity activity;
    String deviceName;
    private DatabaseReference databaseReference;
    public DeviceAdapter(MainActivity context, ArrayList<String> devices) {
        super(context, 0, devices);
        this.activity = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String deviceInfo = getItem(position);
        String[] parts = deviceInfo.split(" - ");
        String deviceMac = parts[2];

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.device_list_item, parent, false);
        }
        TextView tvDeviceName = convertView.findViewById(R.id.tvDeviceName);
        TextView tvDeviceMac = convertView.findViewById(R.id.tvDeviceMac);
        ImageButton btnDelete = convertView.findViewById(R.id.btnDelete);

        databaseReference = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac);
        databaseReference.child("general").child("device_name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()){
                    deviceName = dataSnapshot.getValue(String.class);
                    tvDeviceName.setText(deviceName);
                }else{
                    Toast.makeText(getContext(), "SNAPSHOT ERROR", Toast.LENGTH_SHORT).show();
                }
            }else{
                System.out.println("IT DOES NTO EXIST");
                Toast.makeText(getContext(), "Firebase Retrieval Error", Toast.LENGTH_SHORT).show();
            }
        });

        System.out.println("DEVICE NAME SET: " + deviceName);
        tvDeviceMac.setText(deviceMac);

        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmation(position);
        });

        // Add ripple effect to entire item
        convertView.setOnClickListener(v -> {
            String devicep = deviceInfo.split(" - ")[1];
            String deviceM = deviceInfo.split(" - ")[2];
            Intent intent = new Intent(activity, DeviceDataActivity.class);
            intent.putExtra("DEVICE_IP", devicep);
            intent.putExtra("DEVICE_MAC", deviceM);
            activity.startActivity(intent);
        });

        return convertView;
    }

    private void showDeleteConfirmation(int position) {
        new MaterialAlertDialogBuilder(getContext(), R.style.AlertDialogTheme)
                .setTitle("Remove Device")
                .setMessage("Are you sure you want to remove this device?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    activity.removeDevice(position);
                })
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.baseline_warning_24)
                .show();
    }

}