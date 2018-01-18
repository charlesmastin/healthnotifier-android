package com.healthnotifier.healthnotifier.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.fragment.ScanFragment;

/**
 * Created by charles on 2/12/17.
 */

public class CaptureLifesquareActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_lifesquare);

        // EXCEPTION as this is wrapping a fragment that is typically embedded in the Main Activity
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        if(Build.VERSION.SDK_INT >= 21) {
            myToolbar.setElevation(Float.valueOf("0.0"));
        }
        getSupportActionBar().setTitle("Scan LifeSticker");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // fragment manger, lol bro, of course we could also use an intent for "SCAN" hella overloaded here or "CAPTURE" but that would be too logical
        FragmentManager manager = getSupportFragmentManager();
        ScanFragment fragment = (ScanFragment) manager.findFragmentById(R.id.fragment_capture_lifesquare);
        fragment.mScanOnCapture = false;
    }

    // listen bro
    // and then send intents back to da checkout son, yea son, lol bro

}
