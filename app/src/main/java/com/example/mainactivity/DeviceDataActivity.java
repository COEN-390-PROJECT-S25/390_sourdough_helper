package com.example.mainactivity;

import static android.view.View.GONE;
import static androidx.core.text.HtmlCompat.fromHtml;
import static androidx.core.util.TimeUtils.formatDuration;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DeviceDataActivity extends AppCompatActivity implements FeedingDialogFragment.OnFeedingDialogListener{
    private TextView tvstarthumidity, tvstartco2, tvstarttemperature, tvstartheight;
    private TextView tvmaxhumidity, tvmaxco2, tvmaxtemperature, tvmaxheight;
    //general info textviews
    private TextView tvhours, tvday, tvready, tvstartername;
    private float max_humidity, max_co2, max_temperature, max_height;
    private String deviceName = "Missing name!";
    private String deviceIp, deviceMac;
    private Button btnStop, btnStart, btnNew, btnNextDay, btnGraphData;
    private DatabaseReference databaseReference;
    private Integer currentDay;
    private long firstTimestamp = -1;
    private long lastTimestamp = -1;
    private int attempt = 1;

    private Boolean ready_state = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_data);

        setupUI();
        toolbar_setup();
        fetchStartData();
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

        //summary textview
        //should show the start data,
        //should show the peak data
        //should show how long it has been running for
        //ready or not status

        //start data
        tvstarthumidity = findViewById(R.id.device_data_start_humidity);
        tvstartco2= findViewById(R.id.device_data_start_co2);
        tvstarttemperature= findViewById(R.id.device_data_start_temp);
        tvstartheight= findViewById(R.id.device_data_start_height);
        //max data
        tvmaxhumidity = findViewById(R.id.device_data_max_humidity);
        tvmaxco2 = findViewById(R.id.device_data_max_co2);
        tvmaxtemperature = findViewById(R.id.device_data_max_temp);
        tvmaxheight= findViewById(R.id.device_data_max_height);
        //general data
        tvhours = findViewById(R.id.device_data_duration_textview);
        tvday = findViewById(R.id.device_data_current_day_textview);
        tvready = findViewById(R.id.device_data_result_textview);
        tvstartername = findViewById(R.id.device_data_starter_name);



        databaseReference = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac);

        databaseReference.child("general").child("device_name").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    String name = snapshot.getValue(String.class);
                    tvstartername.setText(name);
                }
            }
        });

        databaseReference.child("general").child("attempt").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    attempt = snapshot.getValue(Integer.class);
                }
            }
        });

        //buttons
        btnStart = findViewById(R.id.device_data_start_button);
        btnNew = findViewById(R.id.device_data_new_button);

        btnStart.setOnClickListener(v -> {
            databaseReference.child("general").child("current_day").get().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || !task.getResult().exists()) {
                    Toast.makeText(this, "Failed to check current day", Toast.LENGTH_SHORT).show();
                    return;
                }

                Integer currentDay = task.getResult().getValue(Integer.class);
                if (currentDay == null || currentDay == 0) {
                    Toast.makeText(this, "You need to start a new dough first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                ///SET DATABASE READING CHANGER THING HERE. WHEN YOU CHANGE IT SHOULD REACT TO IT.
                // TODO: SET A LISTENER HERE? OR HARD MANUALLY MODIFY IT?
                //  MAKE A BUNDLE? MAKE A WAY TO JUST SEND 2 PIECE OF DATA BACK TO THE ACTIVITIES?
                //  CHANGE THE TEXTVIEW TO NOT READY AFTER PRESSING STARTER FED
                //  CHANGE THE ENABLE BUTTON TO BE FOR DISABLING AFTER PRESSING STARTER FED
                databaseReference.child("general").child("enable").get().addOnCompleteListener(enableTask -> {
                    if (!enableTask.isSuccessful() || !enableTask.getResult().exists()) {
                        Toast.makeText(this, "Failed to check device status", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Boolean isEnabled = enableTask.getResult().getValue(Boolean.class);
                    boolean newState = !Boolean.TRUE.equals(isEnabled); // Toggle the state

                    // Update UI first for responsiveness
                    if (newState) {
                        btnStart.setText("Disable Lid");
                        btnStart.setBackgroundColor(Color.parseColor("#B9375D")); // Red for disable
                    } else {
                        btnStart.setText("Enable Lid");
                        btnStart.setBackgroundColor(Color.parseColor("#689B8A")); // Green for enable
                    }

                    // Update database
                    databaseReference.child("general").child("enable").setValue(newState)
                            .addOnSuccessListener(aVoid -> {
                                String message = newState ? "Device enabled!" : "Device disabled!";
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Operation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // Revert UI if update fails
                                if (Boolean.TRUE.equals(isEnabled)) {
                                    btnStart.setText("Disable Lid");
                                    btnStart.setBackgroundColor(Color.parseColor("#B9375D"));
                                } else {
                                    btnStart.setText("Enable Lid");
                                    btnStart.setBackgroundColor(Color.parseColor("#689B8A"));
                                }
                            });
                });
            });
        });

        btnNew.setOnClickListener(v -> {
            new AlertDialog.Builder(DeviceDataActivity.this)
                .setTitle("Confirm Restart")
                .setMessage("Are you sure you want to start a new dough?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User confirmed - perform restart
                    databaseReference.child("general").child("attempt").get().addOnCompleteListener(attemptTask -> {
                        if (attemptTask.isSuccessful()) {
                            DataSnapshot attemptSnapshot = attemptTask.getResult();
                            Integer currentAttempt = attemptSnapshot.exists() ? attemptSnapshot.getValue(Integer.class) : 0;
                            if (currentAttempt == null) currentAttempt = 0;

                            // Update values
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("current_day", 0);
                            updates.put("enable", false);
                            updates.put("attempt", currentAttempt + 1);

                            databaseReference.child("general").updateChildren(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(DeviceDataActivity.this, "Ready for new dough! \n Go check the feeding instructions!", Toast.LENGTH_SHORT).show();
                                        ready_state = true;
                                        tvready.setText("Go check the instructions for day 1's feeding!");

                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(DeviceDataActivity.this, "Failed to reset! " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
        });

        //setup action bar
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            ColorDrawable colorDrawable = new ColorDrawable(getColor(R.color.primary_color));
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(fromHtml("Data Dashboard",getColor(R.color.on_primary_color)));
        }

        //graphed data button click
        btnGraphData = findViewById(R.id.buttonGraphedData);
        btnGraphData.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceDataActivity.this, DataGraphActivity.class);
            intent.putExtra("DEVICE_MAC",deviceMac);
            intent.putExtra("DAY", currentDay);
            intent.putExtra("ATTEMPT", attempt);
            //in the xml we use Singletop to prevent loss of data per pressing back buttons betweent
            //activities.
            startActivity(intent);
        });
        //feeding instructions button click
        findViewById(R.id.buttonFeedingInstructions).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("MAC", deviceMac);
            args.putBoolean("READY", ready_state);
            args.putInt("ATTEMPT", attempt);
            //open feeding dialog fragment
            FeedingDialogFragment feedingDialogFragment = new FeedingDialogFragment();
            feedingDialogFragment.setArguments(args);
            feedingDialogFragment.show(getSupportFragmentManager(), "FeedingDialogFragment");
        });

    }

    private void evaluateDoughStatus() {
        //get start data
        databaseReference.child("attempt_" + attempt).child("day_" + currentDay).child("start_data").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot startSnapshot) {
                if (startSnapshot.exists()) {
                    // Get start values
                    Float startHumidity = startSnapshot.child("humidity").getValue(Float.class);
                    Float startCo2 = startSnapshot.child("co2").getValue(Float.class);
                    Float startTemperature = startSnapshot.child("temperature").getValue(Float.class);
                    Float startHeight = startSnapshot.child("height").getValue(Float.class);

                    if (startHumidity == null || startCo2 == null || startTemperature == null || startHeight == null) {
                        tvready.setText("Incomplete start data");
                        return;
                    }

                    //used the max and start data to  compare.
                    String statusMessage = analyzeDoughProgress(
                            startHumidity, max_humidity,
                            startCo2, max_co2,
                            startTemperature, max_temperature,
                            startHeight, max_height
                    );

                    tvready.setText(statusMessage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvready.setText("Error reading start data");
            }
        });
    }

    private String analyzeDoughProgress(float startHumidity, float maxHumidity,
                                        float startCo2, float maxCo2,
                                        float startTemp, float maxTemp,
                                        float startHeight, float maxHeight) {
        // Convert heights to actual dough height in mm
        float doughStartHeight = (180.0f - startHeight) / 10.0f;
        float doughMaxHeight = (180.0f - maxHeight) / 10.0f;
        String extra_message;
        // Calculate percentage changes
//        float humidityChange = ((maxHumidity - startHumidity) / startHumidity) * 100;
        float co2Change = ((maxCo2 - startCo2) / startCo2) * 100;
//        float tempChange = ((maxTemp - startTemp) / startTemp) * 100;
        float heightChange = ((doughMaxHeight - doughStartHeight) / doughStartHeight) * 100;

        // Define thresholds (you can adjust these based on your requirements)
//        final float GOOD_HUMIDITY_CHANGE = 10f; // %
        final float GOOD_CO2_CHANGE = 100f; // %
//        final float GOOD_TEMP_CHANGE = 5f; // %
        final float GOOD_HEIGHT_CHANGE = 50f; // %

        // Evaluate each parameter
//        boolean goodHumidity = humidityChange >= GOOD_HUMIDITY_CHANGE;
        boolean goodCo2 = co2Change >= GOOD_CO2_CHANGE;
//        boolean goodTemp = tempChange >= GOOD_TEMP_CHANGE;
        boolean goodHeight = heightChange >= GOOD_HEIGHT_CHANGE;

        if (goodCo2 && goodHeight) {
            ready_state = true;
            return "Dough is ready! Excellent fermentation.";
        } else if (goodHeight || goodCo2) {
            ready_state = false;
            return "Dough is progressing well.";
        } else {
            ready_state = false;
            return "Dough needs more time to ferment.";
    }


    }
    private void checkEnabled() {
        databaseReference.child("general").child("enable").get().addOnCompleteListener(enableTask -> {
            if (!enableTask.isSuccessful() || !enableTask.getResult().exists()) {
                Toast.makeText(this, "Failed to check device status", Toast.LENGTH_SHORT).show();
                return;
            }

            Boolean isEnabled = enableTask.getResult().getValue(Boolean.class);
            boolean newState = Boolean.TRUE.equals(isEnabled); // Toggle the state

            // Update UI first for responsiveness
            if (newState) {
                btnStart.setText("Disable Lid");
                btnStart.setBackgroundColor(Color.parseColor("#B9375D")); // Red for disable
            } else {
                btnStart.setText("Enable Lid");
                btnStart.setBackgroundColor(Color.parseColor("#689B8A")); // Green for enable
            }
        });
    }
    private void fetchStartData() {
        databaseReference.child("general").child("current_day").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentDay = snapshot.getValue(Integer.class);
                    if (currentDay == 0){
                        tvready.setText("Go check the instructions for day 1's feeding!");
                        ready_state = true;
                    }
                    if (currentDay != null && currentDay > 0) {
                        // Fetch start data for the current day
                        databaseReference.child("attempt_" + attempt).child("day_" + currentDay).child("start_data").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot startSnapshot) {
                                if (startSnapshot.exists()) {
                                    // Get start values
                                    System.out.println("I HAVE FOUND THE START DATA!!! DAY IS: " + currentDay);
                                    Float startHumidity = startSnapshot.child("humidity").getValue(Float.class);
                                    Float startCo2 = startSnapshot.child("co2").getValue(Float.class);
                                    Float startTemperature = startSnapshot.child("temperature").getValue(Float.class);
                                    Float startHeight = startSnapshot.child("height").getValue(Float.class);

                                    System.out.println("1: "  + startHumidity + " 2: " + startCo2 + " 3: " + startTemperature + " 4: "+ startHeight);
                                    // Update UI with start values
                                    if (startHumidity != null) {
                                        tvstarthumidity.setText(String.format("%.1f%%", startHumidity));
                                    }
                                    if (startCo2 != null) {
                                        tvstartco2.setText(String.format("%.1f ppm", startCo2));
                                    }
                                    if (startTemperature != null) {
                                        tvstarttemperature.setText(String.format("%.1f°C", startTemperature));
                                    }
                                    if (startHeight != null) {
                                        tvstartheight.setText(String.format("%.1f mm", ((180.0f -startHeight)/10.0f)));
                                    }
                                    if (currentDay == 8){
                                        tvday.setText("The dough is currently at Day 7+");
                                        btnGraphData.setVisibility(GONE);
                                    }else{
                                        tvday.setText("The dough is currently at Day " + currentDay);
                                    }

                                    fetchMaxData(currentDay);
                                    setupTimestampTracker();
                                    checkEnabled();
                                    databaseReference.child("general").child("device_name").addListenerForSingleValueEvent(new ValueEventListener() {
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                deviceName = snapshot.getValue(String.class);
                                                tvstartername.setText(deviceName);
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(DeviceDataActivity.this, "Failed to read current day: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(DeviceDataActivity.this, "Failed to read start data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeviceDataActivity.this, "Failed to read current day: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setupTimestampTracker() {
        //get reference
        DatabaseReference deviceReadingsRef = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac + "/attempt_" + attempt + "/day_" + currentDay);

        //track data changes on the database
        deviceReadingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvhours.setText("No data available for Day " + currentDay);
                    return;
                }

                // Count the number of data points
                long dataPointCount = snapshot.getChildrenCount();

                // Calculate duration in minutes
                long durationMinutes = dataPointCount-1;

                // Update the text view
                updateDurationInfo(durationMinutes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeviceDataActivity.this, "Error reading data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateDurationInfo(long durationMinutes) {
        if (durationMinutes == 0) {
            tvhours.setText("No dough started yet");
            return;
        }

        // Convert minutes to hours and minutes
        long hours = durationMinutes / 60;
        long minutes = durationMinutes % 60;

        // Format the duration text
        String durationText;
        if (hours > 0) {
            durationText = String.format(Locale.getDefault(), "%d hours, %d minutes", hours, minutes);
        } else {
            durationText = String.format(Locale.getDefault(), "%d minutes", minutes);
        }

        // Update the TextView
        tvhours.setText("Duration: " + durationText);
    }
    private void fetchMaxData(int currentDay) {
        DatabaseReference dayRef = databaseReference.child("attempt_" + attempt).child("day_" + currentDay);

        // Create a holder class for our max values
        class MaxValues {
            float humidity = Float.MIN_VALUE;
            float co2 = Float.MIN_VALUE;
            float temperature = Float.MIN_VALUE;
            float height = Float.MAX_VALUE;
        }

        final MaxValues maxValues = new MaxValues();

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot daySnapshot) {
                if (daySnapshot.exists()) {
                    //skip start data
                    for (DataSnapshot entrySnapshot : daySnapshot.getChildren()) {
                        if (entrySnapshot.getKey().equals("start_data")) {
                            continue; // Skip the start node
                        }

                        //get values from entry
                        Float humidity = entrySnapshot.child("humidity").getValue(Float.class);
                        Float co2 = entrySnapshot.child("co2").getValue(Float.class);
                        Float temperature = entrySnapshot.child("temperature").getValue(Float.class);
                        Float height = entrySnapshot.child("height").getValue(Float.class);

                        //update the values
                        if (humidity != null && humidity > maxValues.humidity) {
                            maxValues.humidity = humidity;
                        }
                        if (co2 != null && co2 > maxValues.co2) {
                            maxValues.co2 = co2;
                        }
                        if (temperature != null && temperature > maxValues.temperature) {
                            maxValues.temperature = temperature;
                        }
                        if (height != null && height < maxValues.height) {
                            maxValues.height = height;
                        }
                    }

                    // Update UI with max values
                    runOnUiThread(() -> {
                        tvmaxhumidity.setText(String.format("%.1f%%", maxValues.humidity));
                        tvmaxco2.setText(String.format("%.1f ppm", maxValues.co2));
                        tvmaxtemperature.setText(String.format("%.1f°C", maxValues.temperature));
                        tvmaxheight.setText(String.format("%.1f mm", ((180.0f - maxValues.height)/10.0f)));

                        //update class variables
                        max_humidity = maxValues.humidity;
                        max_co2 = maxValues.co2;
                        max_temperature = maxValues.temperature;
                        max_height = maxValues.height;

                        evaluateDoughStatus();
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                runOnUiThread(() ->
                        Toast.makeText(DeviceDataActivity.this,
                                "Failed to read max data: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
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
        //TODO: FIX THIS LATER WIFI ISSUE RECONNECTION.
//        else if (item.getItemId() == R.id.action_setup_connection){
//            Intent intent = new Intent(DeviceDataActivity.this, BluetoothActivity.class);
//            startActivity(intent);
//            return true;
//        }
        else if (item.getItemId() == R.id.action_rename_device){
            //open dialog fragment to rename device
            DeviceNameDialogFragment deviceNameDialogFragment = new DeviceNameDialogFragment();
            Bundle args = new Bundle();
            args.putString("MAC", deviceMac);
            deviceNameDialogFragment.setArguments(args);
            deviceNameDialogFragment.show(getSupportFragmentManager(), "DeviceNameDialogFragment");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogDismissed() {
        // Refresh data when dialog is closed
        fetchStartData();
    }

    @Override
    public void onStarterFed(int newDay) {
        runOnUiThread(() -> {
            // Update the button state
            btnStart.setText("Disable Lid");
            btnStart.setBackgroundColor(Color.parseColor("#B9375D"));

            // Update the day and status text
            currentDay = newDay;
            if (currentDay == 8) {
                tvday.setText("The dough is currently at Day 7+");
                tvready.setText("Dough is ready for baking!");
                btnGraphData.setVisibility(GONE);
            } else {
                tvday.setText("The dough is currently at Day " + currentDay);
                tvready.setText("Dough needs more time to ferment");
            }

            // Force a refresh of the data
            fetchStartData();
        });
    }
}