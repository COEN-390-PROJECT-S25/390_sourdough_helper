package com.example.mainactivity;

import android.os.Bundle;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataGraphActivity extends AppCompatActivity {
    private TextView tvDeviceMessage, tvDuration, tvStartTime, tvCurrentTime;
    private String deviceMac;
    private Button databaseReset;
    private Spinner daySpinner;
    private int selectedDay = 1; // Default to day 1
    private ViewPager2 viewPager;

    // Variables to track timestamps
    private long firstTimestamp = -1;
    private long lastTimestamp = -1;
    private int attempt = 1;
    boolean first_open = true;
    int current_day = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_graph);

        toolbar_setup();

        //we are not using IP anymore. we use mac for database recognition
        deviceMac = getIntent().getStringExtra("DEVICE_MAC");
        current_day = getIntent().getIntExtra("DAY",1);
        selectedDay = current_day;
        attempt = getIntent().getIntExtra("ATTEMPT", 1);

        //text views about which device is connected.
        tvDeviceMessage = findViewById(R.id.device_data_start_textview);
        tvDeviceMessage.setText("Device from " + deviceMac);

        tvStartTime = findViewById(R.id.data_start_time);
        tvDuration = findViewById(R.id.data_duration);
        tvCurrentTime = findViewById(R.id.data_current_time);

        //setup a method to track duration.
        setupTimestampTracker();

        //setup the pager and the tab layout
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        //the adapter is each different charts, give the mac address
        SensorPagerAdapter adapter = new SensorPagerAdapter(this, deviceMac, selectedDay, attempt);
        viewPager.setAdapter(adapter);

        //the tabs are used to navigate between charts.
        SensorPagerAdapter finalAdapter = adapter;
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(finalAdapter.getTabTitle(position));
        }).attach();

        //DONE:
        // WHEN CYCLING THROUGH THE TABS AND CHANGING THE DAY FOR THE FIRST TIME, IT DOESNT WORK
        // YOU MUST RECHANGE THE DAY TO MAKE EACH TAB CHANGE
        // OR PRESS A TAB FIRST THEN CHANGE THE DAY.
        // MAKE IT SO THAT IT CHANGES TO THE DAY AUTMATICALLY TO AVOID ISSUES?
        // BUT THIS DOESNT NECESSARILY FIX IT YOU STILL NEED TO BE CONSISTENT AND AVOID CHANGING TABS FIRST TO  CHANGE DAY PROPERLY
        daySpinner = findViewById(R.id.daySpinner);
        setupDaySpinner();

        // Modify viewPager initialization
        viewPager = findViewById(R.id.viewPager);
        adapter = new SensorPagerAdapter(this, deviceMac, selectedDay, attempt);
        viewPager.setAdapter(adapter);

//        //reset button, resets the data on the
//        databaseReset = findViewById(R.id.db_reset);
//        databaseReset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                resetDeviceData();
//            }
//        });
//        //database test move later
//        mDatabase = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac);
//        mDatabase.addValueEventListener(new ValueEventListener() {
//        @Override
//        public void onDataChange(@NonNull DataSnapshot snapshot) {
//            List<Entry> dataPoints = new ArrayList<>();
//
//            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
//                Float temp = childSnapshot.child("sht31_temp").getValue(Float.class);
//                Long timestamp = childSnapshot.child("timestamp").getValue(Long.class);
//
//                if (temp != null && timestamp != null) {
//                    // Convert timestamp to hours/minutes for X-axis
//                    String time = convertTimestampToTime(timestamp);
//                    dataPoints.add(new Entry(dataPoints.size(), temp)); // X = index, Y = temp
//                }
//            }
//            updateChart(dataPoints); // Update the chart (see next step)
//        }
//
//        @Override
//        public void onCancelled(@NonNull DatabaseError error) {
//            Log.e("Firebase", "Error fetching data", error.toException());
//        }
//    });
    }

    //DONE: MAKE A DRILL DOWN ON THE GRAPHS TO CHOOSE PER DAY.

    private void setupDaySpinner() {
        try {
            DatabaseReference deviceRef = FirebaseDatabase.getInstance()
                    .getReference("sensors/" + deviceMac);

            deviceRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> days = new ArrayList<>();

                    for (int day = 1; day <= 7; day++) {
                        if (snapshot.child("attempt_" + attempt).child("day_" + day).exists()) {
                            days.add("Day " + day);
                        }
                    }

                    if (days.isEmpty()) {
                        days.add("No data available");
                        daySpinner.setEnabled(false);
                    }



                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            DataGraphActivity.this,
                            android.R.layout.simple_spinner_item,
                            days);



                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    daySpinner.setAdapter(spinnerAdapter);



                    // Add this spinner selection listener
                    daySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // Extract day number from "Day X" string
                            if (first_open){
                                selectedDay = current_day;
                                int spinnerPosition = spinnerAdapter.getPosition("Day " + current_day);
                                daySpinner.setSelection(spinnerPosition);
                                first_open = false;
                            }else{
                                String selected = (String) parent.getItemAtPosition(position);
                                selectedDay = Integer.parseInt(selected.replace("Day ", ""));
                            }

                            refreshDataForSelectedDay();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // Do nothing
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DataGraphActivity.this, "Error checking days", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("SpinnerError", "Error setting up spinner", e);
            Toast.makeText(this, "Error initializing day selection", Toast.LENGTH_SHORT).show();
        }
    }
    private void refreshDataForSelectedDay() {

        // Refresh timestamp tracking
        setupTimestampTracker();

        // Notify all fragments to refresh their data
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof SensorFragment) {
                ((SensorFragment) fragment).refreshData(selectedDay);
            }
        }
    }
    private void setupTimestampTracker() {
        //get reference
        DatabaseReference deviceReadingsRef = FirebaseDatabase.getInstance()
                .getReference("sensors/" + deviceMac + "/attempt_" + attempt + "/day_" + selectedDay);

        //track data changes on the database
        deviceReadingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvStartTime.setText("No data available for Day " + selectedDay);
                    tvCurrentTime.setText("N/A");
                    tvDuration.setText("N/A");
                    return;
                }
                firstTimestamp = -1;
                lastTimestamp = -1;
                //fetch the data, if it was reset, you will be able to see it is resetted.
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    Long timestamp = childSnapshot.child("timestamp").getValue(Long.class);

                    if (timestamp != null) {
                        if (firstTimestamp == -1 || timestamp < firstTimestamp) {
                            firstTimestamp = timestamp;
                        }
                        if (lastTimestamp == -1 || timestamp > lastTimestamp) {
                            lastTimestamp = timestamp;
                        }
                    }
                }
                //go update the textviews
                updateDurationInfo();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DataGraphActivity.this, "Error reading timestamps", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateDurationInfo() {
        if (firstTimestamp == -1 || lastTimestamp == -1) {
            tvStartTime.setText("No dough started yet");
            tvCurrentTime.setText("N/A");
            tvDuration.setText("N/A");
            return;
        }

        //format time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String startTime = sdf.format(new Date(firstTimestamp * 1000L));
        String currentTime = sdf.format(new Date(lastTimestamp * 1000L));
        //calculate the duration
        long duration = lastTimestamp - firstTimestamp;

        //format the time accordingly. from seconds to days.
        String durationText = formatDuration(duration);

        // Update the TextView
        tvStartTime.setText("Started: " + startTime);
        tvCurrentTime.setText("Current: " + currentTime);
        tvDuration.setText("Duration:" + durationText);

    }
    private String formatDuration(long seconds) {
        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format(Locale.getDefault(), "%d days, %d hours", days, hours);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hours, %d minutes", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d minutes, %d seconds", minutes, secs);
        } else {
            return String.format(Locale.getDefault(), "%d seconds", secs);
        }
    }

    private void toolbar_setup() {
        ActionBar actionBar = getSupportActionBar();
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#cc8e90"));
        actionBar.setBackgroundDrawable(colorDrawable);
        //different name, profile page
        actionBar.setTitle(Html.fromHtml("<font color='#ffffff'>Starter Data Graphs </font>"));
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.data_graphs_toolbar, menu);
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
    private void resetDeviceData() {
        //get the database reference for the mac_address
        DatabaseReference deviceReadingsRef = FirebaseDatabase.getInstance()
                .getReference("sensors/" + deviceMac + "/day_1");

        //ask the user the confirm delete
        new AlertDialog.Builder(this)
                .setTitle("Clear Device Readings")
                .setMessage("This will reset all data.\n Do you wish to continue?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    //removes the lid device's data in the firebase only
                    deviceReadingsRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cleared data", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to clear data" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
//    Helper method to format timestamp
//    private String convertTimestampToTime(long timestamp) {
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
//        return sdf.format(new Date(timestamp * 1000L)); // Convert seconds to milliseconds
//    }
//
//    private void updateChart(List<Entry> dataPoints) {
//        LineChart lineChart = findViewById(R.id.temperatureChart);
//
//        LineDataSet dataSet = new LineDataSet(dataPoints, "Temperature (°C)");
//        dataSet.setColor(Color.RED);
//        dataSet.setValueTextColor(Color.BLACK);
//        dataSet.setLineWidth(2f);
//
//        LineData lineData = new LineData(dataSet);
//        lineChart.setData(lineData);
//
//        // Customize X-axis (time)
//        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setValueFormatter(new ValueFormatter() {
//            @Override
//            public String getFormattedValue(float value) {
//                return String.format("%.0f:00", value); // Format as "HH:00"
//            }
//        });
//
//        lineChart.invalidate(); // Refresh chart
//    }
}