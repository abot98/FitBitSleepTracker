package com.example.arun.fitbitsleeptracker;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Arun on 3/31/2017.
 */
public class SleepTracker implements FitbitResponse {
    private String authToken, userId, tokenType;
    private ArrayList<String> datesToCheck;
    private FitbitSleepRequest request;
    private Calendar calendar;
    private static int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private int mode;
    private int daysToCheck, sleepGoalHours, sleepGoalMinutes;
    private int sleepSum;

    public FitbitResponse delegate;

    public SleepTracker(String authToken, String userId, String tokenType) {
        this.authToken = authToken;
        this.userId = userId;
        this.tokenType = tokenType;

        calendar = Calendar.getInstance();
    }

    //Checks the sleep goal by determining the day of the week, and creating array
    //containing all of the days Fitbit needs to check.  Then begins starts the first data
    //request from Fitbit.  This action is completed by the processFinished method
    public void checkSleepGoal(int sleepGoalHours, int sleepGoalMinutes) {
        this.sleepGoalHours = sleepGoalHours;
        this.sleepGoalMinutes = sleepGoalMinutes;

        //The day we subtract is the night it resets.  i.e Sunday means Monday's count has 1 day
        daysToCheck = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        if (daysToCheck <= 0) daysToCheck = 7 + daysToCheck;


        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;  //January is 0 in the Calendar API
        int year = calendar.get(Calendar.YEAR);

        datesToCheck = new ArrayList<String>();
        String date = ""; //Should end up as yyyy-MM-dd format

        for (int i = 0; i < daysToCheck; i++) {
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
            if (day == 0) {
                month--;
                if (month == 0) {
                    month = 12;
                    year--;
                }
                day = daysInMonth[month - 1]; //Arrays start from 0
                if (month == 2 && year % 4 == 0) day = 29; //Accounting for leap years
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
        if (output == null) return;
        try {
            Log.i("Output", output.get("summary").toString());
            sleepSum += ((JSONObject) output.get("summary")).getInt("totalMinutesAsleep");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (datesToCheck == null || datesToCheck.size() == 0) {
            delegate.processFinished(sleepSum, daysToCheck);
            return;
        }

        request = new FitbitSleepRequest();
        request.delegate = this;
        if (datesToCheck.size() > 1)
            request.execute(userId, authToken, tokenType, datesToCheck.remove(0));
        else {
            request.execute(userId, authToken, tokenType, datesToCheck.get(0));
            datesToCheck = null;
        }
    }

    @Override
    public void processFinished(int lostHours, int lostMinutes) {  }

}