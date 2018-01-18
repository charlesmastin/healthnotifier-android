package com.healthnotifier.healthnotifier.activity;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.fcm.PushController;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
	}
}
