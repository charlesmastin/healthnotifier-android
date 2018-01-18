package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.adapter.CollectionAdapter;
import com.healthnotifier.healthnotifier.model.CollectionMeta;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 1/30/17.
 */

public class EditCollectionsBaseFragment extends Fragment {
    protected View mRootView;
    protected Handler mHandler; // only needed for continuing onboarding from inside this bizzle
    protected String mPatientRawJson;
    protected JSONObject mPatientJson;
    protected String mPatientId;
    protected int mFragmentId = 0;
    protected ArrayList<CollectionMeta> mCollections;
    protected String mTitle;

    private String mUpdateAction;
    // handle all the common transactions bits
    // progression for onboarding?
    // not sure if we need this class honestly as the collection items are saved individually from the edit screens
    // but this would handle some "future" version of top level deleting, like long-press and action menu or something
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HealthNotifierApplication.bus.register(this);
        // mHandler = new Handler(Looper.getMainLooper());
        setHasOptionsMenu(true); // naa son
        setRetainInstance(true);

        mHandler = new Handler(Looper.getMainLooper());

        mPatientRawJson = getActivity().getIntent().getStringExtra("PATIENT_JSON");
        try {
            mPatientJson = new JSONObject(mPatientRawJson);
            mPatientId = mPatientJson.getJSONObject("profile").getString("uuid");
        } catch (Exception e) {
            mPatientJson = null;
        }
        mCollections = new ArrayList<CollectionMeta>();
        // this.bindCollections();
        // called in child class son, but w/e FML
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: intercept and check for DIRTY just like iOS since we're aborting, this is all we need
                // to work approximately like iOS we need to have 100% form UI coverage for onChange

                Intent intent;
                intent = new Intent(getContext(), PatientActivity.class);
                intent.putExtra("PATIENT_JSON", mPatientJson.toString());
                intent.putExtra("PATIENT_ID", mPatientId);
                startActivity(intent);

                // getActivity().finish(); // lazy town USA
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(mFragmentId, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, mTitle);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        for(CollectionMeta collection: mCollections) {
            ListView listView = (ListView) mRootView.findViewById(collection.listId);

            CollectionAdapter adaptor = new CollectionAdapter(this.getContext(), collection.collectionJson, collection.collectionId, mPatientId);
            listView.setAdapter(adaptor);
            // timeout your shit
            //LayoutHelper.setListViewHeightBasedOnChildren(listView);
            LayoutHelper.setListViewHeightBasedOnItems(listView);
            // and come back later and reset the height??? huh wut?
        }

    }
    // DEAD F SIMPLE, or else we will loose at life
    // generic event for "updateCollection" for now, which is simply the name of the collection so we can replace it in the scope and re-render accordingly
    // any operation will cause this, no relevance for type of operation
    /*
    @Subscribe
    public void handleGenericEvent(GenericEvent event) {
        Logcat.d("EditColletionBaseFragment.handleGenericEvents " + event.eventName + ":" + event.attributes.toString());
        // this is extremely ready to be retired, once we're able to get the proper fragment and instantiate it all from the forms helper
        if(event.eventName.equals("CollectionUpdate") && event.attributes != null) {
            Logcat.d("CollectionUpdate:" + event.attributes.toString());
            // in the future (and to save bandwidth), we could pass the collection item instance, but that's SO much extra work now
            queryCollection((String) event.attributes.get("PatientId"), (String) event.attributes.get("CollectionId"), (String) event.attributes.get("CollectionName"));
            // Snackbar.make(mRootView.findViewById(android.R.id.content), "CollectionUpdate", Snackbar.LENGTH_SHORT).show();
            //
        }
    }
    */

    // quick ghetto slap
    public void queryCollection(String patientId, String collectionId, String collectionName, String action){
        // Logcat.d("queryCollection: " + collectionId + '-' + collectionName);
        // rewrite our collections requests, OMG
        if(collectionId.equals("conditions")){
            collectionName = "conditions";
        }
        if(collectionId.equals("procedures")){
            collectionName = "procedures";
        }
        if(collectionId.equals("immunizations")){
            collectionName = "immunizations";
        }
        final String mfCollectionName = collectionName;
        mUpdateAction = action;
        if(patientId.equals(mPatientId)){
            for(CollectionMeta collection: mCollections) {
                if(collectionId.equals(collection.collectionId)){
                    // so apparently it's a little involved at the moment to obtain the collectionName from the colletion meta, lol bones
                    // yea son, matvh it up son
                    HealthNotifierAPI.getInstance().getCollection(mPatientId, mfCollectionName, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "Error loading collection. No network connection to server.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, okhttp3.Response response) throws IOException {

                            if (response.isSuccessful()) { // exactly what status codes determine this?
                                try {
                                    JSONArray collectionJSON = new JSONArray(response.body().string());
                                    // TODO: parse the response items here? or there?, meh
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateCollectionContainer(collectionId, mfCollectionName, collectionJSON);
                                        }
                                    });
                                } catch (Exception e) {
                                    //
                                }
                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "Error loading collection.", Toast.LENGTH_SHORT).show();
                                        // meh or is it our webservice vomiting 500's
                                    }
                                });
                            }
                        }
                    });

                    // hehehehe

                    break;
                }
            }
        } else {
            Logcat.d("queryCollection: wrong patient son");
        }
        // iterate the collections to find the meta object, in case we need extra deets
        // query that shit async
    }

    // pass in our VERB as well, so we can well form the message
    protected void updateCollectionContainer(String collectionId, String collectionName, JSONArray collectionJSON){
        // Logcat.d("updateCollectionContainer" + collectionId + "-" + collectionName);
        // Logcat.d("updateCollectionContainer" + collectionJSON.toString());
        // given some basic info, jimmy slip this into the container, adapter, and re-render UI
        // CoordinatorLayout cl = (CoordinatorLayout) mRootView.findViewById(R.id.cl1);
        String message = "Update Success";
        if(mUpdateAction.equals("create")){
            message = "Item Added!";
        }
        if(mUpdateAction.equals("update")){

        }
        if(mUpdateAction.equals("delete")){
            message = "Item Removed";
        }
        Snackbar.make(mRootView, message, Snackbar.LENGTH_SHORT).show();

        for(CollectionMeta collection: mCollections) {
            if(collection.collectionId.equals(collectionId)){
                ListView listView = (ListView) mRootView.findViewById(collection.listId);
                CollectionAdapter adapter = (CollectionAdapter) listView.getAdapter();
                adapter.replaceItems(JSONHelper.jsonArrayToArrayList(collectionJSON));
                adapter.notifyDataSetChanged();

                // INVALIDATE THE MF VIEW RECYCLER CACHE FOR THE LOVE OF …
                listView.setAdapter(adapter);

                try {
                    mPatientJson.put(collectionId, collectionJSON);
                } catch(Exception e){
                    Logcat.d("SYNCING JSON FAILED INTERNALLY");
                }

                LayoutHelper.setListViewHeightBasedOnItems(listView);

                break;
            }
        }
    }

    // we have to be careful in terms of when / how this will get there, like did we stop or did we pause

    @Override
    public void onStart() {
        //Logcat.d("EditCollectionBaseFragment.onStart getting on the bus");
        super.onStart();

    }

    @Override
    public void onStop() {
        //Logcat.d("EditCollectionBaseFragment.onStop");
        super.onStop();
    }

    @Override
    public void onDetach(){
        //Logcat.d("EditColectionBaseFragment.onDetach jumping off the bus");
        //HealthNotifierApplication.bus.unregister(this);
        super.onDetach();
    }

}
