package com.healthnotifier.healthnotifier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.fragment.PatientNetworkManageFragment;
import com.healthnotifier.healthnotifier.utility.Logcat;


public class PatientNetworkManageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_network_manage);
    }
    //intent Jimmy hacks
    @Override
    protected void onNewIntent(Intent intent) {
        Logcat.d("PatientActivity.onNewIntent Resyncing Dat Patient Doh");
        super.onNewIntent(intent);
        setIntent(intent);
        // we don't care about what was in the intent, just that we know we need to reload and refresh the view
        // probably an easier way of donig this
        Intent freshIntent = getIntent();
        FragmentManager manager = getSupportFragmentManager();
        PatientNetworkManageFragment fragment = (PatientNetworkManageFragment) manager.findFragmentById(R.id.fragment_patient_network_manage);
        fragment.loadData();
    }

}
