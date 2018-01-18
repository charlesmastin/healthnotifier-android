package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.adapter.CareplanRecommendationComponentsAdapter;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/17/17.
 */

public class CareplanRecommendationFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mPatientId;
    private String mRecommendationId;
    private JSONObject mResponseJson;
    private Menu mMenu;

    private RecyclerView recyclerView;
    private CareplanRecommendationComponentsAdapter mAdapter;// yup that's a tounge twisterz

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
        if (intent.hasExtra("CAREPLAN_RECOMMENDATION_ID")) {
            mRecommendationId = intent.getStringExtra("CAREPLAN_RECOMMENDATION_ID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_careplan_recommendation, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Recommendation");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // visually disable some dem things first though
        loadData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_careplan_recommendation, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            case R.id.action_done:
                // intent back ALL da way to the patient screen
                // gotta read up on de deets bro
                Intent intent = new Intent(getContext(), PatientActivity.class);
                intent.putExtra("PATIENT_ID", mPatientId);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadData(){
        HealthNotifierAPI.getInstance().getCareplanRecommendation(mPatientId, mRecommendationId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // etc etc
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        mResponseJson = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // etc etc
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                renderResults();
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
                            // etc etc etc
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void renderResults(){
        // yea son
        // response.getArray("components")
        // yea son
        // crank it out
        // title? maybe
        // description? maybe
        // cards per instruction set
        // w/ title
        // and text content, yea yea yea yea
        try {
            mAdapter = new CareplanRecommendationComponentsAdapter(getContext(), JSONHelper.jsonArrayToArrayList(mResponseJson.getJSONArray("components")));
            recyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view); // blablablabla
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(mLayoutManager);
            //recyclerView.addItemDecoration(new PatientsFragment.GridSpacingItemDecoration(2, dpToPx(5), true));
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(mAdapter);
        } catch(Exception e){

        }



    }
}
