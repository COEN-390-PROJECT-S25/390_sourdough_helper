package com.example.mainactivity;

import static androidx.core.text.HtmlCompat.fromHtml;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DeviceDataActivity extends AppCompatActivity {
    private TextView tvDeviceData, tvDeviceMessage;
    private String deviceIp, deviceMac;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_data);

        setupUI();
        toolbar_setup();
    }

    private void toolbar_setup() {
        ActionBar actionBar = getSupportActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#cc8e90"));
        actionBar.setBackgroundDrawable(colorDrawable);
        //different name, profile page
        actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>Starter Dashboard </font>"));
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupUI(){
        deviceIp = getIntent().getStringExtra("DEVICE_IP");
        deviceMac = getIntent().getStringExtra("DEVICE_MAC");

        tvDeviceMessage = findViewById(R.id.device_msg);
        tvDeviceData = findViewById(R.id.tvDeviceData);

        tvDeviceMessage.setText("Device IP: " + deviceIp);
        tvDeviceData.setText("Device MAC Address: " + deviceMac);

        //setup action bar
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            ColorDrawable colorDrawable = new ColorDrawable(getColor(R.color.primary_color));
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(fromHtml("Data Dashboard",getColor(R.color.on_primary_color)));
        }

        //graphed data button click
        findViewById(R.id.buttonGraphedData).setOnClickListener(v -> {
            Intent intent = new Intent(DeviceDataActivity.this, DataGraphActivity.class);
            intent.putExtra("DEVICE_MAC",deviceMac);
            //in the xml we use Singletop to prevent loss of data per pressing back buttons betweent
            //activities.
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_data_toolbar, menu);
        return true;
    }

    //options to select in the toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()== android.R.id.home){
            //back button
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        else if (item.getItemId() == R.id.action_setup_connection){
            Intent intent = new Intent(DeviceDataActivity.this, BluetoothActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}