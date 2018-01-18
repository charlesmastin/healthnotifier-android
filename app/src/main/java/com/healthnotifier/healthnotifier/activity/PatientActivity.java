package com.healthnotifier.healthnotifier.activity;


import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.fragment.PatientFragment;
import com.healthnotifier.healthnotifier.utility.Logcat;

import android.os.Bundle;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import android.content.Intent;

import org.json.JSONObject;


public class PatientActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logcat.d("PatientActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Logcat.d("PatientActivity.onNewIntent Resyncing Dat Patient Doh");
        super.onNewIntent(intent);
        setIntent(intent);

        Intent freshIntent = getIntent();
        FragmentManager manager = getSupportFragmentManager();
        PatientFragment fragment = (PatientFragment) manager.findFragmentById(R.id.fragment_patient);
        // TODO: Need to check the PatientID though, to be sure we didn't cross our streams though, you never knows EGON

        if(freshIntent.hasExtra("PATIENT_ID")) {
            if (freshIntent.hasExtra("PATIENT_JSON")) {
                try {
                    JSONObject json = new JSONObject(freshIntent.getStringExtra("PATIENT_JSON"));
                    fragment.syncPatientJson(freshIntent.getStringExtra("PATIENT_ID"), json);
                } catch (Exception e) {
                    fragment.syncPatientJson(freshIntent.getStringExtra("PATIENT_ID"), null);
                }
            } else {
                fragment.syncPatientJson(freshIntent.getStringExtra("PATIENT_ID"), null);
            }
        } else {
            // huh what naa son, no patient id is no bueno, just do nothing and hope for the worst
        }
    }



}
