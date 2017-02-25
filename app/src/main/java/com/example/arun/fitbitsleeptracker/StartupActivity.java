package com.example.arun.fitbitsleeptracker;

import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class StartupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        String url = "https://www.fitbit.com/oauth2/authorize?client_id=227YJR&response_type=token&scope=sleep heartrate&expires_in=31536000";

        //If this intent was started with forceLogin=true, tell Fitbit to make the user login
        boolean forceLogin = getIntent().getBooleanExtra("forceLogin", false);
        if(forceLogin) url += "&prompt=login";

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));

        finish();
    }
}
