package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.adapter.CareplansAdapter;
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
 * Created by charles on 2/17/17.
 */

public class CareplansFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mPatientId;

    private ArrayList<JSONObject> mResults = new ArrayList<JSONObject>();
    private CareplansAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
        Intent intent = getActivity().getIntent();
        if (intent.hasExtra("PATIENT_ID")) {
            mPatientId = intent.getStringExtra("PATIENT_ID");
        }
    }

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.fragment_careplans, container, false);
            LayoutHelper.initActionBar(getActivity(), mRootView, "Advise Me");
            return mRootView;
        }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: suck it up list adapter
        // query the data bro bra
        mAdapter = new CareplansAdapter(getContext(), mResults, mPatientId);
        ListView listView = (ListView) mRootView.findViewById(R.id.lvCareplans);
        listView.setAdapter(mAdapter);
        // hide dat view contianer, set loading status
        loadData();
    }

    private void updateItems(){
        ArrayList<JSONObject> filteredResults = new ArrayList<JSONObject>();
        for(int i=0;i<mResults.size(); i++){
            try {
                JSONObject item = mResults.get(i);
                if(item.getString("status").equals("active")){
                    filteredResults.add(item);
                }
            } catch (Exception e){
                // straight zipper nuts
            }
        }
        mAdapter.replaceItems(filteredResults);
        mAdapter.notifyDataSetChanged();
        ListView listView = (ListView) mRootView.findViewById(R.id.lvCareplans);
        LayoutHelper.setListViewHeightBasedOnChildren(listView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadData(){
        // love that we need a list adapter for this, so much tedium already though
        HealthNotifierAPI.getInstance().getCareplans(mPatientId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        JSONArray items = new JSONArray(response.body().string());
                        mResults = JSONHelper.jsonArrayToArrayList(items);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // etc view changing bro brizz
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                updateItems();
                            }
                        });
                    } catch (Exception e) {
                        //
                    }
                } else {
                    Logcat.d( "onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            //swipeContainer.setRefreshing(false);
                        }
                    });
                }
            }
        });

    }

    private void doubleSecretInit(){

    }


}
