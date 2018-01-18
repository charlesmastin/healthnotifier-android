package com.healthnotifier.healthnotifier.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.BuildConfig;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.activity.EditAccountActivity;
import com.squareup.otto.Bus;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;

import com.healthnotifier.healthnotifier.activity.ProviderCredentialsActivity;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

public class AccountFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private Menu mMenu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_edit_account:
                actionEditAccount();
                return true;
            case R.id.action_logout:
                promptLogout();
                return true;
            case R.id.action_delete_account:
                actionDeleteAccount();
                return true;
            /*
            case R.id.action_debug_unlock_account:
                intent = new Intent(getActivity(), UnlockActivity.class);
                intent.putExtra("RECOVERY_MOBILE_PHONE", "4152796521");
                startActivity(intent);
                return true;
            case R.id.action_debug_complete_recovery:
                intent = new Intent(getActivity(), CompleteRecoveryActivity.class);
                intent.putExtra("UNLOCK_TOKEN", "DONKEY420");
                startActivity(intent);
                return true;
            case R.id.action_debug_forgot_password:
                intent = new Intent(getActivity(), ForgotPasswordActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_debug_create_account:
                intent = new Intent(getActivity(), RegistrationActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_debug_login:
                intent = new Intent(getActivity(), LoginActivity.class);
                intent.putExtra("SKIP_AUTH_CHECK", "86");
                startActivity(intent);
                return true;
            */
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void promptLogout(){
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle("Really Logout?");
        // alertDialog.setMessage("This device and any other currently signed in devices (for your account) will be signed out");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Logout",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        actionLogout();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void actionLogout(){
        Bus bus = HealthNotifierApplication.bus;
        bus.post(new GenericEvent("Logout"));
    }

    private void actionRegister(){
        Intent intent = new Intent(getActivity(), ProviderCredentialsActivity.class);
        startActivity(intent);
    }

    private void actionDeleteAccount(){
        promptDeleteAccount();
    }

    private void deleteAccount(){
        // lock it down son, chop it down, then finish this here activity
        // toast your nuts silly
        final Activity context = getActivity();
        HealthNotifierAPI.getInstance().deleteAccount(HealthNotifierApplication.preferences.getAccountId(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                // mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Error deleting your account :( ", Toast.LENGTH_SHORT).show();
                        // context.finish();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Account deleted! We’re sad to see you go.", Toast.LENGTH_LONG).show();
                            // TODO: do the jimmy hack, aka reloadProfiles message, which just will grab the bizzle sizzle
                            // issue the logged out
                            Bus bus = HealthNotifierApplication.bus;
                            bus.post(new GenericEvent("Logout"));
                            context.finish();
                        }
                    });
                } else {
                    Logcat.d( "onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Error deleting your account :( ", Toast.LENGTH_SHORT).show();
                            // context.finish();
                        }
                    });
                }
            }
        });

    }

    private void promptDeleteAccount(){
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle("Really Delete Acount?");
        alertDialog.setMessage(getString(R.string.dialog_delete_account_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // call dat delete
                        deleteAccount();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_account, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        //mRootView.findViewById(R.id.btActionEdit).setVisibility(View.GONE);
        //mRootView.findViewById(R.id.btActionChangePassword).setVisibility(View.GONE);
        ((Button) mRootView.findViewById(R.id.btActionEdit)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditAccount();
            }
        });

        ((TextView) mRootView.findViewById(R.id.tvAccountEmail)).setText("Email: " + HealthNotifierApplication.preferences.getEmail());
        // String phone = HealthNotifierApplication.preferences.get();
        String phone = HealthNotifierApplication.preferences.getMobilePhone();
        if(phone == null) {
            ((TextView) mRootView.findViewById(R.id.tvAccountPhone)).setText("Recovery Mobile Phone: Not Set");
        } else {
            ((TextView) mRootView.findViewById(R.id.tvAccountPhone)).setText("Recovery Mobile Phone: " + phone);
        }

        Button btActionLogout = (Button) mRootView.findViewById(R.id.btActionLogout);
        btActionLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptLogout();
            }
        });

        Button btDeleteAccount = (Button) mRootView.findViewById(R.id.btActionDeleteAccount);
        btDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionDeleteAccount();
            }
        });

        Button terms = (Button) mRootView.findViewById(R.id.btActionLinkTerms);
        terms.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.domain.com/terms/"));
                startActivity(browserIntent);
            }
        });

        Button privacy = (Button) mRootView.findViewById(R.id.btActionLinkPrivacy);
        privacy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.domain.com/privacy/"));
                startActivity(browserIntent);
            }
        });

        Button help = (Button) mRootView.findViewById(R.id.btActionLinkHelp);
        help.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.domain.com/help/"));
                startActivity(browserIntent);
            }
        });

        Preferences pref = HealthNotifierApplication.preferences;
        String status = pref.getProviderCredentialStatus();
        // yea son



        if(HealthNotifierApplication.preferences.getProvider()){
            // hide dat button
            // and say it is so
            // mMenu.findItem(R.id.action_register_provider).setVisible(false);
            mRootView.findViewById(R.id.btActionRegister).setVisibility(View.GONE);
            mRootView.findViewById(R.id.tvAccountProviderStatus).setVisibility(View.VISIBLE);
        } else {
            // don't even bother with the nuance
            Button buttonRegister = (Button) mRootView.findViewById(R.id.btActionRegister);
            buttonRegister.setVisibility(View.VISIBLE);
            buttonRegister.setEnabled(true);// yea, the code is non-op now
            buttonRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionRegister();
                }
            });

            mRootView.findViewById(R.id.tvAccountProviderStatus).setVisibility(View.GONE);

            if (status != null && (status.equals("ACCEPTED") || status.equals("PENDING"))){
                mRootView.findViewById(R.id.btActionRegister).setVisibility(View.GONE);
                ((TextView)mRootView.findViewById(R.id.tvAccountProviderStatus)).setText("Status: " + status);
            } else {
                //mMenu.findItem(R.id.action_register_provider).setVisible(true);
            }

        }

        ((TextView) mRootView.findViewById(R.id.tvCopyright)).setText("© 2018 Charles Mastin. HealthNotifier for Android " + versionName.toString() + "(build: " + String.valueOf(versionCode) + ")");
    }

    private void setProviderState(){
        // balls son
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_account, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void actionEditAccount(){
        Intent intent = new Intent(getContext(), EditAccountActivity.class);
        startActivity(intent);
    }

}
