package com.example.arun.fitbitsleeptracker;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class UnlockListenerStarter extends BroadcastReceiver {

    private AlarmManager manager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(ScreenUnlockListener.instance == null) {
            Intent listener = new Intent(context, ScreenUnlockListener.class);
            context.startService(listener);
        }
    }
}
