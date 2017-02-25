package com.example.arun.fitbitsleeptracker;

import org.json.JSONObject;

/**
 * Created by Arun on 12/18/2016.
 */
public interface FitbitResponse {
    void processFinished(JSONObject output);
}
