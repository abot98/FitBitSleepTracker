package com.example.arun.fitbitsleeptracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import org.json.JSONObject;

import java.io.Console;
import java.util.ArrayList;
import java.util.Calendar;

public class SleepTrackerGUI extends AppCompatActivity implements FitbitResponse {

    private String authToken, userId, tokenType, date; //Date in yyyy-MM-dd format
    private static String authTokenSearch = "access_token", userIdSearch = "user_id",
            tokenTypeSearch = "token_type";
    private boolean hasTokenInfo = false;
    private Button checkGoalBtn;
    private TextView goalOutput;
    private TimePicker goalInput;
    private Toolbar toolbar;
    private SleepTracker tracker;
    private int sleepGoalHours, sleepGoalMinutes;
    private SharedPreferences preferences;

    @Override
    //Creates the sleep tracking display, checks login, and gets data returned from login sit
    //Adds on click listener to checkGoalBtn
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Before doing anything, check if this is the first time the user has logged in
        preferences = this.getSharedPreferences(getString(R.string.preferencesKey), Context.MODE_PRIVATE);

        checkGoalBtn = (Button) findViewById(R.id.checkGoalBtn);
        goalOutput = (TextView) findViewById(R.id.goalOutput);
        goalInput = (TimePicker) findViewById(R.id.sleepGoalInput);

        //Set up toolbar
        toolbar = (Toolbar) findViewById(R.id.topToolbar);
        toolbar.setTitle("Sleep Tracking");
        setSupportActionBar(toolbar);

        //Try to get tokens
        authToken = preferences.getString(getString(R.string.authTokenKey), null);
        userId = preferences.getString(getString(R.string.userIdKey), null);
        tokenType = preferences.getString(getString(R.string.tokenTypeKey), null);

        if(authToken == null || userId == null || tokenType == null) {
            Uri data = this.getIntent().getData();
            if (data != null && data.isHierarchical()) {
                String uri = this.getIntent().getDataString();
                Log.i("DebugMsg", "222 Deep link clicked " + uri);
                hasTokenInfo = collectData(uri);
            }
        }
        else hasTokenInfo = true;

        if(!hasTokenInfo) {
            Intent intent = new Intent(this, StartupActivity.class);
            intent.putExtra("forceLogin", true);
            startActivity(intent);
            finish();
            return;
        }

        tracker = new SleepTracker(authToken, userId, tokenType);

        //Set up sleep goal
        sleepGoalHours = preferences.getInt(getString(R.string.sleepGoalHoursKey), 8);
        sleepGoalMinutes = preferences.getInt(getString(R.string.sleepGoalMinutesKey), 0);

        //Register listener
        goalInput.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            goalInput.setHour(sleepGoalHours);
            goalInput.setMinute(sleepGoalMinutes);
        }
        else {
            goalInput.setCurrentHour(sleepGoalHours);
            goalInput.setCurrentMinute(sleepGoalMinutes);
        }

        //Set up alarm
        setupAlarm();

        goalInput.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

            public void onTimeChanged(TimePicker view, int hour, int minute) {
                sleepGoalHours = hour;
                sleepGoalMinutes = minute;

                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(getString(R.string.sleepGoalHoursKey), sleepGoalHours);
                editor.putInt(getString(R.string.sleepGoalMinutesKey), sleepGoalMinutes);
                editor.commit();
            }

        });

        checkGoalBtn.setOnClickListener(new View.OnClickListener() {
           public void onClick(View v) {
               if(hasTokenInfo) {
                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                       sleepGoalHours = goalInput.getHour();
                       sleepGoalMinutes = goalInput.getMinute();
                   }
                   else {
                       sleepGoalHours = goalInput.getCurrentHour();
                       sleepGoalMinutes = goalInput.getCurrentMinute();
                   }
                   goalOutput.setText("Checking sleep results.");
                   checkGoalBtn.setEnabled(false);
                   getSleepData();
               }
           }
        });
    }

    public void setupAlarm() {
        if(ScreenUnlockListener.instance == null) {
            Intent listener = new Intent(this, ScreenUnlockListener.class);
            startService(listener);
            Log.i("SERVICE", "STARTED");
        }
    }

    public void getSleepData() {
        tracker.delegate = this;
        tracker.checkSleepGoal(sleepGoalHours, sleepGoalMinutes);
    }

    //Extracts data from the URI used to call this activity
    public boolean collectData(String uri) {
        int authTokenIndex = uri.indexOf(authTokenSearch);
        int userIdIndex = uri.indexOf(userIdSearch);
        int tokenTypeIndex = uri.indexOf(tokenTypeSearch);
        int scopeIndex = uri.indexOf("scope");
        int expiresInIndex = uri.indexOf("expires_in");
        if(authTokenIndex == -1 || userIdIndex == -1 || tokenTypeIndex == -1 || scopeIndex == -1
                || expiresInIndex == -1) return false;
        authToken = uri.substring(authTokenIndex + authTokenSearch.length() + 1, userIdIndex - 1);
        userId = uri.substring(userIdIndex + userIdSearch.length() + 1, scopeIndex - 1);
        tokenType = uri.substring(tokenTypeIndex + tokenTypeSearch.length() + 1, expiresInIndex - 1);
        Log.i("DebugMsg", "Token: " + authToken);
        Log.i("Output", "User ID: " + userId);
        Log.i("DebugMsg", "Token Type: " + tokenType);

        //Save the tokens to preferences
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.authTokenKey), authToken);
        editor.putString(getString(R.string.userIdKey), userId);
        editor.putString(getString(R.string.tokenTypeKey), tokenType);
        editor.commit();

        return true;
    }

    //Completes the check sleep goal process by reading in the data Fitbit returns on the sleep
    //request, then if there are more days to check, sending another request.  If this is
    //the final request, calculate the sleep data and post it to the screen for the user.
    @Override
    public void processFinished(int sleepSum, int daysToCheck) {
        int hours = (int) (sleepSum + 0.5) / 60;
        int minutes = (int) (sleepSum + 0.5) % 60;
        String goalOutputText = "You've slept a total of " +
                hours + " hours and " + minutes + " minutes in the last " + ((daysToCheck > 1) ? daysToCheck + " days.\n" : "day.\n");

        //Check against sleep goal
        if(sleepSum >= (sleepGoalHours * 60 + sleepGoalMinutes) * daysToCheck) goalOutputText += "You are achieving your goal!";
        else {
            int totalDeprivation = (sleepGoalHours * 60 + sleepGoalMinutes) * daysToCheck - sleepSum;
            int hourDeprivation = totalDeprivation / 60;
            int minuteDeprivation = totalDeprivation % 60;
            goalOutputText += "As of today, you are ";
            if(hourDeprivation > 0) goalOutputText += (hourDeprivation) + ((hourDeprivation > 1) ? " hours and " : " hour and ");
            if(minuteDeprivation != 0) goalOutputText += minuteDeprivation + ((minuteDeprivation > 1) ? " minutes " : " minute ");
            goalOutputText += "sleep deprived.";
        }

        goalOutput.setText(goalOutputText);
        checkGoalBtn.setEnabled(true);
    }

    @Override
    public void processFinished(JSONObject output) {}

    @Override
    //Prevents the user returning to the login verification by hitting the back button.  This
    //changes the back button operation to leave this activity.
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    //Creates toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar, menu);
        return true;
    }

    //Handles the toolbar clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_logout:
                //Start a startup activity which forces the user to login again
                intent = new Intent(this, StartupActivity.class);
                intent.putExtra("forceLogin", true);
                startActivity(intent);
                finish();
                return true;
            case R.id.action_beta:
                // User chose the "Settings" item, show the app settings UI...
                intent = new Intent(this, HeartbeatBeta.class);
                intent.putExtra("authToken", authToken);
                intent.putExtra("userId", userId);
                intent.putExtra("tokenType", tokenType);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences.Editor editor = preferences.edit();

        boolean received = preferences.getBoolean(getString(R.string.received), false);
        Log.d("Test", received + "");

        editor.putBoolean(getString(R.string.received), false);
        editor.commit();

    }
}
