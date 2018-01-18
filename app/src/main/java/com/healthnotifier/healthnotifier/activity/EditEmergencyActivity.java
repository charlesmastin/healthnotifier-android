package com.healthnotifier.healthnotifier.activity;

import android.os.Bundle;
import com.healthnotifier.healthnotifier.R;

public class EditEmergencyActivity extends EditCollectionsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        mActivityId = R.layout.activity_edit_emergency;
        mFragmentId = R.id.fragment_edit_emergency;
        super.onCreate(savedInstanceState);
    }

}
