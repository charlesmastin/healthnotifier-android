package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.activity.PatientNetworkSearchActivity;
import com.healthnotifier.healthnotifier.adapter.PatientNetworkConnectionsAdapter;
import com.healthnotifier.healthnotifier.adapter.PatientNetworkInvitesAdapter;
import com.healthnotifier.healthnotifier.adapter.PatientNetworkPendingAdapter;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/2/17.
 */

public class PatientNetworkManageFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mPatientRawJson;
    private String mPatientId;
    private JSONObject mPatientJson;
    private String mMode = "inbound"; // Or outbound, lolzone

    private Boolean mFreshJson = false; // quick hack to determine if we reloaded, and thus should pass back to the parent activity an intent to "re-render" UI

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(mMode.equals("inbound")){
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Shared With You");
        }
        if(mMode.equals("outbound")){
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("LifeCircle");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // naa son
        setRetainInstance(true);

        mHandler = new Handler(Looper.getMainLooper());

        mPatientRawJson = getActivity().getIntent().getStringExtra("PATIENT_JSON");
        mMode = getActivity().getIntent().getStringExtra("MODE"); // inbound || outbound

        // change the title son, perhaps we'll change this


        try {
            mPatientJson = new JSONObject(mPatientRawJson);
            mPatientId = mPatientJson.getJSONObject("profile").getString("uuid");
        } catch(Exception e){
            mPatientJson = null;
        }

        HealthNotifierApplication.bus.register(this);
    }

    public void loadData() {
        // THIS IS COMEDY TIMES

        HealthNotifierAPI.getInstance().getPatient(mPatientId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Error loading profile. No network connection to server.", Toast.LENGTH_SHORT).show();
                        // meh or is it our webservice vomiting 500's
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        mFreshJson = true;
                        mPatientJson = null;
                        // DOUBLE PUNCH IT??
                        mPatientJson = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                renderView();
                            }
                        });
                    } catch (Exception e) {
                        //
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Error loading profile.", Toast.LENGTH_SHORT).show();
                            // meh or is it our webservice vomiting 500's
                        }
                    });
                }
            }
        });
    }

    private void renderView(){
        try {
            JSONObject networkNode = mPatientJson.getJSONObject("network");
            if(mMode.equals("inbound")){
                // connections
                JSONArray connections = networkNode.getJSONArray("granters");
                if(connections.length() > 0){
                    ListView lv = (ListView) mRootView.findViewById(R.id.lvPatientNetworkConnections);
                    lv.setVisibility(View.VISIBLE);
                    PatientNetworkConnectionsAdapter adapter = new PatientNetworkConnectionsAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(connections),
                            mPatientId,
                            mMode
                    );
                    lv.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lv);
                    //((TextView) mRootView.findViewById(R.id.tvPatientNetworkConnections)).setVisibility(View.VISIBLE);
                    //((ListView) mRootView.findViewById(R.id.lvPatientNetworkConnections)).setVisibility(View.VISIBLE);

                } else {
                    // PROMOTE THIS FIRST ACTION SON
                    //((TextView) mRootView.findViewById(R.id.tvPatientNetworkConnections)).setVisibility(View.INVISIBLE);
                    //((ListView) mRootView.findViewById(R.id.lvPatientNetworkConnections)).setVisibility(View.INVISIBLE);
                }

                // pending requests we had (invites we requested)
                JSONArray pending = networkNode.getJSONArray("granters_pending");
                if(pending.length() > 0){

                    ListView lv = (ListView) mRootView.findViewById(R.id.lvPatientNetworkPendingRequests);
                    ((TextView) mRootView.findViewById(R.id.tvPatientNetworkPendingRequests)).setVisibility(View.VISIBLE);

                    lv.setVisibility(View.VISIBLE);
                    PatientNetworkPendingAdapter adapter = new PatientNetworkPendingAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(pending),
                            mPatientId,
                            mMode
                    );
                    lv.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lv);

                } else {
                    ((TextView) mRootView.findViewById(R.id.tvPatientNetworkPendingRequests)).setVisibility(View.INVISIBLE);
                    ((ListView) mRootView.findViewById(R.id.lvPatientNetworkPendingRequests)).setVisibility(View.INVISIBLE);
                }
                // hide invites - because others simply grant to us, or decline our request
                ((TextView) mRootView.findViewById(R.id.tvPatientNetworkInvites)).setVisibility(View.INVISIBLE);
                ((ListView) mRootView.findViewById(R.id.lvPatientNetworkInvites)).setVisibility(View.INVISIBLE);
            }
            if(mMode.equals("outbound")){
                // connections
                JSONArray connections = networkNode.getJSONArray("auditors");
                if(connections.length() > 0){
                    ListView lv = (ListView) mRootView.findViewById(R.id.lvPatientNetworkConnections);
                    lv.setVisibility(View.VISIBLE);
                    PatientNetworkConnectionsAdapter adapter = new PatientNetworkConnectionsAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(connections),
                            mPatientId,
                            mMode
                    );
                    lv.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lv);
                    //((TextView) mRootView.findViewById(R.id.tvPatientNetworkConnections)).setVisibility(View.VISIBLE);
                    //((ListView) mRootView.findViewById(R.id.lvPatientNetworkConnections)).setVisibility(View.VISIBLE);
                } else {
                    // PROMOTE THIS FIRST ACTION SON
                    //((TextView) mRootView.findViewById(R.id.tvPatientNetworkConnections)).setVisibility(View.INVISIBLE);
                    //((ListView) mRootView.findViewById(R.id.lvPatientNetworkConnections)).setVisibility(View.INVISIBLE);
                }

                // THESE ARE THE people that want to see our medical profile
                JSONArray invites = networkNode.getJSONArray("auditors_pending");
                if(invites.length() > 0){
                    ListView lv = (ListView) mRootView.findViewById(R.id.lvPatientNetworkInvites);
                    ((TextView) mRootView.findViewById(R.id.tvPatientNetworkInvites)).setVisibility(View.VISIBLE);

                    lv.setVisibility(View.VISIBLE);
                    PatientNetworkInvitesAdapter adapter = new PatientNetworkInvitesAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(invites),
                            mPatientId,
                            mMode
                    );
                    lv.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lv);
                } else {
                    ((TextView) mRootView.findViewById(R.id.tvPatientNetworkInvites)).setVisibility(View.INVISIBLE);
                    ((ListView) mRootView.findViewById(R.id.lvPatientNetworkInvites)).setVisibility(View.INVISIBLE);
                }

                // hide pending because we only share with others
                ((TextView) mRootView.findViewById(R.id.tvPatientNetworkPendingRequests)).setVisibility(View.INVISIBLE);
                ((ListView) mRootView.findViewById(R.id.lvPatientNetworkPendingRequests)).setVisibility(View.INVISIBLE);

            }
        } catch(Exception e){
            // one bing honking TRY, because I am lazy and because I disagree with the org.json API
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_patient_network_manage, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Manage Connections");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // DAMN YOU CATHAMES AND YOUR "LOGICAL" NAMING OF THINGS / SO CONFUSING!
        // INBOUND / we are AUDITOR - we can see "Shared with You" | API collection granters (those who share with us)
        // OUTBOUND / we are GRANTER - we are sharing "LifeCircle" | API collection auditors (those who see us)

        // right now, all we really want to do is jump ship over to search network, k
        // state on visibility of the jimmy wrappers
        renderView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_patient_network_manage, menu);
        // mMenu = menu; I guess this is so we can config it later???
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(mFreshJson) {
                    // ok for overhead purposes, we probably only want to be "dirty" if we hit our reload data cycle
                    // but whatever bro, come at me
                    Intent intent = new Intent(getActivity(), PatientActivity.class);
                    intent.putExtra("PATIENT_ID", mPatientId);
                    intent.putExtra("PATIENT_JSON", mPatientJson.toString());
                    intent.putExtra("MODE", mMode); // inbound || outbound son
                    startActivity(intent);
                } else {
                    // if we're dirty bro, intent that way right on back now son
                    getActivity().finish();
                }
                return true;
            // remember this is not search submit, this is launch the search activity
            case R.id.action_patient_network_search:
                Intent intent2 = new Intent(getActivity(), PatientNetworkSearchActivity.class);
                intent2.putExtra("PATIENT_JSON", mPatientJson.toString());
                intent2.putExtra("MODE", mMode); // inbound || outbound son
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Subscribe
    public void handleGenericEvent(GenericEvent event) {
        if (event.eventName.equals("FetchPatient")) {
            // check dem patient_uuid and see if it matches though
            if(event.attributes.get("PatientId").equals(mPatientId)) {
                loadData();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HealthNotifierApplication.bus.unregister(this);
    }

}
