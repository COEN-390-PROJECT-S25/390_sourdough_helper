package com.example.mainactivity;

import static androidx.core.text.HtmlCompat.fromHtml;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toolbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainactivity.Database.AppDatabase;
import com.example.mainactivity.Database.entity.InfoEntity;
import com.example.mainactivity.Database.entity.TipsEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ListView lvDevices;
    private Toolbar materialToolbar;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayList<String> connectedDevices = new ArrayList<>();
    private Set<String> deviceIps = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();
        //TODO: UNDO MODIFICATIONS
        addConnectedDevice("192.168.50.76", "EC:E3:34:D1:60:7C");
        loadConnectedDevices();
        checkNewDevice();
        populateLocalDatabase();
    }

    private void setupViews() {
        // Initialize views
        lvDevices = findViewById(R.id.lvDevices);
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, connectedDevices);
        lvDevices.setAdapter(deviceAdapter);

        //setup action bar
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            ColorDrawable colorDrawable = new ColorDrawable(getColor(R.color.primary_color));
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(fromHtml("List of Starters",getColor(R.color.on_primary_color)));
        }


        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = connectedDevices.get(position);
            String deviceIp = deviceInfo.split(" - ")[1]; // Extract IP from display string
            String deviceMac = deviceInfo.split(" - ")[2]; //get the mac address

            Intent intent = new Intent(MainActivity.this, DeviceDataActivity.class);
            intent.putExtra("DEVICE_IP", deviceIp);
            intent.putExtra("DEVICE_MAC", deviceMac);
            startActivity(intent);
        });

        Button btnAddDevice = findViewById(R.id.gotobluetooth);
        btnAddDevice.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
            startActivity(intent);
        });

        Button btnReset = findViewById(R.id.reset_button);
        btnReset.setOnClickListener(v -> {
            clearConnectedDevices();
        });
    }

    private void loadConnectedDevices() {
        SharedPreferences prefs = getSharedPreferences("DEVICE_PREFS", MODE_PRIVATE);
        Set<String> savedDevices = prefs.getStringSet("CONNECTED_DEVICES", new HashSet<>());
        connectedDevices.clear();
        connectedDevices.addAll(savedDevices);
        deviceAdapter.notifyDataSetChanged();
    }

    private void checkNewDevice() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("ESP32_IP")) {
            String newIp = intent.getStringExtra("ESP32_IP");
            String newMac = intent.getStringExtra("ESP32_MAC");
            addConnectedDevice(newIp, newMac);
        }
    }
    //modify to add placeholders
    private void addConnectedDevice(String ip, String Mac) {
        String deviceInfo = "ESP32 Device - " + ip + " - " + Mac;
        if (!connectedDevices.contains(deviceInfo)) {
            connectedDevices.add(deviceInfo);
            saveConnectedDevices();
            deviceAdapter.notifyDataSetChanged();
        }
    }

    private void saveConnectedDevices() {
        SharedPreferences prefs = getSharedPreferences("DEVICE_PREFS", MODE_PRIVATE);
        Set<String> devicesSet = new HashSet<>(connectedDevices);
        prefs.edit().putStringSet("CONNECTED_DEVICES", devicesSet).apply();
    }

    private void clearConnectedDevices() {
        connectedDevices.clear();
        saveConnectedDevices();
        deviceAdapter.notifyDataSetChanged();
    }

    private void populateLocalDatabase(){
        //initialize database
        AppDatabase db = AppDatabase.getInstance(this);

        //info table
        db.infoDao().insertInfo(new InfoEntity(0, "Requirements", "-Large mason jar (Greater than 750ml)\r\n-Whole wheat flour\r\n-All purpose or bread flour\r\n-Digital scale\r\n-small rubber spatula"));
        db.infoDao().insertInfo(new InfoEntity(1, "Day 1", "To a glass jar add: \r\n-60g Whole wheat flour\r\n-60g water\r\n-Mix together well\r\nYields approx. 120g starter\r\nRest 24h at 70-75°F/21-24°C"));
        db.infoDao().insertInfo(new InfoEntity(2, "Day 2", "Let rest for 24 hours stirring once or twice to oxygenate the mixture. You may or may not see bubbles. Either way is OK."));
        db.infoDao().insertInfo(new InfoEntity(3, "Day 3", "Discard half (60g) Feed (add): 60g AP or Bread flour 60g water Yields approx. 180g starter Rest 24 hrs at 70-75°F/21-24°C"));
        db.infoDao().insertInfo(new InfoEntity(4, "Day 4", "Discard half (90g) Feed (add): 60g AP or Bread flour 60g water Yields approx. 210g starter Rest 24 hrs at 70-75°F/21-24°C"));
        db.infoDao().insertInfo(new InfoEntity(5, "Day 5", "Discard half (105g) Feed (add): 60g AP or Bread flour 60g water Yields approx. 225g starter Rest 24 hrs at 70-75°F/21-24°C"));
        db.infoDao().insertInfo(new InfoEntity(6, "Day 6", "Discard half (112g) Feed (add): 60g AP or Bread flour 60 g water Yields approx. 233g starter Rest 24 hrs at 70-75°F/ 21-24°C"));
        db.infoDao().insertInfo(new InfoEntity(7, "Day 7", "Discard half (116g) Feed (add): 60g AP or Bread flour 60g water Yields approx. 236g starter Rest at 70-75°F/21-24°C until active and bubbling. It should then be ready to use!"));
        db.infoDao().insertInfo(new InfoEntity(8, "Day 7+", "On day 7+, up to 6 hours after feeding, your starter might be active. An active starter will double in size and have lots of bubbles on the surface. It’s now ready to use!\r\nIf your starter has NOT doubled in size, feed every 8-12 hours (not 24) and continue the same formula: Discard half starter. Feed (Add): 60g flour & 60g water at 70-75°F / 21-24°C. Too runny? Add an additional 1-2 tbs of flour." ));

        //TODO: tips table
        db.tipsDao().insertTips(new TipsEntity(0, "default", "Tips"));
    }

}