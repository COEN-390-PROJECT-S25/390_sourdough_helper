package com.example.mainactivity;

import static android.text.style.TtsSpan.ARG_DAY;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SensorFragment extends Fragment {
    private static final String ARG_MAC = "device_mac";
    private static final String ARG_FIELD = "field_name";
    private static final String ARG_UNIT = "field_unit";
    private LineChart chart;
    private DatabaseReference dbRef;
    private int currentDay;
    private int attempt;
    private TextView tvNoData;
    private static final float MAX_HEIGHT = 180.0f; // Adjust this value as needed


    public static SensorFragment newInstance(String deviceMac, String fieldName, String unit, int day, int attempt) {
        SensorFragment fragment = new SensorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MAC, deviceMac);
        args.putString(ARG_FIELD, fieldName);
        args.putString(ARG_UNIT, unit);
        args.putInt(ARG_DAY, day);
        args.putInt("ATTEMPT", attempt);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor, container, false);
        chart = view.findViewById(R.id.chart);
        currentDay = getArguments().getInt(ARG_DAY, 1);
        tvNoData = view.findViewById(R.id.tvNoData); // Initialize the no data text
        attempt = getArguments().getInt("ATTEMPT",1);
        //this class is based on making the chart. this is for each sensor.
        setupChart();
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        //make sure to refresh twith the right data
        setupChart();
    }

    private void setupChart() {
        Bundle args = getArguments();
        String deviceMac = args.getString(ARG_MAC);
        String fieldName = args.getString(ARG_FIELD);
        String unit = args.getString(ARG_UNIT);

        //DONE:  MAKE SURE YOU CAN CYCLE PER DAY, AND DECTECT THE DAY YOU ARE REPRESENTING ON THE CHARTS.
        // DRILLDOWN REQUIRED>
        //get the database
        dbRef = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac + "/attempt_" + attempt + "/day_" + currentDay);
        //when a new value is added to the database, to that specific device,
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> entries = new ArrayList<>();
                if (!snapshot.exists()) {
                    // No data available for this day
                    showNoDataMessage();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getKey().equals("start_data")) {
                        continue; // Skip the start node
                    }
                    //the values may vary so here we can manage it.
                    Object value = child.child(fieldName).getValue();
                    Long timestamp = child.child("timestamp").getValue(Long.class);

                    //we want our chart to only display float values.
                    //not all values are floats, we must cast it appropriately.
                    //we can make a class later if we want to condense this.
                    if (value != null && timestamp != null) {
                        float floatValue = 0;
                        if (value instanceof Integer) {
                            floatValue = ((Integer) value).floatValue();
                        } else if (value instanceof Long) {
                            floatValue = ((Long) value).floatValue();
                        } else if (value instanceof Double) {
                            floatValue = ((Double) value).floatValue();
                        } else if (value instanceof Float) {
                            floatValue = (Float) value;
                        }
                        // Special handling for height data
                        if (fieldName.equals("height")) {
                            floatValue = ((MAX_HEIGHT - floatValue) / 10.0f);
                            floatValue = Math.round(floatValue * 10) / 10.0f;
                        }else{

                        }

                        entries.add(new Entry(entries.size(), floatValue));
                    }
                }

                if (entries.isEmpty()) {
                    showNoDataMessage();
                } else {
                    hideNoDataMessage();
                    //send the new values.
                    //updates each time there is a new value added to database.
                    updateChart(entries, fieldName, unit);
                }
            }

            //error handling.
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", error.getMessage());
            }
        });
    }

    private void showNoDataMessage() {
        chart.setVisibility(View.GONE);
        tvNoData.setVisibility(View.VISIBLE);
    }

    private void hideNoDataMessage() {
        chart.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
    }
    public void refreshData(int day) {
        currentDay = day;
        Bundle args = getArguments();
        if (args != null) {
            args.putInt(ARG_DAY, day);
        }

        // Force a refresh by reattaching the listener
        setupChart();
    }

    //update function
    private void updateChart(List<Entry> entries, String label, String unit) {
        LineDataSet dataSet = new LineDataSet(entries, label + " (" + unit + ")");
        //customized so that the chart only shows a line
        dataSet.setColor(getColorForLabel(label));  //color depends on the type of measurement
        dataSet.setDrawCircles(false);              //hides the dots
        //dataSet.setCircleRadius(4f);              //you can have dots, now no need.
        dataSet.setLineWidth(1f);                   //line width
        // Add these lines to disable value drawing
        dataSet.setDrawValues(false); // This hides the numbers above points
        dataSet.setHighlightEnabled(false); // Optional: disables highlighting on touch

        //setup the chart with the data
        LineData lineData = new LineData(dataSet);


        //x axis details
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);  //on the bottom of chart
        xAxis.setLabelRotationAngle(-15); //rotate the label
        xAxis.setLabelCount(4);
        xAxis.setAvoidFirstLastClipping(true); //no clipping
        //should list the time as such:
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Convert sample index to minutes (30 seconds per sample = 0.5 minutes)
                //float minutes = value * 0.5f;
                int hours = (int) (value / 60);
                int mins = (int) (value % 60);
                return String.format(Locale.getDefault(), "%02d:%02d", hours, mins);
            }
        });

        //y axis details
        chart.getAxisRight().setEnabled(false); //remove the lhs axis
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        //only show the units you need according to the chart you are on
        yAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f %s", value, unit);
            }
        });

        //set the legend on top of the chart
        Legend legend = chart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        //chart settings
        chart.setData(lineData);
        chart.setExtraBottomOffset(30f);
        chart.invalidate();

        Description desc = new Description();
        desc.setText("Time since the start of the day (hours)");  // Your X-axis label
        desc.setTextSize(12f);
        desc.setPosition(chart.getWidth()/1.10f, chart.getHeight() - 10f); // Bottom center
        chart.setDescription(desc);

    }

    //this will tell the color we want.
    private int getColorForLabel(String label) {
        switch (label) {
            case "co2":
                return Color.GRAY;
            case "height":
                return Color.RED;
            case "humidity":
                return Color.GREEN;
            case "temperature":
                return Color.CYAN;
            default: return Color.BLACK;
        }
    }
}