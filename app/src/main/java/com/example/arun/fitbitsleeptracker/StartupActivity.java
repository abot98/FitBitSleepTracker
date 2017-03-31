package com.example.arun.fitbitsleeptracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

public class StartupActivity extends AppCompatActivity {

    private Button loginBtn;
    private SharedPreferences preferences;
    private int mode = R.string.sumMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Before doing anything, check if this is the first time the user has logged in
        preferences = this.getSharedPreferences(getString(R.string.preferencesKey), Context.MODE_PRIVATE);

        boolean firstTime = preferences.getBoolean(getString(R.string.firstTimeKey), true);
        Log.i("Test", firstTime + "");
        if (!firstTime) {
            if(getIntent().getBooleanExtra("forceLogin", false)) {
                login(true); //Force a login
            }
            else {
                Intent intent = new Intent(this, SleepTrackerGUI.class);
                startActivity(intent);
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_startup);

        loginBtn = (Button) findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Save the mode
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(getString(R.string.trackingMode), mode);

                //Make it so this is no longer the first time
                editor.putBoolean(getString(R.string.firstTimeKey), false);

                editor.commit();
                login(false);
            }
        });

    }

    public void onRadioButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.sumModeBtn:
                mode = R.string.sumMode;
                break;
            case R.id.bucketModeBtn:
                mode = R.string.bucketMode;
                break;
        }

        ((RadioButton) view).setChecked(true); //Makes unchecking impossible
    }

    public void login(boolean forceVerification) {
        String url = "https://www.fitbit.com/oauth2/authorize?client_id=228C4D&response_type=token&scope=sleep heartrate&expires_in=31536000";

        if(forceVerification) {
            url += "&prompt=login";
        }

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }
}
