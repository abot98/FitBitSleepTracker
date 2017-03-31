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

public class SleepTracker extends AppCompatActivity implements FitbitResponse {

    private String authToken, userId, tokenType, date; //Date in yyyy-MM-dd format
    private static String authTokenSearch = "access_token", userIdSearch = "user_id",
            tokenTypeSearch = "token_type";
    private boolean hasTokenInfo = false;
    private Button checkGoalBtn;
    private TextView goalOutput;
    private TimePicker goalInput;
    private Calendar calendar;
    private Toolbar toolbar;
    private ArrayList<String> datesToCheck;
    private static int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private FitbitSleepRequest request;
    private int daysToCheck, sleepGoalHours, sleepGoalMinutes;
    private int sleepSum;
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
        goalInput.setIs24HourView(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            goalInput.setHour(8);
            goalInput.setMinute(0);
        }
        else {
            goalInput.setCurrentHour(8);
            goalInput.setCurrentMinute(0);
        }

        calendar = Calendar.getInstance();

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
                   checkSleepGoal();
               }
           }
        });
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

    //Checks the sleep goal by determining the day of the week, and creating array
    //containing all of the days Fitbit needs to check.  Then begins starts the first data
    //request from Fitbit.  This action is completed by the processFinished method
    public void checkSleepGoal() {
        //The day we subtract is the night it resets.  i.e Sunday means Monday's count has 1 day
        daysToCheck = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        if(daysToCheck <= 0) daysToCheck = 7 + daysToCheck;


        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;  //January is 0 in the Calendar API
        int year = calendar.get(Calendar.YEAR);

        datesToCheck = new ArrayList<String>();
        String date = ""; //Should end up as yyyy-MM-dd format

        for(int i = 0; i < daysToCheck; i++) {
            //Add current date to arraylist
            date = year + "-";
            if (month < 10) date += "0";
            date += month + "-";
            if (day < 10) date += "0";
            date += day + "";
            datesToCheck.add(date);
            Log.i("Output", date);

            //Decrement date
            day--;
            if(day == 0) {
                month--;
                if(month == 0) {
                    month = 12;
                    year--;
                }
                day = daysInMonth[month - 1]; //Arrays start from 0
                if(month == 2 && year % 4 == 0) day = 29; //Accounting for leap years
            }
        }

        sleepSum = 0;

        request = new FitbitSleepRequest();
        request.delegate = this;
        request.execute(userId, authToken, tokenType, datesToCheck.remove(0));
    }

    //Completes the check sleep goal process by reading in the data Fitbit returns on the sleep
    //request, then if there are more days to check, sending another request.  If this is
    //the final request, calculate the sleep data and post it to the screen for the user.
    @Override
    public void processFinished(JSONObject output) {
        if(output == null) return;
        try {
            Log.i("Output", output.get("summary").toString());
            sleepSum += ((JSONObject) output.get("summary")).getInt("totalMinutesAsleep");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(datesToCheck == null || datesToCheck.size() == 0) {
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
            return;
        }

        request = new FitbitSleepRequest();
        request.delegate = this;
        if(datesToCheck.size() > 1)
            request.execute(userId, authToken, tokenType, datesToCheck.remove(0));
        else {
            request.execute(userId, authToken, tokenType, datesToCheck.get(0));
            datesToCheck = null;
        }
    }

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
}
