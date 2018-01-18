package com.healthnotifier.healthnotifier.fragment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

//import com.auth0.jwt.JWT;
//import com.auth0.jwt.JWTVerifier;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.auth0.jwt.exceptions.JWTVerificationException;
//import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.android.jwt.JWT;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.fcm.PushController;
import com.healthnotifier.healthnotifier.model.Lifesquare;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.activity.ForgotPasswordActivity;
import com.healthnotifier.healthnotifier.activity.MainActivity;
import com.healthnotifier.healthnotifier.activity.RegistrationActivity;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.Validators;

import com.squareup.otto.Bus;

import org.json.JSONObject;

//import io.jsonwebtoken.Jwt;
//import io.jsonwebtoken.JwtParser;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
import okhttp3.Call;
import okhttp3.Callback;

public class LoginFragment extends Fragment {
    
    private View mRootView;
    private Handler mHandler;

    private int failures = 0;
    private Date timerStart;

    private EditText etEmail;
    private EditText etPassword;

    // HACKTOWN USA™
    private Boolean mLocked = false;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_login, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logcat.d("LoginFragment.onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        // use the extra data hack to skip auth check son
        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        Bundle bd = intent.getExtras();

        etEmail = (EditText) mRootView.findViewById(R.id.etEmail);
        etPassword = (EditText) mRootView.findViewById(R.id.etPassword);

        Boolean autologin = false;

        if(bd != null) {
            String tEmail = (String) bd.get("EMAIL");
            if (tEmail != null && tEmail.length() > 0) {
                etEmail.setText(tEmail);
            }
            String tPassword = (String) bd.get("PASSWORD");
            if (tPassword != null && tPassword.length() > 0) {
                etPassword.setText(tPassword);
                // autologin = true;
            }
        }

        if(bd != null && (String) bd.get("SKIP_AUTH_CHECK") != null){
            // LOLZORS.COM


        } else {
            checkAuthToken();
        }
        timerStart = new Date();
        Button btLogin = (Button) mRootView.findViewById(R.id.btLogin);
        btLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        Button btForgotPassword = (Button) mRootView.findViewById(R.id.btForgotPassword);
        btForgotPassword.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                forgotPassword();
            }
        });

        Button btCreateAccount = (Button) mRootView.findViewById(R.id.btCreateAccount);
        btCreateAccount.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });


        etPassword.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    login();
                }
                return handled;
            }
        });
        if(autologin){
            Logcat.d("ABOUT TO ATTEMPT DAT AUTO LOGIN");
            login();
        }
    }
    private void checkAuthToken() {
        Logcat.d("checkAuthToken");
        Preferences pref = HealthNotifierApplication.preferences;
        // for historical reasons bro
        if(pref.getAuthToken() != Preferences.NULL_STRING){
            // wrap this
            Logcat.d("Existing Token: " + pref.getAuthToken());

            try {
                JWT jwt = new JWT(pref.getAuthToken());
                if (jwt.isExpired(60 * 10)) { // 10 minutes drift / leeway
                    Logcat.d("JWT EXPIRED?");
                    return;
                }
            } catch (Exception e){
                // legacy token as well
                Logcat.d(e.getMessage());
                return;
            }

            // TODO: check dat expiration son
            // because it very well may already be toast
            // wrap this in JW, so our JWT lookup doesn't barf on existing legacy tokens on the device
            // don't really need to do this because the class does it itself
            // mid-launch-lifecycle attempt at scoring the push token
            HealthNotifierApplication.persistDeviceToken();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);

            Boolean shouldFetch = true;
            // lolzin, if we were launched with the bonus notification scope,
            // denote this here beefcakes
            // TODO: experiment with putting this post re-auth re-launch though, just in case
            if(getActivity().getIntent().hasExtra("event")) {
                // for the most part we don't get access to the original notification
                // need a jimmy hack fcm observer that is subject to change
                // this should do
                Logcat.d("Remote Notification Inbound Bro, forward on dat Intent");

                // we're careful not to bind too tightly to any schema, since it's unfolded from data
                PushController controller = new PushController();
                controller.intent = getActivity().getIntent();
                controller.handleTransitioning(getContext());

                // exclude fetch based on matching event
                // in da future, also da data-only push events
                if(getActivity().getIntent().getStringExtra("event").equals("provider-status")){
                    shouldFetch = false;
                }
            }
            // mmkay, fetch dat user update on resuming app for sync state on User
            // this may be tooooo frequent
            if(shouldFetch) {
                Bus bus = HealthNotifierApplication.bus;
                bus.post(new GenericEvent("FetchUser"));
            }

            getActivity().finish();
        } else {
            Logcat.d("Auth token was null");
        }
    }

    // this is a ghetto hack for auto signon, because unfortunately our networking is tightly coupled to this class, and I don't have time to duplicate or decouple
    public void autoLogin(String email, String password) {
        etEmail.setText(email);
        etPassword.setText(password);
        login();
    }

    private void createAccount() {
        Intent intent = new Intent(getActivity(), RegistrationActivity.class);
        intent.putExtra("EMAIL", etEmail.getText().toString());
        startActivity(intent);
    }

    private void forgotPassword() {
        Intent intent = new Intent(getActivity(), ForgotPasswordActivity.class);
        intent.putExtra("EMAIL", etEmail.getText().toString());
        startActivity(intent);
    }

    private void login() {
        if(mLocked) {
            return;
        }
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (Validators.isValidEmail(email)) {
            mLocked = true;
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            mRootView.findViewById(R.id.btLogin).setEnabled(false);
            final Snackbar snackProgress = Snackbar.make(mRootView, "Logging in…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            // consume new API
            HealthNotifierAPI.getInstance().getAccessToken(email, password, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // send back a failure event
                            mLocked = false;
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            mRootView.findViewById(R.id.btLogin).setEnabled(true);
                            snackProgress.dismiss();
                            Snackbar.make(mRootView, "Network offline :( ", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        try {
                            JSONObject loginResponse = new JSONObject(response.body().string());

                            //Jwt parsed = Jwts.parser().parse(token)
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        // decode that shizzle ma nizzle
                                        String token = loginResponse.getString("access_token");
                                        JWT jwt = new JWT(token);
                                        String account_uuid = jwt.getClaim("lifesquare_account_uuid").asString();
                                        Boolean account_provider = jwt.getClaim("lifesquare_provider").asBoolean();
                                        HealthNotifierApplication.preferences.setAuthToken(token);
                                        HealthNotifierApplication.preferences.setAccountId(account_uuid);
                                        HealthNotifierApplication.preferences.setProvider(account_provider);

                                        // INTO memory
                                        HealthNotifierAPI.getInstance().setAccessToken(token);

                                        Bus bus = HealthNotifierApplication.bus;
                                        bus.post(new GenericEvent("FetchUser"));


                                        // log analytics
                                        Map<String, Object> attributes = new HashMap<String, Object>();
                                        attributes.put("AccountId", account_uuid);
                                        attributes.put("Provider", account_provider);
                                        attributes.put("Failures", failures);

                                        Date timerEnd = new Date();
                                        long duration = timerEnd.getTime() - timerStart.getTime();
                                        attributes.put("IdleDuration", TimeUnit.MILLISECONDS.toSeconds(duration));

                                        bus.post(new AnalyticsEvent("Login", attributes));



                                        // persistUser(loginResponse);
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        startActivity(intent);

                                    } catch(Exception e){
                                        // JSON access is such a drag son
                                        Logcat.d(e.getMessage());
                                    }
                                }
                            });
                        } catch(Exception e) {
                        }

                    } else {
                        Logcat.d("onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // send back
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                mRootView.findViewById(R.id.btLogin).setEnabled(true);
                                snackProgress.dismiss();
                                String message = "Login Incorrect!";
                                failures += 1;
                                // get granular
                                // look at the error codes based on http status

                                // TODO: offer a button to Forgot Password
                                Snackbar lilSnack = Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG);
                                lilSnack.setAction("Recover Password", new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        lilSnack.dismiss();
                                        forgotPassword();
                                    }
                                });
                                lilSnack.show();
                            }
                        });
                    }
                }
            });
        }else{
            Snackbar.make(mRootView, "Please input a valid email address", Snackbar.LENGTH_LONG).show();
            etEmail.requestFocus();
        }
    }
}
