package com.example.mainactivity;

import static androidx.core.text.HtmlCompat.fromHtml;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

public class WiFiActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private EditText etSsid, etPassword;
    private BluetoothSocket btSocket;
    boolean finish = false;
    private Button complete, btnSubmit;
    String ip, MacAddress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi);

        //setup action bar
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            ColorDrawable colorDrawable = new ColorDrawable(getColor(R.color.primary_color));
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(fromHtml("WiFi Connection",getColor(R.color.on_primary_color)));
        }

        //widgets
        etSsid = findViewById(R.id.etSsid);
        etPassword = findViewById(R.id.etPassword);
        btnSubmit = findViewById(R.id.btnSubmit);
        complete = findViewById(R.id.wifi_complete_button);

        //get the bluetooth socket from the manager class we setup
        btSocket = BluetoothConnectionManager.getInstance().getSocket();
        if (btSocket == null || !btSocket.isConnected()) {
            Toast.makeText(this, "FAILED CONNECTION TO BLUETOOTH", Toast.LENGTH_SHORT).show();
        }

        //when you press this it sends the data to the device via bluetooth
        btnSubmit.setOnClickListener(v -> {
            String ssid = etSsid.getText().toString();
            String password = etPassword.getText().toString();

            if (btSocket != null && btSocket.isConnected()) {
                new Thread(() -> {
                    try {
                        OutputStream out = btSocket.getOutputStream();
                        out.write((ssid + "|" + password + "\n").getBytes());

                        InputStream in = btSocket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        String response = reader.readLine();

                        runOnUiThread(() -> {
                            if (response != null && response.startsWith("WIFI_SUCCESS")) {
                                try {
                                    ip = response.split("\\|")[1];
                                    MacAddress = response.split("\\|")[2];
                                    verifyMacAddressInFirebase(MacAddress);
                                    //Toast.makeText(this, "Successfully connected to WiFi!", Toast.LENGTH_SHORT).show();
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    Toast.makeText(this, "Invalid response format", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Failed to configure WiFi", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            } else {
                Toast.makeText(this, "Not connected to device", Toast.LENGTH_SHORT).show();
            }
        });

        //complete button only if you are done bluetooth connecting.
        complete.setOnClickListener(v -> {
            if (finish) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("ESP32_IP", ip);
                intent.putExtra("ESP32_MAC", MacAddress); //send MAC address
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Connect to WiFi first!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void verifyMacAddressInFirebase(String macAddress) {
        // Make sure databaseReference is properly initialized
        if (databaseReference == null) {
            databaseReference = FirebaseDatabase.getInstance().getReference();
        }

        // Clean the MAC address (remove colons and make uppercase)
        String cleanMac = macAddress;

        // Show waiting message
        Toast.makeText(this, "Waiting for device to register in database...", Toast.LENGTH_SHORT).show();

        // Create a listener that will keep checking until MAC appears
        ValueEventListener macVerificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean macFound = false;

                // Check each sensor node for matching MAC
                for (DataSnapshot sensorSnapshot : dataSnapshot.getChildren()) {
                    String sensorMac = sensorSnapshot.child("general").child("mac_address").getValue(String.class);
                    System.out.println("MAC FOUND: " + sensorMac + " VS " + cleanMac);
                    if (Objects.equals(cleanMac, sensorMac)) {
                        macFound = true;
                        break;
                    }
                }

                if (macFound) {
                    // MAC found - success!
                    finish = true;
                    databaseReference.child("sensors").removeEventListener(this); // Stop listening
                    Toast.makeText(WiFiActivity.this,
                            "Device successfully registered!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // MAC not found yet, keep waiting
                    Toast.makeText(WiFiActivity.this,
                            "Still waiting for device registration...",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseReference.child("sensors").removeEventListener(this);
                finish = false;
                Toast.makeText(WiFiActivity.this,
                        "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        // Start listening for changes in the sensors node
        databaseReference.child("sensors")
                .addValueEventListener(macVerificationListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.wifi_toolbar, menu);
        return true;
    }

    //options to select in the toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //first option is to sort students by id or surname
        if (item.getItemId()== android.R.id.home){
            //back button
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}