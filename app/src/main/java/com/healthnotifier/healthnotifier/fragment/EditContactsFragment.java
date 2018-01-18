package com.healthnotifier.healthnotifier.fragment;

import android.os.Bundle;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.model.CollectionMeta;
import com.healthnotifier.healthnotifier.utility.JSONHelper;

/**
 * Created by charles on 1/30/17.
 */

public class EditContactsFragment extends EditCollectionsBaseFragment {

    public EditContactsFragment(){
        this.mFragmentId = R.layout.fragment_edit_contacts;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // do our customizations sonny buns
        mTitle = "Insurance & Medical Contacts";

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "insurances";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("insurances"));
            collection.listId = R.id.lvInsurances;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "care_providers";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("care_providers"));
            collection.listId = R.id.lvPhysicians;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "hospitals";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("hospitals"));
            collection.listId = R.id.lvHospitals;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "pharmacies";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("pharmacies"));
            collection.listId = R.id.lvPharmacies;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }
    }


}
