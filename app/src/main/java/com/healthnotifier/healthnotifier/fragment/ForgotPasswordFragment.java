package com.healthnotifier.healthnotifier.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.activity.UnlockActivity;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.LoginActivity;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Validators;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;

public class ForgotPasswordFragment extends Fragment {

	private Handler mHandler;
	private View mRootView;

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
		mRootView = inflater.inflate(R.layout.fragment_forgot_password, container, false);
		return mRootView;
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        Button loginButton = (Button) mRootView.findViewById(R.id.btToLogin);
        Button submitButton = (Button) mRootView.findViewById(R.id.btSubmit);

        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                segueToLogin();
            }
        });

        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmit();
            }
        });

        EditText etEmail = (EditText) mRootView.findViewById(R.id.etEmail);

        EditText etMobilePhone = (EditText) mRootView.findViewById(R.id.etMobilePhone);

        etMobilePhone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    onSubmit();
                }
                return handled;
            }
        });

        // check if there is incoming email son son
        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        Bundle bd = intent.getExtras();

        if(bd != null) {
            String tEmail = (String) bd.get("EMAIL");
            if(tEmail != null && tEmail.length() > 0){
                etEmail.setText(tEmail);
            }
        }
	}

    private void onSubmit(){
        // then kick that shit to submit if we validate correctly yea son
        if(mLocked) {
            return;
        }
        // basic though
        if (Validators.isValidEmail(((EditText) mRootView.findViewById(R.id.etEmail)).getText().toString())) {
            if(Validators.isValidPhone(((EditText) mRootView.findViewById(R.id.etMobilePhone)).getText().toString())){
                submit();
            } else {
                // prompt dem son? and let's just do an alert because that's one I know I can handle 2 actions on yea son
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("Missing Recovery Phone");
                alertDialog.setMessage("If you never added a phone to your account it’s ok. We’ll just send you an email instead of a SMS.");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Begin Recovery",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                submit();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            // and do we have a mobile, if not, just let us confirm son
        } else {
            Snackbar.make(mRootView, "Please enter your email", Snackbar.LENGTH_LONG).show();
            // focus dat field bro
        }

    }

	private void submit() {
		if(mLocked) {
			return;
		}

        try {

            mLocked = true;
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            mRootView.findViewById(R.id.btSubmit).setEnabled(false);
            final Snackbar snackProgress = Snackbar.make(mRootView, "Recovering Account…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            //
            String email = ((EditText) mRootView.findViewById(R.id.etEmail)).getText().toString();
            String phone = ((EditText) mRootView.findViewById(R.id.etMobilePhone)).getText().toString();

            // TODO: ok, we need to strip the phone down to integers only based on keyboard input options
            // yea that is important bro bra

            // consume new API
            HealthNotifierAPI.getInstance().beginAccountRecovery(email, phone, new Callback() {
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
                            JSONObject recoveryResponse = new JSONObject(response.body().string());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if(recoveryResponse.getString("channel").toUpperCase().equals("SMS")){

                                            // log analytics
                                            Bus bus = HealthNotifierApplication.bus;
                                            Map<String, Object> attributes = new HashMap<String, Object>();
                                            attributes.put("Email", email);
                                            try {
                                                attributes.put("Phone", "xxx-xxx-" + phone.substring(phone.length() - 4)); // maybe we shouldn't sent that to analytics too much PII??
                                            } catch(Exception e){
                                                // meh
                                                attributes.put("Phone", "xxx-xxx-xxxx");
                                            }
                                            bus.post(new AnalyticsEvent("Password Reminder", attributes));

                                            Intent intent = new Intent(getContext(), UnlockActivity.class);
                                            intent.putExtra("EMAIL", email);
                                            intent.putExtra("RECOVERY_MOBILE_PHONE", phone);
                                            startActivity(intent);

                                        } else {
                                            // email only bra
                                            Toast.makeText(getContext(), "A password reset email was sent to you from support@domain.com", Toast.LENGTH_LONG).show();
                                            String email = ((EditText) mRootView.findViewById(R.id.etEmail)).getText().toString();

                                            // log analytics
                                            Bus bus = HealthNotifierApplication.bus;
                                            Map<String, Object> attributes = new HashMap<String, Object>();
                                            attributes.put("Email", email);
                                            bus.post(new AnalyticsEvent("Password Reminder", attributes));

                                            segueToLogin();
                                        }
                                        //
                                        // DETERMINE were we mobile or email mode
                                        //
                                        // then seqeue appropriately
                                        /*

                                        */
                                    } catch(Exception e){
                                        Logcat.d("bombed out son" + e.toString());
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
                                String message = "Credentials incorrect";
                                // failures += 1; lol bro
                                // get granular
                                // look at the error codes based on http status
                                Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });

        } catch(Exception e){

        }

	}

	private void segueToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        startActivity(intent);
	}

}
