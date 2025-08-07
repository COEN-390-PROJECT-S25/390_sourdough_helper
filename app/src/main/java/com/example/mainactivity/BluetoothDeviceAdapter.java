package com.example.mainactivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> devices;
    public BluetoothDeviceAdapter(Context context, List<String> devices) {
        super(context, 0, devices);
        this.context = context;
        this.devices = devices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String device = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.bluetooth_device_list_item, parent, false);
        }
        String deviceInfo = devices.get(position);
        String[] parts = deviceInfo.split("\n");
        String deviceName = parts[0];
        String deviceAddress = parts.length > 1 ? parts[1] : "";


        TextView tvName = convertView.findViewById(R.id.bt_device_n);
        TextView tvAddress = convertView.findViewById(R.id.bt_device_add);

        tvName.setText(deviceName);
        tvAddress.setText(deviceAddress);

        return convertView;
    }
}