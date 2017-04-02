package com.example.arun.fitbitsleeptracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UnlockScreenReceiver extends BroadcastReceiver {
    public UnlockScreenReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context instanceof ScreenUnlockListener) {
            ((ScreenUnlockListener) context).createNotification();
        }
    }


}
