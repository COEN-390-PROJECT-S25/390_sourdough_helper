package com.example.mainactivity;

public class FermentationAnalyzerHelper {

    /**
     * Helper class which is used to interpret the sensors data
     */

    public static String analyzeTemperature(float temp) {
        /**
         * Function to give a status on sourdough based on temperature. The temperature will vary widely through the process.
         * Furthermore, there are many external factors that effect the the sourdough being complete. The most useful info
         * from the temperature is whether or not it is being fermented at best range to have best flavour.
         */
        String tempAdvice;
        if(temp < 23)
            tempAdvice = "Temperature low: Fermentation may be slow.";
        else if(temp > 30)
            tempAdvice = "Temperature high: risk of off-flavours of sourdough";
        else
            tempAdvice = "Temperature optimal for fermentation.";

        return tempAdvice;
    }

    public static String analyzeHumidity(float humidity) {
        String humidityAdvice;
        /**
         * Found this which talks about humidity affects on sourdough : https://sourdoughgeek.com/does-humidity-affect-sourdough/
         */
        if(humidity < 70)
            humidityAdvice = "Humidity is low, dough may dry out. Consider adding water.";
        else if(humidity <= 80)
            humidityAdvice = "Humidity is optimal for sourdough fermentation.";
        else if(humidity <= 90)
            humidityAdvice = "Humidity is high. Watch dough closely.";
        else
            humidityAdvice = "Very high humidity. Dough may over-ferment or dry out quickly. Consider reducing the hydration.";

        return humidityAdvice;
    }

    public static String analyzeToF(float currentToF, float initialToF) {
        /**
         * Function to look at change in volume to determine whether the starter is finished or not
         * gives a message based on the sourdough height (can be used to print messages based on daily trends in sourdough algorithm)
         */

        String ToFAdvice;
        Boolean TofBool = false;
        float jarHeight = 180;

        // Convert ToF distances to actual sourdough height in jar
        float currentHeight = jarHeight - currentToF;
        float initialHeight = jarHeight - initialToF;

        // Can check it with volume but the jar isn't uniform in diameter and changes slightly through the jar so might be less accurate
        // float jarDiameter = 80; // goes from about 75 to 85 so take average
        // float radius = jarDiameter / 2.0f;
        // double currentVolume = Math.PI * radius * radius * currentHeight;
        // double initialVolume = Math.PI * radius * radius * initialHeight;
        // double growthFactor = currentVolume / initialVolume;

        // Height-based growth factor
        float growthFactor = currentHeight / initialHeight;

        if (initialHeight <= 0 || currentHeight < 0) {
            ToFAdvice = "Invalid readings.";
            return ToFAdvice;
        }

        if (initialHeight + 15 > currentHeight) {
            ToFAdvice = "Starter hasn’t grown much. Consider checking the temperature or feeding starter.";
            TofBool = false;
        } else if (initialHeight + 15 < currentHeight) {
            ToFAdvice = "Starter is growing !";
            TofBool = true;
        } else {
            TofBool = true;
            ToFAdvice = "Your starter has doubled (or more) in size!";
        }

        return ToFAdvice;
    }


    public static String analyzeCO2(float co2) {
        String co2Stuff;
        /**
         *
         */
        if(co2 >= 400 && co2 < 1000)
            co2Stuff = "Ambient CO₂ levels—not significant fermentation activity.";
        else if(co2 <= 3000)
            co2Stuff = "Mild fermentation—starter is active.";
        else if(co2 <= 8000)
            co2Stuff = "Strong fermentation—well-fed and vigorous starter or levain.";
        else
            co2Stuff = "Very high buildup—possible jar pressure concerns (especially in sealed systems) and nearing full fermentation.";

        return co2Stuff;
    }

    public static boolean proceedToNextDay(float minCo2, float maxCo2, float initialToF, float currentToF, float avgTemp, float avgHumidity, int hoursElapsed) {

        // Don't know what to do for getting real time (hours elapsed) something with timestamps??

        float co2Growth = maxCo2 / minCo2;
        float jarHeight = 195;
        float heightGrowth = (jarHeight - currentToF) / (jarHeight - initialToF);

        if(hoursElapsed >= 16) {
            if(avgTemp < 24 || avgTemp > 28) {
                String tempMsg = analyzeTemperature(avgTemp);
                //send a notification using tempMsg with anthonys stuff
                //TODO
            }
            if(avgHumidity < 70 || avgHumidity > 85) {
                String humidityMsg = analyzeHumidity(avgHumidity);
                //send a notification using humidityMsg with anthonys stuff
                //TODO
            }
            if(co2Growth < 3.0) {
                String co2Msg = analyzeCO2(maxCo2);
                //send a notification using co2Msg with anthonys stuff
                //TODO
            }
            if(heightGrowth < 2.0) {
                String heightMsg = analyzeToF(currentToF, initialToF);
                //send a notification using heightMsg with anthonys stuff
                //TODO
            }
        }
        // Decide if user should proceed to next day (after everything else that can affect the starter has been checked)
        if(co2Growth >= 3.0f && heightGrowth >= 2.0f) {
            return true;
        }
        return false;
    }

    /*
    Each day record the min and max co2, humidity, temperature and height. after 16 hours each day you compare the max and the minimum
    and you put a section for each element.

    conditions to tell the user to go on the next feeding/day:
    (actually making dough)
    Check if there was a 3- 4 times increase in co2
    If dough doubles in height good sign your starter is going well

    if the user doesnt meet these in 24 hours (in 1 day) then there is an issue.
    You give trouble shooting tips, maybe tell them to restart, add water, flour, etc.

    Conditions to warn the user (notifications)
    IF the temperature is under 24 C then you need to tell them that your starter is
    not growing optimally.
    If the temperature is over 28, it is growing too fast and the quality is going to be bad
    If the humidity is under 70% (confirm), you tell the user that it is too dry, add water
    If the humidity is over 90% too wet. You messed up. You need to restart.
    The user will get these as notifications, so when something like this happens, we send a notification
    to tell them that something is wrong.

    ERROR HANDLING:
    -portrait mode issue. When you decide to rotate your phone, your app will rotate and mess up the entire UI.
    Make sure you can lock the entire app to just portrait mode to avoid this issue.
    Search up ways to make an app unbreakable.
    - Maybe you should try to break it using what people said.
    - For example, if you press many buttons at the same time, does it crash?
    - If you spam things really really fast, will it crash? How can we fix this?
    - Literally try it on your app, spam press things rally fast and it might just crash/
     */

}
