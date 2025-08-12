package com.example.mainactivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.mainactivity.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DeviceAdapter extends ArrayAdapter<String> {
    private ArrayList<String> devices;
    private Context context;
    private SharedPreferences prefs;
    private DatabaseReference databaseReference;
    private final MainActivity activity;

    public DeviceAdapter(Context context, ArrayList<String> devices, MainActivity activity) {
        super(context, R.layout.device_list_item, devices);
        this.devices = devices;
        this.context = context;
        this.prefs = context.getSharedPreferences("DEVICE_PREFS", Context.MODE_PRIVATE);
        this.activity = activity;
    }

    private static class ViewHolder {
        TextView tvDeviceName;
        ImageButton btnDelete;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.device_list_item, parent, false);
            holder = new ViewHolder();
            holder.tvDeviceName = convertView.findViewById(R.id.tvDeviceName);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        String device = getItem(position);
        String[] parts = device.split(" - ");
        String deviceMac = parts[1];

        System.out.println("NEW DEVICE IS: " + deviceMac);
        databaseReference = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac);
        databaseReference.child("general").child("device_name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()){
                    String deviceName = dataSnapshot.getValue(String.class);
                    holder.tvDeviceName.setText(deviceName);
                }else{
                    Toast.makeText(getContext(), "SNAPSHOT ERROR", Toast.LENGTH_SHORT).show();
                }
            }else{
                System.out.println("IT DOES NTO EXIST");
                Toast.makeText(getContext(), "Firebase Retrieval Error", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(position));

        // Add ripple effect to entire item
        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, DeviceDataActivity.class);
            intent.putExtra("DEVICE_MAC", deviceMac);
            activity.startActivity(intent);
        });


        return convertView;
    }

    private void showDeleteConfirmationDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogTheme2);
        builder.setTitle("Delete Device")
                .setMessage("Are you sure you want to remove this device?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    activity.removeDevice(position);
                })
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors if needed
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.icon_red));
    }
}