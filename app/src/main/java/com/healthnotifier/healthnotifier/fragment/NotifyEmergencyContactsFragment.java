package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/7/17.
 */

public class NotifyEmergencyContactsFragment extends Fragment {

    // TODO: base patient scope fragment < LOLZONE SON
    private View mRootView;
    private Handler mHandler;
    private JSONObject mPatientJson;
    private String mPatientId;
    private Boolean mSending = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());
        setHasOptionsMenu(true); // meh, delay defer
        setRetainInstance(true);
        Intent ogIntent = getActivity().getIntent();
        Bundle ogBd = ogIntent.getExtras();
        mPatientId = ogBd.getString("PATIENT_ID");
        // TODO: the json or not
        // we only need it for showing a summary, list of the recipients, v2 son
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_notify_emergency_contacts, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Notify Contacts");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notify_emergency_contacts, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(!mSending) {
                    getActivity().finish();
                }
                return true;
            case R.id.action_submit:
                sendMessage();
                return true;
        }
        return true;
    }

    private void sendMessage(){
        if(mSending){
            return;
        }
        // latitute
        Double latitude = null;
        // longitude
        Double longitude = null;

        // ask for permission, if not using location though, lolzones, realizing this is totally not the time to do it, roger
        // we should do it during setup of your emergency contacts, and some notion of scanning preferences etc son
        // but for now, we'll just lol on it

        if(HealthNotifierApplication.getCurrentLocation() != null){
            latitude = HealthNotifierApplication.getCurrentLocation().getLatitude();
            longitude = HealthNotifierApplication.getCurrentLocation().getLongitude();
        }
        // quick ass validation
        String message = ((EditText) mRootView.findViewById(R.id.etNotifyMessage)).getText().toString();
        // more than blank, but less than say, some arbitary times
        if(message == null || message.equals("") || message.length() == 0){
            Toast.makeText(getActivity(), "Please add a message!", Toast.LENGTH_SHORT).show();
            return;
        }
        if(message.length() > 140){
            // meh, we should just let it go
            // in a real emergency, nobody needs this kind of crap
            // aka this isn't just for emergencies though, yo
            Toast.makeText(getActivity(), "Please shorten message some!", Toast.LENGTH_SHORT).show();
            return;
        }

        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        // meh disable that shit

        mSending = true;

        HealthNotifierAPI.getInstance().notifyEmergencyContacts(mPatientId, message, latitude, longitude, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                mSending = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        Toast.makeText(getActivity(), "Error notifying contacts :( ", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Emergency Contacts Notified!", Toast.LENGTH_SHORT).show();
                            getActivity().finish();
                        }
                    });
                } else {
                    Logcat.d( "onResponseError" + response.toString());
                    mSending = false;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            Toast.makeText(getActivity(), "Error notifying your contacts :( ", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}
