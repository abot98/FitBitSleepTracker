package com.example.arun.fitbitsleeptracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONObject;

public class HeartbeatBeta extends AppCompatActivity implements FitbitResponse {

    private String authToken, userId, tokenType;
    private FitbitHeartbeatRequest request;
    private Plotter plotter;
    private NumberPicker lowerLimitInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heartbeat_beta);

        lowerLimitInput = (NumberPicker) findViewById(R.id.lowerLimit);

        lowerLimitInput.setMinValue(50);
        lowerLimitInput.setMaxValue(180);
        lowerLimitInput.setValue(80);

        lowerLimitInput.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                if(plotter != null) plotter.update(newVal);
                Log.i("Output", "Input: " + newVal);
            }
        });

        //Pull in authorization data
        authToken = getIntent().getStringExtra("authToken");
        userId = getIntent().getStringExtra("userId");
        tokenType = getIntent().getStringExtra("tokenType");

        //Send request
        request = new FitbitHeartbeatRequest();
        request.delegate = this;
        request.execute(userId, authToken, tokenType, "2017-02-25");
        //System.out.print("Eat dat bussy like groceries");
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.frame);

        plotter = new Plotter(getApplicationContext());

        layout.addView(plotter);
    }

    @Override
    public void processFinished(JSONObject output) {
        if(output == null) return;
        try {
            Log.i("Output", output.get("activities-heart-intraday").toString());

            output = (JSONObject) output.get("activities-heart-intraday");
            JSONArray dataArray = output.getJSONArray("dataset");
            Log.i("Output", dataArray.length() + "");

            String[] timestamp = new String[dataArray.length()];
            int[] heartData = new int[dataArray.length()];

            for(int i = 0; i < dataArray.length(); i++) {
                timestamp[i] = ((JSONObject) dataArray.get(i)).getString("time");
                heartData[i] = ((JSONObject) dataArray.get(i)).getInt("value");
            }

            plotter.setData(heartData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processFinished(int sleepSum, int daysToCheck) {

    }
}
