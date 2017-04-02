package com.example.arun.fitbitsleeptracker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.util.Calendar;

public class ScreenUnlockListener extends Service implements FitbitResponse {

    public static Intent instance; //Keep tracks of whether the service is already running
    private BroadcastReceiver br;
    private SharedPreferences preferences;
    private String authToken, userId, tokenType;
    private Calendar calendar;
    private int sleepGoalHours, sleepGoalMinutes;
    private SleepTracker tracker;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(instance != null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        preferences = this.getSharedPreferences(getString(R.string.preferencesKey), Context.MODE_PRIVATE);

        authToken = preferences.getString(getString(R.string.authTokenKey), null);
        userId = preferences.getString(getString(R.string.userIdKey), null);
        tokenType = preferences.getString(getString(R.string.tokenTypeKey), null);

        sleepGoalHours = preferences.getInt(getString(R.string.sleepGoalHoursKey), 8);
        sleepGoalMinutes = preferences.getInt(getString(R.string.sleepGoalMinutesKey), 0);

        if(authToken != null && userId != null && tokenType != null) {
            tracker = new SleepTracker(authToken, userId, tokenType);
            tracker.delegate = this;
        }

        calendar = Calendar.getInstance();

        br = new UnlockScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        this.registerReceiver(br, filter);

        instance = intent;

        return START_REDELIVER_INTENT;
    }

    public void createNotification() {
        if(tracker == null) return;

        if(calendar.get(Calendar.HOUR_OF_DAY) >= 10 && calendar.get(Calendar.HOUR_OF_DAY) <= 22) {
            if(preferences.getInt(getString(R.string.lastNotifiedDay), 0) != calendar.get(Calendar.DAY_OF_WEEK)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(getString(R.string.lastNotifiedDay), calendar.get(Calendar.DAY_OF_WEEK));
                editor.commit();
                tracker.checkSleepGoal(sleepGoalHours, sleepGoalMinutes);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void processFinished(JSONObject output) {

    }

    @Override
    public void processFinished(int sleepSum, int daysToCheck) {
        Log.i("Test", "Reached");

        String goalOutputText;

        //Check against sleep goal
        if(sleepSum >= (sleepGoalHours * 60 + sleepGoalMinutes) * daysToCheck) goalOutputText = "You are achieving your goal!";
        else {
            int totalDeprivation = (sleepGoalHours * 60 + sleepGoalMinutes) * daysToCheck - sleepSum;
            int hourDeprivation = totalDeprivation / 60;
            int minuteDeprivation = totalDeprivation % 60;
            goalOutputText = "You are ";
            if(hourDeprivation > 0) goalOutputText += (hourDeprivation) + ((hourDeprivation > 1) ? " hours and " : " hour and ");
            if(minuteDeprivation != 0) goalOutputText += minuteDeprivation + ((minuteDeprivation > 1) ? " minutes " : " minute ");
            goalOutputText += "sleep deprived.";
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Sleep Tracker Results");
        mBuilder.setContentText(goalOutputText);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);

        Intent resultIntent = new Intent(this, SleepTrackerGUI.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyManager.notify(001, mBuilder.build());
    }
}
