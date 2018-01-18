package com.healthnotifier.healthnotifier.fragment;

import android.os.Bundle;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.model.CollectionMeta;
import com.healthnotifier.healthnotifier.utility.JSONHelper;

/**
 * Created by charles on 1/30/17.
 */

public class EditMedicalFragment extends EditCollectionsBaseFragment {

    public EditMedicalFragment(){
        this.mFragmentId = R.layout.fragment_edit_medical;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // do our customizations sonny buns
        mTitle = "Medical Details";

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "directives";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("directives"));
            collection.listId = R.id.lvDirectives;
            // collection.adaptor = "image"
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "medications";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("medications"));
            collection.listId = R.id.lvMedications;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "allergies";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("allergies"));
            collection.listId = R.id.lvAllergies;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "conditions";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("conditions"));
            collection.listId = R.id.lvConditions;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "procedures";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("procedures"));
            collection.listId = R.id.lvProcedures;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "immunizations";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("immunizations"));
            collection.listId = R.id.lvImmunizations;
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "documents";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("documents"));
            collection.listId = R.id.lvDocuments;
            // image adaptor son
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

    }


}
