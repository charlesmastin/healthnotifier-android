package com.healthnotifier.healthnotifier.activity;

import android.os.Bundle;
import com.healthnotifier.healthnotifier.R;

public class EditMedicalActivity extends EditCollectionsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        mActivityId = R.layout.activity_edit_medical;
        mFragmentId = R.id.fragment_edit_medical;
        super.onCreate(savedInstanceState);
    }

}
