package com.healthnotifier.healthnotifier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.fragment.EditProfileFragment;
import com.healthnotifier.healthnotifier.utility.Logcat;


public class EditProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logcat.d("EditProfileActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
    }

    // hurts that this isn't DRY / with da blablabla whatever
    // makes me thing EditProfile IS A EditCollections vs HAS A, but w/e for a nother day
    @Override
    protected void onNewIntent(Intent intent) {
        Logcat.d("EditProfileActivity.onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);
        // TODO: check if we have this stuffs
        // ONLY when there is an Event of CollectionUpdate
        // in case we do other things in the future son
        handleUpdateIntent();
    }

    protected void handleUpdateIntent(){
        // do nothing here, not sure how to mark this in java
        // hmm attempt to cast as a superclass for some sleazy polymorphism
        try {
            Intent intent = getIntent();
            FragmentManager manager = getSupportFragmentManager();
            EditProfileFragment fragment = (EditProfileFragment) manager.findFragmentById(R.id.fragment_edit_profile);

            // switch on the event bro
            String eventName = intent.getStringExtra("EVENT");

            if(eventName.equals("CollectionUpdate")){
                // this is lame, but it is what it is
                fragment.queryCollection(
                        intent.getStringExtra("PATIENT_ID"),
                        intent.getStringExtra("COLLECTION_ID"),
                        intent.getStringExtra("COLLECTION_NAME"),
                        intent.getStringExtra("COLLECTION_ACTION")
                );
            }

            if(eventName.equals("LicenseScan")){
                fragment.handleLicenseScan(intent.getStringExtra("CONTENT"));
            }

        } catch(Exception e){
            Logcat.d(e.toString());
        }
    }

}
