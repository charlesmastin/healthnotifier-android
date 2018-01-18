package com.healthnotifier.healthnotifier.activity;

import android.os.Bundle;
import com.healthnotifier.healthnotifier.R;

public class EditContactsActivity extends EditCollectionsBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        mActivityId = R.layout.activity_edit_contacts;
        mFragmentId = R.id.fragment_edit_contacts;
        super.onCreate(savedInstanceState);
    }

}
