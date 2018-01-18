package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
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
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.CareplanQuestionGroupActivity;
import com.healthnotifier.healthnotifier.activity.CareplanRecommendationActivity;
import com.healthnotifier.healthnotifier.adapter.CareplanQuestionAdapter;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/17/17.
 */

public class CareplanQuestionGroupFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mPatientId;
    private String mCareplanId;
    private String mCareplanQuestionGroupId;
    private JSONObject mResponseJson;
    private Boolean mLocked = false;

    private RecyclerView recyclerView;
    private CareplanQuestionAdapter mAdapter;// yup that's a tounge twisterz

    private HashMap<String, String> mAnswers = new HashMap<String, String>();

    // TODO: some notion of the state of answers? perhaps vs say, reading directly from the elements, w/e

    private Menu mMenu;

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
        if (intent.hasExtra("CAREPLAN_ID")) {
            mCareplanId = intent.getStringExtra("CAREPLAN_ID");
        }
        if (intent.hasExtra("CAREPLAN_QUESTION_GROUP_ID")) {
            mCareplanQuestionGroupId = intent.getStringExtra("CAREPLAN_QUESTION_GROUP_ID");
        }
    }

    // yea bro
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_careplan_question_group, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Advise Me");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // disable the SAVE buttton son
        // mMenu.findItem(R.id.action_submit).setEnabled(false);
        loadData();
    }

    private void loadData(){
        HealthNotifierAPI.getInstance().getCareplanQuestionGroup(mPatientId, mCareplanQuestionGroupId, new Callback() {
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
        //mMenu.findItem(R.id.action_submit).setEnabled(true); // lol bro, but really, only when all answers done been answered
        // build up dem adapter and all that shee show barnum n bailes
        try {
            mAdapter = new CareplanQuestionAdapter(getContext(), JSONHelper.jsonArrayToArrayList(mResponseJson.getJSONArray("questions")));
            recyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view); // blablablabla
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(mAdapter);
        } catch(Exception e){

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_careplan_question_group, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // return if not loaded yea
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: SOMETHING LEGIT SON, to back it up, if we choose to support this though, BIG IF THOUGH, that would umm require
                // a view stack and not singleTop for this particular view, which is easy enough, I suppose, lol brolo
                getActivity().finish();
                return true;
            case R.id.action_submit:
                onSubmit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSubmit(){
        // temp
        try {
            JSONArray items = mResponseJson.getJSONArray("questions");
            if(items.length() == mAnswers.keySet().size()){
                submit();
            } else {
                Snackbar.make(mRootView, "Please answer all questions", Snackbar.LENGTH_LONG).show();
            }
        } catch(Exception e){

        }
    }

    private void submit(){
        if(mLocked){
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("question_group_uuid", mCareplanQuestionGroupId);
            JSONArray answers = new JSONArray();
            for(String key :mAnswers.keySet()){
                JSONObject a = new JSONObject(); // long-hand bro
                a.put("question_uuid", key);
                a.put("choice_uuid", mAnswers.get(key));
                answers.put(a);
            }
            payload.put("answers", answers);


            mLocked = true;
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            mMenu.findItem(R.id.action_submit).setEnabled(false);
            final Snackbar snackProgress = Snackbar.make(mRootView, "Saving responses…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            HealthNotifierAPI.getInstance().createCareplanResponse(mPatientId, payload, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d("onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: global offline UX
                            mLocked = false;
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            mMenu.findItem(R.id.action_submit).setEnabled(true);
                            snackProgress.dismiss();
                            Snackbar.make(mRootView, "Network offline :( ", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        try {
                            JSONObject responseJSON = new JSONObject(response.body().string());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // zone-down the state bro, so if we resume here, it's chilltown
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    mMenu.findItem(R.id.action_submit).setEnabled(true);
                                    snackProgress.dismiss();
                                    try {
                                        if (responseJSON.getBoolean("complete")) {
                                            Intent intent = new Intent(getContext(), CareplanRecommendationActivity.class);
                                            intent.putExtra("PATIENT_ID", mPatientId);
                                            intent.putExtra("CAREPLAN_RECOMMENDATION_ID", responseJSON.getString("recommendation_uuid"));
                                            startActivity(intent);
                                        } else {
                                            Intent intent = new Intent(getContext(), CareplanQuestionGroupActivity.class);
                                            intent.putExtra("PATIENT_ID", mPatientId);
                                            intent.putExtra("CAREPLAN_ID", responseJSON.getString("care_plan_uuid"));
                                            intent.putExtra("CAREPLAN_QUESTION_GROUP_ID", responseJSON.getString("question_group_uuid"));
                                            startActivity(intent);
                                        }
                                    } catch(Exception e){

                                    }
                                }
                            });
                        } catch(Exception e) {
                            Logcat.d("it didn't work out " + e.toString());
                        }

                    } else {
                        try {
                            JSONObject responseJSON = new JSONObject(response.body().string());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    mMenu.findItem(R.id.action_submit).setEnabled(true);
                                    snackProgress.dismiss();
                                    String message = "Error saving responses";
                                    Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG).show();
                                }
                            });
                        } catch(Exception e){
                            // lol yea so much fail
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "Unknown Error. Please contact support@domain.com", Toast.LENGTH_SHORT).show();
                                    getActivity().finish();
                                }
                            });
                        }
                    }
                }
            });

        } catch(Exception e){

        }
        // network your nuts off
        // listen to the response json
        // go to next question group
        // or to recommendations yea son

        // test it out sonny buns


    }

    private void recordChoice(String questionId, String choiceId){
        // or something, dump into some notion of a Set so we can't worry about conflicts, just overwrite and go
        // so a MAP?
        Logcat.d("YOU MADE IT THIS FAR" + questionId + "-" + choiceId);
        mAnswers.put(questionId, choiceId);
    }

    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        if(event.eventName.equals("onChoiceChange") && event.attributes != null) {
            // JW the real implementation
            recordChoice((String) event.attributes.get("QuestionId"), (String) event.attributes.get("QuestionChoiceId"));
        }
    }

    // sub up son
    @Override
    public void onStart() {
        super.onStart();
        HealthNotifierApplication.bus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        HealthNotifierApplication.bus.unregister(this);
    }

}
