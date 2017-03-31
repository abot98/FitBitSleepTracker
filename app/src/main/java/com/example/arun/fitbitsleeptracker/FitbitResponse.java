package com.example.arun.fitbitsleeptracker;

import org.json.JSONObject;

/**
 * Created by Arun on 12/18/2016.
 */
public interface FitbitResponse {
    //For the response directly from the website
    void processFinished(JSONObject output);

    //For the response from the calculator
    void processFinished(int sleepSum, int daysToCheck);
}
