package com.healthnotifier.healthnotifier.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.adapter.PatientNetworkSearchAdapter;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by charles on 2/2/17.
 */

public class PatientNetworkSearchFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mPatientRawJson;
    private JSONObject mPatientJson;
    private String mPatientId;
    //
    private String mMode = "inbound"; // Or outbound, lolzone
    // the colletion adaptors son
    // for the network connections
    // for the invites
    private ArrayList<JSONObject> mResults = new ArrayList<JSONObject>();
    private PatientNetworkSearchAdapter mAdapter;

    // TODO: next rev use the material serach in the title bar son and the X for the back lol with the sweet animations and such

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());
        setHasOptionsMenu(true); // naa son
        setRetainInstance(true);

        mMode = getActivity().getIntent().getStringExtra("MODE"); // inbound || outbound
        mPatientRawJson = getActivity().getIntent().getStringExtra("PATIENT_JSON");
        try {
            mPatientJson = new JSONObject(mPatientRawJson);
            mPatientId = mPatientJson.getJSONObject("profile").getString("uuid");
        } catch(Exception e){
            mPatientJson = null;
            // we failed miserably so
            getActivity().finish();
        }

        // TODO: change the title son, perhaps we'll change this in the activity son
        if(mMode.equals("inbound")){
            // getActivity().getSupportActionBar().setTitle((String) bd.get("PATIENT_NAME"));
        }
        if(mMode.equals("outbound")){

        }

        try {
            mPatientJson = new JSONObject(mPatientRawJson);
        } catch(Exception e){
            mPatientJson = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_patient_network_search, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Search Network");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // zero that shizzle out son
        mAdapter = new PatientNetworkSearchAdapter(getContext(), mResults, mPatientId, mMode);
        ListView listView = (ListView) mRootView.findViewById(R.id.lvSearch);
        listView.setAdapter(mAdapter);
        // handle dat on submit from da search son
        EditText etSearch = (EditText) mRootView.findViewById(R.id.etSearch);
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideKeyboard();
                    searchNetwork(v.getText().toString());
                }
                return handled;
            }
        });
    }

    private void updateItems(){
        ArrayList<JSONObject> filteredResults = new ArrayList<JSONObject>();
        for(int i=0;i<mResults.size(); i++){
            try {
                JSONObject item = mResults.get(i);
                if (mMode.equals("outbound")) {
                    // remove existing connections
                    // TODO: pending status?
                    if(!item.getBoolean("IsAuditor")){
                        filteredResults.add(item);
                    }
                }
                if (mMode.equals("inbound")){
                    // remove existing connections
                    // TODO: pending status?
                    if(!item.getBoolean("IsGranter")){
                        filteredResults.add(item);
                    }
                }
            } catch (Exception e){
                // straight zipper nuts
            }
        }

        mAdapter.replaceItems(filteredResults);
        mAdapter.notifyDataSetChanged();
        ListView listView = (ListView) mRootView.findViewById(R.id.lvSearch);
        LayoutHelper.setListViewHeightBasedOnChildren(listView);
    }

    private void searchNetwork(String keywords){
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        HealthNotifierAPI.getInstance().patientNetworkSearch(mPatientId, keywords, "", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    // SO MANY DONKEY DICKS
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        mResults = JSONHelper.jsonArrayToArrayList(json.getJSONArray("Patients"));
                    } catch (Exception e) {

                    }

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // DONKEY DOUBLE DICKS
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            updateItems();
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // DONKEY DOUBLE DICKS
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    // TODO: put this in da static forms library son
    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // remember this is not search submit, this is launch the search activity
            case android.R.id.home:
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
