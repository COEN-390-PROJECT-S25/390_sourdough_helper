package com.example.mainactivity;

import static androidx.core.text.HtmlCompat.fromHtml;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.Html;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayList<String> deviceList;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    TextView tvStatus;
    Button btnScan, btnSend, complete;
    ListView lvDevices;
    private boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        btnScan = findViewById(R.id.btnScan);
//      btnSend = findViewById(R.id.btnSend);
        complete = findViewById(R.id.complete_bluetooth);
        lvDevices = findViewById(R.id.lvDevices);
        tvStatus = findViewById(R.id.tvStatus);

        //bluetooth setup
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            tvStatus.setText("Bluetooth not supported");
            btnScan.setEnabled(false);
            return;
        }

        //setup action bar
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            ColorDrawable colorDrawable = new ColorDrawable(getColor(R.color.primary_color));
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(fromHtml("Bluetooth Connection",getColor(R.color.on_primary_color)));
        }

        //get the list of devices
        deviceList = new ArrayList<>();
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        lvDevices.setAdapter(deviceAdapter);

        //scan the devices around
        btnScan.setOnClickListener(v -> {
            if (checkBluetoothPermissions()) {
                scanDevices();
            }
        });

        //COMPLETE button
        complete.setOnClickListener(v -> {
            if (connected){
                Intent intent = new Intent (BluetoothActivity.this, WiFiActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Connect to a device first!", Toast.LENGTH_SHORT).show();
            }
        });

        //the list itself
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (checkBluetoothPermissions()) {
                String deviceInfo = deviceList.get(position);
                String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);

                try {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                    connectToDevice(device);
                    String deviceName;
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        deviceName = device.getName();
                    } else {
                        deviceName = "Unknown Device";
                    }
                    tvStatus.setText("Connecting to " + deviceName);
//                    btnSend.setEnabled(true);
                } catch (IllegalArgumentException e) {
                    tvStatus.setText("Invalid device address");
                }
            }
        });

        //Send data and try to connect
//        btnSend.setOnClickListener(v -> {
//            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
//                try {
//                    String message = "Hello ESP32!\n";
//                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
//                    outputStream.flush();
//                    //confirmation
//                    tvStatus.setText("Message sent: " + message);
//                } catch (IOException e) {
//                    tvStatus.setText("Error sending message");
//                }
//            }
//        });
    }

    //check if user can use bluetooth
    private boolean checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        return true;
    }

    //scan for devices
    private void scanDevices() {
        if (bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            pairedDevices = bluetoothAdapter.getBondedDevices();
            deviceList.clear();
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().toLowerCase().contains("esp")){
                    deviceList.add(device.getName() + "\n" + device.getAddress());
                }
            }
            deviceAdapter.notifyDataSetChanged();
            TextView tvStatus = findViewById(R.id.tvStatus);
            tvStatus.setText("Found " + deviceList.size() + " device(s)");
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
        }
    }

    //connect to the bluetooth device
    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                //existing connection code
                if (ActivityCompat.checkSelfPermission(BluetoothActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                //try to connect
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();

                BluetoothConnectionManager.getInstance().setSocket(bluetoothSocket);

                //add the distance reading receiver
                InputStream inputStream = bluetoothSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                runOnUiThread(() -> {
                    tvStatus.setText("Connected to " + device.getName());
//                    btnSend.setEnabled(true);
                    connected = true;
                });

//                //continuous reading loop for TESTING ONLY
//                while (true) {
//                    try {
//                        String distanceData = reader.readLine();
//                        runOnUiThread(() -> {
//                            tvStatus.setText("Distance: " + distanceData);
//                        });
//                    } catch (IOException e) {
//                        runOnUiThread(() -> {
//                            tvStatus.setText("Disconnected");
//                        });
//                        break;
//                    }
//                }

            } catch (IOException e) {
                runOnUiThread(() -> {
                    tvStatus.setText("Connection failed: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bluetooth_toolbar, menu);
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

    //check if ur allowed to connect
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //when given permission you scan
                scanDevices();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //when stopping bluetooth
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
