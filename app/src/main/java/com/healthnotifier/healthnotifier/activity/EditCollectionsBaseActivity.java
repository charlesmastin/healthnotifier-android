package com.healthnotifier.healthnotifier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.healthnotifier.healthnotifier.fragment.EditCollectionsBaseFragment;
import com.healthnotifier.healthnotifier.utility.Logcat;

/**
 * Created by charles on 2/8/17.
 */

public class EditCollectionsBaseActivity extends AppCompatActivity {
    protected int mActivityId;
    protected int mFragmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mActivityId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Logcat.d("EditCollectionsBaseActivity.onNewIntent");
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
            EditCollectionsBaseFragment fragment = (EditCollectionsBaseFragment) manager.findFragmentById(mFragmentId);
            // this is lame, but it is what it is
            fragment.queryCollection(
                    intent.getStringExtra("PATIENT_ID"),
                    intent.getStringExtra("COLLECTION_ID"),
                    intent.getStringExtra("COLLECTION_NAME"),
                    intent.getStringExtra("COLLECTION_ACTION")
            );
        } catch(Exception e){
            Logcat.d(e.toString());
        }
    }
}
