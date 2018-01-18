package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.CompleteRecoveryActivity;
import com.healthnotifier.healthnotifier.activity.ForgotPasswordActivity;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/15/17.
 */

public class UnlockFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;

    private Boolean mLocked = false;
    private String mPhone;
    private String mEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
        // capture da phone now
        mPhone = getActivity().getIntent().getStringExtra("RECOVERY_MOBILE_PHONE");
        mEmail = getActivity().getIntent().getStringExtra("EMAIL");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_unlock, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button submitButton = (Button) mRootView.findViewById(R.id.btSubmit);
        submitButton.setEnabled(false);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });

        // change handler on the unlock code bra
        EditText etUnlockCode = (EditText) mRootView.findViewById(R.id.etUnlockCode);
        etUnlockCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Button submitButton = (Button) mRootView.findViewById(R.id.btSubmit);
                if(s.toString().length() == 6){
                    submitButton.setEnabled(true);
                }else {
                    submitButton.setEnabled(false);
                }
            }
        });

        etUnlockCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if(etUnlockCode.getText().toString().length() == 6) {
                        submit();
                    }
                }
                return handled;
            }
        });

    }

    private void submit(){
        // this bad boy is already validated son
        if(mLocked){
            return;
        }
        mLocked = true;
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.btSubmit).setEnabled(false);
        final Snackbar snackProgress = Snackbar.make(mRootView, "Transmitting codes…", Snackbar.LENGTH_INDEFINITE); // sounds like some SIGINT fun times hackzors
        // RUSSIA TODAY TIMES™
        snackProgress.show();

        String unlockCode = ((EditText) mRootView.findViewById(R.id.etUnlockCode)).getText().toString();
        HealthNotifierAPI.getInstance().unlockAccount(unlockCode, mPhone, new Callback() {
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
                        // parse dat response bro, because we need da token son
                        JSONObject responseJson = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // do something
                                try {
                                    Intent intent = new Intent(getActivity(), CompleteRecoveryActivity.class);
                                    intent.putExtra("UNLOCK_TOKEN", responseJson.getString("token"));
                                    intent.putExtra("EMAIL", mEmail);
                                    startActivity(intent);
                                } catch(Exception e) {
                                    //Native JSON slop never gets old, oh, well it does actually
                                }
                            }
                        });
                    } catch(Exception e) {
                        Logcat.d("it didn't work out " + e.toString());
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
                            String message = "Invalid Unlock Code!";
                            // failures += 1; lol bro
                            // get granular
                            // look at the error codes based on http status
                            Snackbar sizzle = Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG);
                            sizzle.setAction("Request Another", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(getActivity(), ForgotPasswordActivity.class);
                                    startActivity(intent);
                                }
                            });
                            sizzle.show();
                        }
                    });
                }
            }
        });

    }

}
