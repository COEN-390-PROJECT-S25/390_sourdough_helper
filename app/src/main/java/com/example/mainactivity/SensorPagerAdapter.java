package com.example.mainactivity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SensorPagerAdapter extends FragmentStateAdapter {

    private final String deviceMac;
    private final String[] tabTitles = {"CO₂","Temperature", "Humidity", "Height"};
    private int currentDay, attempt;
    public SensorPagerAdapter(FragmentActivity fa, String deviceMac, int day, int attempt) {
        super(fa);
        //this class is the one to handle the different charts you can display. Not the chart itself
        //only the one who controls which chart is shown.
        //we are passed the mac device so that the actual chart can grab data.
        this.deviceMac = deviceMac;
        this.currentDay = day;
        this.attempt = attempt;
    }
    public void setDay(int day) {
        this.currentDay = day;
        notifyDataSetChanged();
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            //each chart has a number we decide, here is a layout.
            //this will create a new instance of charts each time you scroll to a new position.
            case 0: return SensorFragment.newInstance(deviceMac, "co2", "ppm", currentDay, attempt);
            case 1: return SensorFragment.newInstance(deviceMac, "temperature", "°C", currentDay,attempt);
            case 2: return SensorFragment.newInstance(deviceMac, "humidity", "%", currentDay,attempt);
            case 3: return SensorFragment.newInstance(deviceMac, "height", "cm", currentDay,attempt);

            default: return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return (deviceMac + position + currentDay).hashCode();
    }
    @Override
    public boolean containsItem(long itemId) {
        return true;
    }

    @Override
    public int getItemCount() {
        return tabTitles.length;
    }
    //used to get the naming for the tabs
    public String getTabTitle(int position) {
        return tabTitles[position];
    }

}