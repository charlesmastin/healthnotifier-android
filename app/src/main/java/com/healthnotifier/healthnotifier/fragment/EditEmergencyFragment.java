package com.healthnotifier.healthnotifier.fragment;

import android.os.Bundle;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.model.CollectionMeta;
import com.healthnotifier.healthnotifier.utility.JSONHelper;

/**
 * Created by charles on 1/30/17.
 */

public class EditEmergencyFragment extends EditCollectionsBaseFragment {

    public EditEmergencyFragment(){
        this.mFragmentId = R.layout.fragment_edit_emergency;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // do our customizations sonny buns
        mTitle = "Emergency Contacts";

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "emergency";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("emergency"));
            collection.listId = R.id.lvEmergency;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

    }


}
