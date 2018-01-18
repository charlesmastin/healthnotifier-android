package com.healthnotifier.healthnotifier.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.CareplansActivity;
import com.healthnotifier.healthnotifier.activity.CheckoutActivity;
import com.healthnotifier.healthnotifier.activity.EditCollectionItemActivity;
import com.healthnotifier.healthnotifier.activity.EditContactsActivity;
import com.healthnotifier.healthnotifier.activity.EditEmergencyActivity;
import com.healthnotifier.healthnotifier.activity.EditMedicalActivity;
import com.healthnotifier.healthnotifier.activity.EditProfileActivity;
import com.healthnotifier.healthnotifier.activity.LifesquareActivity;
import com.healthnotifier.healthnotifier.activity.NotifyEmergencyContactsActivity;
import com.healthnotifier.healthnotifier.activity.PatientNetworkManageActivity;
import com.healthnotifier.healthnotifier.adapter.AccessLogAdapter;
import com.healthnotifier.healthnotifier.adapter.CollectionAdapter;
import com.healthnotifier.healthnotifier.adapter.PatientNetworkConnectionsAdapter;
import com.healthnotifier.healthnotifier.adapter.PatientNetworkInvitesAdapter;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;

public class PatientFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;
    private JSONObject mPatientJson;
    // quick references to shiz ma niz
    private String mPatientId;
    private Boolean mHasLifesquare = false;
    private Boolean mHasCoverage = false;
    private Boolean mConfirmed = false;
    private Menu mMenu;

    // TODO: swap this to the Glide loader maybe? wut?
    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    // PUBLIC API FACING THE WRAPPING ACTIVITY
    public void syncPatientJson(String patientId, @Nullable JSONObject json){
        if(patientId.equals(mPatientId)) {
            if (json == null) {
                Logcat.d("syncPatient: reloading");
                loadData(true);
            } else {
                Logcat.d("syncPatient: merging");
                mPatientJson = json;
                doubleSecretInit();
            }
        } else {
            Logcat.d("syncPatient: patientId mismatch");
            mPatientId = patientId;
            loadData(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logcat.d("PatientFragment.onCreate");
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());
        setHasOptionsMenu(true); // meh, delay defer
        setRetainInstance(true);
        // TODO: handle the donkey town when this doesn't exist, etc
        Intent ogIntent = getActivity().getIntent();
        Bundle ogBd = ogIntent.getExtras();
        mPatientId = ogBd.getString("PATIENT_ID");

        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();

        HealthNotifierApplication.bus.register(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_patient, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // cock block
        if(mPatientJson == null){
            return false;
        }
        switch (item.getItemId()) {
            case R.id.action_continue_setup:
                actionContinueSetup();
                return true;
            case R.id.action_view_lifesquare:
                actionViewLifesquare();
                return true;
            case R.id.action_edit_profile:
                actionEditProfile();
                return true;
            case R.id.action_edit_medical:
                actionEditMedical();
                return true;
            case R.id.action_edit_contacts:
                actionEditContacts();
                return true;
            case R.id.action_edit_emergency:
                actionEditEmergency();
                return true;
            case R.id.action_notify_emergency:
                actionNotifyEmergency();
                return true;
            case R.id.action_checkout_assign:
                actionCheckout("assign");
                return true;
            case R.id.action_checkout_renew:
                actionCheckout("renew");
                return true;
            case R.id.action_checkout_replace:
                actionCheckout("replace");
                return true;
            case R.id.action_edit_network_inbound:
                actionEditPatientNetwork("inbound");
                return true;
            case R.id.action_edit_network_outbound:
                actionEditPatientNetwork("outbound");
                return true;
            case R.id.action_advise_me:
                actionCareplans();
                return true;
            case R.id.action_delete_profile:
                actionDeleteProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logcat.d("PatientFragment.onCreateView");
        mRootView = inflater.inflate(R.layout.fragment_patient, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout ll = (LinearLayout) mRootView.findViewById(R.id.llJW);
        ll.setVisibility(View.INVISIBLE);
        // so everything in the main BIZZLE NEEDS TO BE HIDDEN, until we have the data, yea son? fair enough
        // ALL THE BUTTONS SON
        // because I care
        // ok click on the entire honking thing
        Button btContinueSetup = (Button) mRootView.findViewById(R.id.btPatientContinueSetup);
        btContinueSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionContinueSetup();
            }
        });
        CardView cardContinueSetup = (CardView) mRootView.findViewById(R.id.cardOnboarding);
        cardContinueSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionContinueSetup();
            }
        });
        // click the entire thing
        CardView cardLifesquare = (CardView) mRootView.findViewById(R.id.cardLifesquare);
        cardLifesquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionViewLifesquare();
            }
        });
        Button btViewLifesquare = (Button) mRootView.findViewById(R.id.btPatientLifesquare);
        btViewLifesquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionViewLifesquare();
            }
        });
        ImageView ivPhoto = (ImageView) mRootView.findViewById(R.id.ivPatientPhoto);
        ivPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionViewLifesquare();
            }
        });
        ImageView ivLifesquare = (ImageView) mRootView.findViewById(R.id.ivLifesquareQRCode);
        ivLifesquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionViewLifesquare();
            }
        });
        Button btEditProfile = (Button) mRootView.findViewById(R.id.btPatientEditProfile);
        btEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditProfile();
            }
        });
        Button btEditMedical = (Button) mRootView.findViewById(R.id.btPatientEditMedical);
        btEditMedical.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditMedical();
            }
        });
        Button btEditContacts = (Button) mRootView.findViewById(R.id.btPatientEditContacts);
        btEditContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditContacts();
            }
        });
        Button btEditEmergency = (Button) mRootView.findViewById(R.id.btPatientEditEmergency);
        btEditEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditEmergency();
            }
        });
        Button btNotifyEmergency = (Button) mRootView.findViewById(R.id.btPatientNotifyEmergency);
        btNotifyEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionNotifyEmergency();
            }
        });
        Button btNetworkInbound = (Button) mRootView.findViewById(R.id.btPatientNetworkInbound);
        btNetworkInbound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditPatientNetwork("inbound");
            }
        });
        Button btNetworkOutbound = (Button) mRootView.findViewById(R.id.btPatientNetworkOutbound);
        btNetworkOutbound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionEditPatientNetwork("outbound");
            }
        });
        Button btAssign = (Button) mRootView.findViewById(R.id.btPatientAssign);
        btAssign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionCheckout("assign");
            }
        });
        Button btReplace = (Button) mRootView.findViewById(R.id.btPatientReplace);
        btReplace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionCheckout("replace");
            }
        });
        Button btRenew = (Button) mRootView.findViewById(R.id.btPatientRenew);
        btRenew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionCheckout("renew");
            }
        });
        Button btDeleteProfile = (Button) mRootView.findViewById(R.id.btPatientDelete);
        btDeleteProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionDeleteProfile();
            }
        });
        Button btCareplans = (Button) mRootView.findViewById(R.id.btCareplans);
        btCareplans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionCareplans();
            }
        });

        loadData(true);
    }

    private void doubleSecretInit(){
        Logcat.d("---------------DOUBLE DOUBLE DOUBLE DOUBLE SECRET INIT");
        // data is loaded, so it's ok to hide the loader
        // fade in the main content view son
        LinearLayout ll = (LinearLayout) mRootView.findViewById(R.id.llJW);
        ll.setVisibility(View.VISIBLE);
        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);

        // view state management
        try {
            // I think this is in need of garbage collection, wtf
            JSONObject profileNode = mPatientJson.getJSONObject("profile");
            String firstName = profileNode.getString("first_name");
            String title = "New Profile";
            if(firstName != null && !firstName.equals("")){
                title = firstName + "’s Profile";
            }
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(title);

            if(!profileNode.isNull("lifesquare_id")){
                mHasLifesquare = true;
            }
            if(!mPatientJson.getJSONObject("meta").isNull("coverage")){
                mHasCoverage = true;
            }
            if(profileNode.getBoolean("confirmed")){
                mConfirmed = true;
            }

            // NOTE: iOS is additive and this approach is subtractive, but okie dokie brain f
            // the nature of Android UI is XML config, vs generally building everything up in code in this context
            // it works for this

            // Continue Setup is basically visible unless we have coverage though
            // lol on certain kinds of views needing to be cast in order to "setVisibility"

            // change dat text of emergency contats manage if we haz 0
            // set here, we'll use it way below
            ArrayList<JSONObject> emergencyContacts = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("emergency"));
            if(emergencyContacts.size() == 0){
                Logcat.d("NO CONTACTS?");
                ((Button) mRootView.findViewById(R.id.btPatientEditEmergency)).setText("Add Contact");
                mRootView.findViewById(R.id.btPatientNotifyEmergency).setEnabled(false);
                mRootView.findViewById(R.id.tvEmergencyPromo).setVisibility(View.VISIBLE);
            } else {
                Logcat.d("CONTACTS?");
                // hide the
                ((Button) mRootView.findViewById(R.id.btPatientEditEmergency)).setText("Manage");
                mRootView.findViewById(R.id.btPatientNotifyEmergency).setEnabled(true);
                mRootView.findViewById(R.id.tvEmergencyPromo).setVisibility(View.GONE);
            }

            // hide the access log card, if it be blank son

            if(mHasCoverage){
                // TODO: account for future CTA an onboarding shenanigans
                mRootView.findViewById(R.id.cardOnboarding).setVisibility(View.GONE);
                mMenu.findItem(R.id.action_continue_setup).setVisible(false);

                // disable dat notify if we haz 0 contacts son

                // care plans yes / no
                JSONObject metaNode = mPatientJson.getJSONObject("meta");
                // DO NOT PUBLISH THIS TO PROD WITHOUT THE SERVER DEPLOYED BRO
                if(metaNode.getDouble("available_careplans") > 0){
                    mRootView.findViewById(R.id.cardCareplans).setVisibility(View.VISIBLE);
                    mMenu.findItem(R.id.action_advise_me).setVisible(true);
                }else {
                    mRootView.findViewById(R.id.cardCareplans).setVisibility(View.GONE);
                    mMenu.findItem(R.id.action_advise_me).setVisible(false);
                }
            } else {
                // care plans no bro
                mRootView.findViewById(R.id.cardCareplans).setVisibility(View.GONE);
                // naa son, not touching that "feature"
                // mRootView.findViewById(R.id.btPatientNotifyEmergency).setVisibility(View.GONE);
                // TEASE TEASE DANGLE THAT CARROT SON
                mRootView.findViewById(R.id.btPatientNotifyEmergency).setEnabled(false);
                mMenu.findItem(R.id.action_notify_emergency).setEnabled(false);
                // dynamically update that continue setup module son
                mRootView.findViewById(R.id.tvPatientCoverage).setVisibility(View.GONE);
                mRootView.findViewById(R.id.tvPatientCoverageSummary).setVisibility(View.GONE);
            }

            // go additive for admin actions, because why not
            mRootView.findViewById(R.id.btPatientAssign).setVisibility(View.GONE);
            // TODODODODODODOD DEVELOPER FEATURE DANGER
            mMenu.findItem(R.id.action_checkout_assign).setVisible(false);
            mRootView.findViewById(R.id.btPatientRenew).setVisibility(View.GONE);
            mMenu.findItem(R.id.action_checkout_renew).setVisible(false);
            mRootView.findViewById(R.id.btPatientReplace).setVisibility(View.GONE);
            mMenu.findItem(R.id.action_checkout_replace).setVisible(false);

            // this just simply doesn't exist in this build of the app
            //mMenu.findItem(R.id.action_advise_me).setVisible(false);

            // tease out the profile
            mRootView.findViewById(R.id.tvProfilePromo).setVisibility(View.GONE);

            if(mHasLifesquare){
                String lifesquareId = mPatientJson.getJSONObject("profile").getString("lifesquare_id");
                if(mHasCoverage){

                    // SHOW OUR CARDS AGAIN SON
                    mRootView.findViewById(R.id.cardNetworkInbound).setVisibility(View.VISIBLE);
                    mMenu.findItem(R.id.action_edit_network_inbound).setVisible(true);
                    mRootView.findViewById(R.id.cardNetworkOutbound).setVisibility(View.VISIBLE);
                    mMenu.findItem(R.id.action_edit_network_outbound).setVisible(true);

                    // TODO: REPLACEMENT FEATURE
                    // mRootView.findViewById(R.id.btPatientReplace).setVisibility(View.VISIBLE);
                    // mMenu.findItem(R.id.action_checkout_replace).setVisible(true);
                    // coverage summary
                    JSONObject coverage = mPatientJson.getJSONObject("meta").getJSONObject("coverage");
                    String summary = "";
                    if(coverage.getBoolean("recurring")){
                        summary = "Auto-renews on " + coverage.getString("end_date");
                        // TODO: show dat cancel recurring coverage button action thingy maybe sometime never
                    } else {
                        summary = "Valid until " + coverage.getString("end_date");
                    }
                    ( (TextView) mRootView.findViewById(R.id.tvPatientCoverage)).setText("Coverage for LifeSticker " + lifesquareId);
                    ( (TextView) mRootView.findViewById(R.id.tvPatientCoverageSummary)).setText(summary);
                } else{
                    // Missing or expired coverage
                    // TODO: RENEW FEATURE
                    // mRootView.findViewById(R.id.btPatientRenew).setVisibility(View.VISIBLE);
                    // mMenu.findItem(R.id.action_checkout_renew).setVisible(true);
                    TextView tvSummary = (TextView) mRootView.findViewById(R.id.tvPatientCoverage);
                    tvSummary.setVisibility(View.VISIBLE);
                    tvSummary.setText("No Coverage for LifeSticker " + lifesquareId);
                    ( (TextView) mRootView.findViewById(R.id.tvPatientCoverageSummary)).setText("Please Renew");
                }


                // LIFESQUARE CARD SON yea, forgot about DRE
                mRootView.findViewById(R.id.cardLifesquare).setVisibility(View.VISIBLE);
                final int size = 48;
                // if we has a profile pic, load that shizzle nizzle
                if(!profileNode.isNull("photo_uuid")){
                    ImageView profilePhoto = (ImageView) mRootView.findViewById(R.id.ivPatientPhoto);
                    String url = Config.API_ROOT + "profiles/" + profileNode.getString("uuid") + "/profile-photo?photo_uuid=" + profileNode.getString("photo_uuid") + "&width=" + (size*2) + "&height=" + (size*2);

                    mImageLoader.displayImage(url, profilePhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            // circle Twerk da croppers ploppers
                            Bitmap circleCroppers86 = Files.getCroppedBitmap(loadedImage, size * 2);
                            ((ImageView) view).setImageBitmap(circleCroppers86);
                            // ((ImageView) view).setImageBitmap(loadedImage);
                            ((ImageView) view).setScaleType(ImageView.ScaleType.CENTER_CROP);
                        }
                    });
                }

                // load lifesquare image
                ImageView lifesquarePhoto = (ImageView) mRootView.findViewById(R.id.ivLifesquareQRCode);
                String url = Config.API_ROOT + "lifesquares/" + mPatientJson.getJSONObject("profile").getString("lifesquare_id") + "/image?width=" + (size*2) + "&height=" + (size*2);
                mImageLoader.displayImage(url, lifesquarePhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        ((ImageView) view).setImageBitmap(loadedImage);
                        ((ImageView) view).setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                });

                // TESTING UI OPTIONS
                // hide the original big ass lifesquare guy
                //

                // configure emergency contacts adapter
                ListView lvEmergency = (ListView) mRootView.findViewById(R.id.lvPatientEmergency);
                CollectionAdapter emergencyAdapter = new CollectionAdapter(this.getContext(), emergencyContacts, "emergency", mPatientId);
                emergencyAdapter.mEdit = false;
                lvEmergency.setAdapter(emergencyAdapter);
                LayoutHelper.setListViewHeightBasedOnItems(lvEmergency);

                ListView lvHistory = (ListView) mRootView.findViewById(R.id.lvPatientAccessLog);
                AccessLogAdapter historyAdapter = new AccessLogAdapter(this.getContext(), JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("access_log")));
                lvHistory.setAdapter(historyAdapter);
                LayoutHelper.setListViewHeightBasedOnItems(lvHistory);

                if(lvHistory.getAdapter().getCount() == 0){
                    mRootView.findViewById(R.id.cardAccessLog).setVisibility(View.GONE);
                } else {
                    mRootView.findViewById(R.id.cardAccessLog).setVisibility(View.VISIBLE);
                }

                JSONObject networkNode = mPatientJson.getJSONObject("network");

                // network inbound
                // CTA custom town
                JSONArray connectionsIn = networkNode.getJSONArray("granters");
                ListView lvIn = (ListView) mRootView.findViewById(R.id.lvPatientNetworkInbound);
                if(connectionsIn.length() > 0){
                    lvIn.setVisibility(View.VISIBLE);
                    PatientNetworkConnectionsAdapter adapter = new PatientNetworkConnectionsAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(connectionsIn),
                            mPatientId,
                            "inbound"
                    );
                    lvIn.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lvIn);
                } else {
                    lvIn.setVisibility(View.GONE);
                }

                // network outbound
                JSONArray connectionsOut = networkNode.getJSONArray("auditors");
                ListView lvOut = (ListView) mRootView.findViewById(R.id.lvPatientNetworkOutbound);
                if(connectionsOut.length() > 0){
                    lvOut.setVisibility(View.VISIBLE);
                    PatientNetworkConnectionsAdapter adapter = new PatientNetworkConnectionsAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(connectionsOut),
                            mPatientId,
                            "outbound"
                    );
                    lvOut.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lvOut);
                } else {
                    lvOut.setVisibility(View.GONE);
                }

                // network outbound pending
                JSONArray connectionsPending = networkNode.getJSONArray("auditors_pending");
                ListView lvPending = (ListView) mRootView.findViewById(R.id.lvPatientNetworkOutboundPending);
                if(connectionsPending.length() > 0){
                    mRootView.findViewById(R.id.cardNetworkOutboundPending).setVisibility(View.VISIBLE);
                    PatientNetworkInvitesAdapter adapter = new PatientNetworkInvitesAdapter(
                            getContext(),
                            JSONHelper.jsonArrayToArrayList(connectionsPending),
                            mPatientId,
                            "outbound"
                    );
                    lvPending.setAdapter(adapter);
                    LayoutHelper.setListViewHeightBasedOnChildren(lvPending);
                } else {
                    mRootView.findViewById(R.id.cardNetworkOutboundPending).setVisibility(View.GONE);
                }
            } else {
                // basic hiding of all the things and so on
                mRootView.findViewById(R.id.cardLifesquare).setVisibility(View.GONE);
                mMenu.findItem(R.id.action_view_lifesquare).setVisible(false);
                mRootView.findViewById(R.id.cardNetworkInbound).setVisibility(View.GONE);
                mMenu.findItem(R.id.action_edit_network_inbound).setVisible(false);
                mRootView.findViewById(R.id.cardNetworkOutbound).setVisibility(View.GONE);
                mMenu.findItem(R.id.action_edit_network_outbound).setVisible(false);
                mRootView.findViewById(R.id.cardNetworkOutboundPending).setVisibility(View.GONE);
                mRootView.findViewById(R.id.cardAccessLog).setVisibility(View.GONE);

                mRootView.findViewById(R.id.tvProfilePromo).setVisibility(View.VISIBLE);

                if(mConfirmed){
                    mRootView.findViewById(R.id.btPatientAssign).setVisibility(View.VISIBLE);
                    mMenu.findItem(R.id.action_checkout_assign).setVisible(true);
                    // you just need your damn coverage son
                    mRootView.findViewById(R.id.tvProfilePromo).setVisibility(View.GONE);
                }
            }

        } catch(Exception e){
            Logcat.d(e.getMessage() + '-' + e.getLocalizedMessage());
        }

    }

    private String getPatientName(){
        String name = "New Profile";
        try {
            JSONObject profileNode = mPatientJson.getJSONObject("profile");
            // HIGHLY unlikely this is gonna be null ish
            if (!profileNode.isNull("first_name")) {
                name = profileNode.getString("first_name");
            }
            if (!profileNode.isNull("middle_name")) {
                String middle = profileNode.getString("middle_name");
                if (!middle.equals("")) {
                    name += " " + middle.substring(0, 1);
                }
            }
            if (!profileNode.isNull("last_name")) {
                name += " " + profileNode.getString("last_name");
            }
        } catch(Exception e){

        }
        return name;
    }

    private String getPatientWebviewURL(){
        String url = null;
        if(mHasLifesquare){
            try {
                JSONObject profileNode = mPatientJson.getJSONObject("profile");
                url = Config.API_ROOT + "lifesquares/" + profileNode.getString("lifesquare_id") + "/webview";
            } catch(Exception e){

            }
        }
        return url;
    }


    // open this bitch up to public so we can tap it from the Parent Activity
    private void loadData(Boolean showLoader) {
        // THIS IS COMEDY TIMES
        HealthNotifierAPI.getInstance().getPatient(mPatientId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Error loading profile. No network connection to server.", Toast.LENGTH_SHORT).show();
                        // meh or is it our webservice vomiting 500's
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        mPatientJson = null;
                        // DOUBLE PUNCH IT??
                        mPatientJson = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                doubleSecretInit();
                            }
                        });
                    } catch (Exception e) {
                        //
                    }
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Error loading profile.", Toast.LENGTH_SHORT).show();
                            // meh or is it our webservice vomiting 500's
                        }
                    });
                }
            }
        });

        // if (showLoader)

        // mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    // all the action handlers son
    private void nextLevelShitContinueSetup(){
        // aka past the gatekeeper of ok with none of that content and so on and so fortht
        try {
            JSONObject profileNode = mPatientJson.getJSONObject("profile");
            if(!profileNode.getBoolean("confirmed")){
                actionConfirmProfile();
                return;
            }

            if(!mHasLifesquare){
                Toast.makeText(getContext(), "Please get LifeStickers!", Toast.LENGTH_LONG).show();
                actionCheckout("assign");
                return;
            }
            // launch that confirmed dialog
            // be sure to mention if we don't have emergency contacts at this point
            // for extra credit, introspect medical record

            // are we confirmed without a lifesquare
            // segue to assign action
            if(!mHasCoverage){
                Toast.makeText(getContext(), "Please renew coverage at https://www.domain.com", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e){

        }
    }

    private void actionContinueSetup(){
        try {
            JSONObject profileNode = mPatientJson.getJSONObject("profile");
            // TODO: it's like our local onboarding checker
            // initially we do things based on server state
            // catch22 we don't store (locally or remote) the skip med/contacts progress
            // so it's a little sloppy

            // ok, we're gonna 86 all previous harcore work, and do our own

            //
            // do we have a name, birthdate and at least 1 residence
            // else Edit Personal
            JSONArray addresses = mPatientJson.getJSONArray("addresses");
            if(profileNode.getString("first_name").equals("") || addresses.length() == 0){
                // basically no way to save Jesus birthday ever, so name check should be good enough ish
                Toast.makeText(getContext(), "Please fill all required profile fields and continue.", Toast.LENGTH_LONG).show();
                actionEditProfile();
                return;
            }

            // who cares about medical, and insurances

            // should we require an Emergency Contact? no
            JSONArray emergency = mPatientJson.getJSONArray("emergency");
            if(emergency.length() == 0){
                //prompt for that
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle("Missing Emergency Contacts");
                alertDialog.setMessage("While you can add them later, it’s a good idea to add at least 1 contact now.");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Skip Contacts",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // call dat delete
                                nextLevelShitContinueSetup();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Add Contacts",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                actionEditEmergency(); // lol bro
                            }
                        });
                alertDialog.show();
                return;
            }


            // are we confirmed
            nextLevelShitContinueSetup();

            // for some dumb reason do we have expired coverage
            // set something on fire, and call charles
            // aka get the renewCoverage action up and running
        } catch(Exception e){
            // lOLZONE BRO
            Logcat.d(e.toString());
        }

    }

    private void actionConfirmProfile(){
        // this is the prompting and shit, whill call confirmProfile the DB side son
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle("Please Confirm");
        alertDialog.setMessage(getString(R.string.dialog_confirm_profile_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Confirm",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // call dat delete
                        confirmProfile();
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

    private void actionViewLifesquare(){
        Intent intent;
        // obtain name and build a webview
        // ain't nobody in "this" activity that isn't also a patient
        // and you ain't getting a LifeSticker "webview" unless you're all set son, yea son, just how it works
        // naturally we're gonna drop the webview in a future build for native views, but let's reduce any further
        // dependency on the webviewurl coming from the Patients API, mmkay
        if(mHasLifesquare) {
            try {
                intent = new Intent(getContext(), LifesquareActivity.class);
                intent.putExtra("PATIENT_ID", mPatientId);
                intent.putExtra("PATIENT_NAME", getPatientName()); // TODO: retire FOR CONVENIENCE SON
                intent.putExtra("PATIENT_WEBVIEW_URL", getPatientWebviewURL()); // TODO: retire FOR CONVENIENCE SON
                startActivity(intent);
            } catch (Exception e) {

            }
        }
    }

    private void actionEditProfile(){
        Intent intent;
        intent = new Intent(getContext(), EditProfileActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO!
        intent.putExtra("PATIENT_JSON", mPatientJson.toString());
        startActivity(intent);
    }

    private void actionEditMedical(){
        Intent intent;
        intent = new Intent(getContext(), EditMedicalActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO!
        intent.putExtra("PATIENT_JSON", mPatientJson.toString());
        startActivity(intent);
    }

    private void actionEditContacts(){
        Intent intent;
        intent = new Intent(getContext(), EditContactsActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO!
        intent.putExtra("PATIENT_JSON", mPatientJson.toString());
        startActivity(intent);
    }

    private void actionEditEmergency(){
        // donkey interception though
        // if we have 0 emergency items, just go straight to the Edit Collection Activity, F YEA ANDROID INTENTS
        Intent intent;
        try {
            JSONArray contacts = mPatientJson.getJSONArray("emergency");
            if(contacts.length() == 0){
                intent = new Intent(getContext(), EditCollectionItemActivity.class);
                intent.putExtra("PATIENT_UUID", mPatientId);
                intent.putExtra("COLLECTION_ID", "emergency");//yup son
                intent.putExtra("CALLING_ACTIVITY_NAME", getContext().getClass().getName());
                startActivity(intent);
            } else {
                intent = new Intent(getContext(), EditEmergencyActivity.class);
                intent.putExtra("PATIENT_ID", mPatientId);
                intent.putExtra("PATIENT_JSON", mPatientJson.toString());
                startActivity(intent);
            }
        } catch(Exception e){
            Logcat.d("NO CONTACTS FOR YOU SON");
        }
    }

    private void actionNotifyEmergency(){
        Intent intent;
        intent = new Intent(getContext(), NotifyEmergencyContactsActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO!
        intent.putExtra("PATIENT_JSON", mPatientJson.toString());
        startActivity(intent);
    }

    private void actionCheckout(String mode){
        // 3 in 1
        Intent intent;
        intent = new Intent(getContext(), CheckoutActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO!
        intent.putExtra("PATIENT_JSON", mPatientJson.toString());
        intent.putExtra("MODE", mode);
        startActivity(intent);
    }

    private void actionEditPatientNetwork(String mode){
        // 2 in 1
        Intent intent;
        intent = new Intent(getContext(), PatientNetworkManageActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO!
        intent.putExtra("PATIENT_JSON", mPatientJson.toString()); // currently out of sync when we do network deletes from this landing screen lolbro
        intent.putExtra("MODE", mode);
        startActivity(intent);
    }

    private void actionCareplans(){
        Intent intent;
        intent = new Intent(getContext(), CareplansActivity.class);
        intent.putExtra("PATIENT_ID", mPatientId); // NO! , seems unecessary bro brizzle
        startActivity(intent);
    }

    private void actionDeleteProfile(){
        promptDeleteProfile();
    }

    // TODO: cancel recurring coverage
    private void actionCancelSubscription(){
        // meh not sure about this one
    }

    // IMPL for this here class

    private void promptDeleteProfile(){
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle("Really Delete Profile?");
        alertDialog.setMessage(getString(R.string.dialog_delete_profile_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // call dat delete
                        deleteProfile();
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

    private void deleteProfile() {
        // lock it down son, chop it down, then finish this here activity
        // toast your nuts silly
        final Activity activity = getActivity();
        HealthNotifierAPI.getInstance().deleteProfile(mPatientId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d("onFailure" + e.toString());
                // mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Error deleting your profile :( ", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getContext(), "Profile deleted!", Toast.LENGTH_SHORT).show();
                            // TODO: do the jimmy hack, aka reloadProfiles message, which just will grab the bizzle sizzle
                            activity.finish();
                        }
                    });
                } else {
                    Logcat.d("onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Error deleting your profile :( ", Toast.LENGTH_SHORT).show();
                            // context.finish();
                        }
                    });
                }
            }
        });
    }

    private void confirmProfile(){
        // lock it down son, chop it down, then finish this here activity
        // toast your nuts silly
        final Activity activity = getActivity();
        HealthNotifierAPI.getInstance().confirmProfile(mPatientId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                // mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Error confirming your profile :( ", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getContext(), "Profile confirmed! Let’s get you some LifeStickers.", Toast.LENGTH_LONG).show();
                            // faux save confirmed back into our Profile bro
                            try {
                                Logcat.d("BEFORE " + mPatientJson.getJSONObject("profile").getBoolean("confirmed"));
                                mPatientJson.getJSONObject("profile").put("confirmed", true);
                                Logcat.d("AFTER " + mPatientJson.getJSONObject("profile").getBoolean("confirmed"));
                            }catch(Exception e){
                                Logcat.d("FAIL BALLS" + e.toString());
                            }
                            actionCheckout("assign");
                        }
                    });
                } else {
                    Logcat.d( "onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Error confirming your profile :( ", Toast.LENGTH_SHORT).show();
                            // context.finish();
                        }
                    });
                }
            }
        });

    }

    @Subscribe
    public void handleGenericEvent(GenericEvent event) {
        if (event.eventName.equals("FetchPatient")) {
            // check dem patient_uuid and see if it matches though
            if(event.attributes.get("PatientId").equals(mPatientId)) {
                loadData(true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HealthNotifierApplication.bus.unregister(this);
    }

}
