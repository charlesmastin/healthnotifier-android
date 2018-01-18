package com.healthnotifier.healthnotifier.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.form.FieldValidation;
import com.healthnotifier.healthnotifier.form.FormValidation;
import com.healthnotifier.healthnotifier.form.Forms;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.healthnotifier.healthnotifier.utility.Validators;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 2/16/17.
 */

public class EditAccountFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private Boolean mLocked = false;
    private Menu mMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_edit_account, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Edit Account");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // populate the email and phone
        Preferences prefs = HealthNotifierApplication.preferences;
        ((EditText) mRootView.findViewById(R.id.etAccountEmail)).setText(prefs.getEmail());
        String phone = prefs.getMobilePhone();
        // hopefully this doesn't bomb son
        if(phone == null){
            // current note can be shown, since you really can't have a null phone when signing up from mobile, lol, ish™
        }else {
            // hide the explanation
            EditText etPhone = (EditText) mRootView.findViewById(R.id.etAccountPhone);
            mRootView.findViewById(R.id.tvAccountPhoneInfo).setVisibility(View.GONE);
            etPhone.setText(prefs.getMobilePhone());
            // mark the field as required bro, as we don't want them to unset it EVER
            etPhone.setHint("Recovery Mobile Phone *");
        }
        // TODO: saved billing cards management, lolbro

        // submit for password blablab
        EditText etNewPassword = (EditText) mRootView.findViewById(R.id.etAccountPasswordNew);
        etNewPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_account, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: hook the dirty check for unsaved changes son, like iOS
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
        // generate the payload here so we can save time and energy later :)
        Preferences prefs = HealthNotifierApplication.preferences;
        try {
            JSONObject payload = new JSONObject();
            FormValidation validator = new FormValidation();

            // validation hour
            // do we have a valid email
            String email = ((EditText) mRootView.findViewById(R.id.etAccountEmail)).getText().toString();
            String phone = ((EditText) mRootView.findViewById(R.id.etAccountPhone)).getText().toString();
            String pass1 = ((EditText) mRootView.findViewById(R.id.etAccountPassword)).getText().toString();
            String pass2 = ((EditText) mRootView.findViewById(R.id.etAccountPasswordNew)).getText().toString();
            // nesting vs complete valiation, meh
            if(Validators.isValidEmail(email)){
                payload.put("Email", email);
            } else {
                // fail
                FieldValidation fieldError = new FieldValidation();
                fieldError.message = "Please input a valid email";
                validator.errors.add(fieldError);
            }

            // phone is "NOT" required if we already have null for some reason or we can obtain the sign up origin, lol bro
            // we have to be careful not to nil out an existing phone bro
            if(prefs.getMobilePhone() != null || phone.length() > 0){
                // then it's required to be valid
                if(Validators.isValidPhone(phone)){
                    // only put that if not bro
                    payload.put("MobilePhone", phone);
                }else{
                    // fail
                    FieldValidation fieldError = new FieldValidation();
                    fieldError.message = "Please input a valid mobile phone";
                    validator.errors.add(fieldError);
                }
            }

            if(pass1.length() > 0){
                if(Validators.isValidPassword(pass1)){
                    if(Validators.isValidPassword(pass2)){
                        // hopefully by having "password" in the name they will be filtered from logs, lol and brolo
                        payload.put("CurrentPassword", pass1);
                        payload.put("NewPassword", pass2);
                    } else {
                        // fail
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.message = "Please input a valid new password";
                        validator.errors.add(fieldError);
                    }
                }else {
                    // fail
                    FieldValidation fieldError = new FieldValidation();
                    fieldError.message = "Your current password is invalid"; // sketchy messaging
                    validator.errors.add(fieldError);
                }
            }

            if(!validator.isValid()){
                Forms.defaultDisplayFormValidation(mRootView, getContext(), validator);
                return;
            }

            // did we have any keys in payload though, otherwise, ain't no time
            // lol bro bra
            // broken, but this is the right idea
            if(payload.keys().hasNext()){
                submit(payload);
            } else {
                // ok, because didn't go back and "delete it because let's say we had nothing but email" right, this logic is busted
                Snackbar.make(mRootView, "No changes to save", Snackbar.LENGTH_LONG).show();
            }
            // the else is we didn't attempt to change passwords son

            // is it a new email, confirm all connected devices will be logged out, etc
            // validate mobile phone is valid number
            // if we have anything in existing password, well, we have to assume we want to add a new pass
            // be sure both that and the new pass pass our sniff test, that's all we can do
            // we could set convenience stuffs here that way we know what to assemble in the payload zone, bro
            // also, we should be sure to abort the "nothing" changed scenario
        } catch(Exception e){

        }


    }

    private void submit(JSONObject payload){
        if(mLocked){
            return;
        }

        // consider we already… umm confirmed some progressive dialogs, but not in MVP
        mLocked = true;
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        mMenu.findItem(R.id.action_submit).setEnabled(false);
        final Snackbar snackProgress = Snackbar.make(mRootView, "Updating Account…", Snackbar.LENGTH_INDEFINITE);
        snackProgress.show();

        HealthNotifierAPI.getInstance().updateAccount(HealthNotifierApplication.preferences.getAccountId(), payload, new Callback() {
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
                        JSONObject recoveryResponse = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // TODO: analytics
                                // Toast it up son
                                Toast.makeText(getContext(), "Account Updated", Toast.LENGTH_SHORT).show();
                                // or get fancy with a special intent back to… bro, hard to do, hard to do
                                // well, we can listen in MainActivity and show some dem snack bars bro, it's totally doable bro bra brizzle
                                getActivity().finish();
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
                                String message = "Error updating account";
                                if (response.code() == 403) {
                                    // yea, you attempted to update someone else's account
                                    message = "Error: Not account owner";
                                }
                                if (response.code() == 400) {
                                    // Account requires a valid mobile phone, not a landline.
                                    try {
                                        String errorMessage = responseJSON.getString("message");
                                        if(errorMessage.toLowerCase().contains("mobile phone")){
                                            message = "Error: Mobile phone required";
                                        } else if (errorMessage.toLowerCase().contains("password")){
                                            // Invalid Password
                                            message = "Error: Unable to change password";
                                        } else {
                                            // general 400 with .errors, assume your desired new email is already in use
                                            message = "Error: Unable to change email"; // vaugue is the name of the game here
                                        }
                                    } catch (Exception e){

                                    }
                                }
                                // failures += 1; lol bro
                                // get granular
                                // look at the error codes based on http status
                                // TODO: error specifics though
                                Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG).show();
                            }
                        });
                    } catch(Exception e){
                        // lol yea so much fail
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Account update failed. Please contact support@domain.com", Toast.LENGTH_SHORT).show();
                                getActivity().finish();
                            }
                        });
                    }
                }
            }
        });

    }
}
