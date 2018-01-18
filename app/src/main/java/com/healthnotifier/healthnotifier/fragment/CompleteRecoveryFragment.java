package com.healthnotifier.healthnotifier.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Validators;
import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/15/17.
 */

public class CompleteRecoveryFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mToken;
    private Boolean mLocked = false;


    // getting desperate here
    private String mEmail;
    private JSONObject mRecoveryResponseJSON = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
        mToken = getActivity().getIntent().getStringExtra("UNLOCK_TOKEN");
        mEmail = getActivity().getIntent().getStringExtra("EMAIL");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_complete_recovery, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button submitButton = (Button) mRootView.findViewById(R.id.btSubmit);
        submitButton.setEnabled(false);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit();
            }
        });
        // watchers on stuffs, lol bro
        EditText pass2 = (EditText) mRootView.findViewById(R.id.etPasswordConfirm);
        pass2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    onSubmit();
                }
                return handled;
            }
        });
    }

    private void onSubmit(){
        if(mLocked){
            return;
        }
        String pass1 = ((EditText) mRootView.findViewById(R.id.etPassword)).getText().toString();
        String pass2 = ((EditText) mRootView.findViewById(R.id.etPasswordConfirm)).getText().toString();
        if(Validators.isValidPassword(pass1)){
            if(pass1.equals(pass2)){
                submit();
            } else {
                Snackbar.make(mRootView, "Passwords to not match", Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(mRootView, "Password does not meet requirements", Snackbar.LENGTH_LONG).show();
        }
    }

    private void submit(){
        if(mLocked){
            return;
        }
        mLocked = true;
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.btSubmit).setEnabled(false);
        final Snackbar snackProgress = Snackbar.make(mRootView, "Updating password…", Snackbar.LENGTH_INDEFINITE); // sounds like some SIGINT fun times hackzors
        // RUSSIA TODAY TIMES™
        snackProgress.show();

        String password = ((EditText) mRootView.findViewById(R.id.etPassword)).getText().toString();
        HealthNotifierAPI.getInstance().completeAccountRecovery(password, mToken, new Callback() {
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
                        mRootView.findViewById(R.id.btSubmit).setEnabled(true);
                        snackProgress.dismiss();
                        Snackbar.make(mRootView, "Network offline :( ", Snackbar.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        // currently crashing 100% of the time here
                        // https://github.com/square/okhttp/issues/1240
                        // http://stackoverflow.com/questions/38641565/okhttp-throwing-an-illegal-state-exception-when-i-try-to-log-the-network-respons
                        // mRecoveryResponseJSON = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // put it on the bus son, probably could do this from the other thread though, lol YOLO SON
                                Bus bus = HealthNotifierApplication.bus;
                                Map<String, Object> attributes = new HashMap<String, Object>();
                                attributes.put("Email", mEmail);
                                attributes.put("Password", password);
                                bus.post(new GenericEvent("AutoLogin", attributes));
                            }
                        });
                    } catch(Exception e){
                        Logcat.d("FAILED" + e.toString());
                    }
                } else {
                    Logcat.d("onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mLocked = false;
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            mRootView.findViewById(R.id.btSubmit).setEnabled(true);
                            snackProgress.dismiss();
                            String message = "Error Updating Password";
                            // failures += 1; lol bro
                            // get granular
                            // look at the error codes based on http status
                            Snackbar sizzle = Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG);
                            sizzle.show();
                        }
                    });
                }
            }
        });
    }
}
