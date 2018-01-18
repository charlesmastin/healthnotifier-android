package com.healthnotifier.healthnotifier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.fragment.CheckoutFragment;
import com.healthnotifier.healthnotifier.utility.Logcat;


public class CheckoutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
    }

    // getIntent son
    @Override
    protected void onNewIntent(Intent intent) {
        Logcat.d("CheckoutActivity.onNewIntent pass dat captured code down the pipe bro sef");
        super.onNewIntent(intent);
        setIntent(intent);

        Intent freshIntent = getIntent();
        FragmentManager manager = getSupportFragmentManager();
        CheckoutFragment fragment = (CheckoutFragment) manager.findFragmentById(R.id.fragment_checkout);

        if(freshIntent.hasExtra("CAPTURED_LIFESQUARE_CODE")) {
            fragment.handleCaptureResult(freshIntent.getStringExtra("CAPTURED_LIFESQUARE_CODE"));
        }
    }

}
