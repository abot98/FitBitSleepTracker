package com.example.arun.fitbitsleeptracker;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Arun on 2/3/2017.
 */
public class FitbitHeartbeatRequest extends AsyncTask<String, String, JSONObject> {

    public FitbitResponse delegate = null;

    @Override
    protected JSONObject doInBackground(String... params) {
        //Collect sleep data
        URL url = null;
        int responseCode = 0;
        HttpURLConnection conn = null;
        try {
            while (responseCode != 200) {
                url = new URL("https://api.fitbit.com/1/user/" + params[0] + "/activities/heart/date/" + params[3] + "/1d/1sec.json");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", params[2] + " " + params[1]);
                responseCode = conn.getResponseCode();
                Log.i("DebugMsg", "\nSending 'GET' request to URL: " + url);
                Log.i("DebugMsg", "Response Code: " + responseCode);
                Log.i("DebugMsg", "Response Message: " + conn.getResponseMessage());
            }
            //Read the message
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            return new JSONObject(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(JSONObject result) {
        delegate.processFinished(result);
    }
}

