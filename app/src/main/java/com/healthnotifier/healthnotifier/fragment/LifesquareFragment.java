package com.healthnotifier.healthnotifier.fragment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.support.design.widget.TabLayout;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.DocumentViewerActivity;
import com.healthnotifier.healthnotifier.activity.MainActivity;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import android.support.v4.app.Fragment;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class LifesquareFragment extends Fragment {

    private View mRootView;
    private WebView mWvClient;
    private String mPatientId;
    private String mPatientName;
    private String mWebviewUrl;
    private int mTabIndex = 0;
    private Date timerStart;
    private Date timerTabStart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        // this is unique for this long-standing fragment which spawns a "descending" stack of activities
        HealthNotifierApplication.bus.register(this);

        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        Bundle bd = intent.getExtras();

        if(bd != null) {
            mPatientId = (String) bd.get("PATIENT_ID");
            mPatientName = (String) bd.get("PATIENT_NAME");
            mWebviewUrl = (String) bd.get("PATIENT_WEBVIEW_URL");
            Preferences prefs = HealthNotifierApplication.preferences;
            prefs.setCurrentPatientId(mPatientId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_lifesquare, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String title = "LifeSticker"; // coincidentally the default app title
        if(mPatientName != null) {
            title = mPatientName;
        }
        LayoutHelper.initActionBar(getActivity(), mRootView, title);
        timerStart = new Date();
        // this gets reset when the webview has loaded, this is to avoid crashing, and to initialize things nicely
        timerTabStart = new Date();

        setTabs();
        setWebView();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish(); // lazy town USA
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView(){

        logTabView();

        // known issue, since we persist this view, the timer will keep running with we look at documents, or add documents
        // but this is ok, because it's the total time we had a patient "chart" open

        // TODO: what happens if the app is immediately backgrounded or whatever
        Bus bus = HealthNotifierApplication.bus;
        Map<String, Object> attributes = new HashMap<String, Object>();
        Date timerEnd = new Date();
        long duration = timerEnd.getTime() - timerStart.getTime();
        attributes.put("ViewDuration", TimeUnit.MILLISECONDS.toSeconds(duration));
        bus.post(new AnalyticsEvent("Patient View", attributes));

        super.onDestroyView();
    }

    private void setWebView() {
        mWvClient = (WebView) mRootView.findViewById(R.id.wvContent);
        mWvClient.setVisibility(View.VISIBLE);

        mWvClient.getSettings().setJavaScriptEnabled(true);
        mWvClient.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        WebViewClient myWebClient = new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                if (url.contains("geo:")){
                    Uri gmmIntentUri = Uri.parse(url); //Uri.parse("geo:37.7749,-122.4194");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getContext().getPackageManager()) != null) {

                        Bus bus = HealthNotifierApplication.bus;
                        Map<String, Object> attributes = new HashMap<String, Object>();
                        attributes.put("Geo", url.replace("geo:", ""));
                        bus.post(new AnalyticsEvent("Map View", attributes));

                        startActivity(mapIntent);

                        return true;
                    }
                }

                if (url.contains("tel:")){

                    Uri phoneNumber = Uri.parse(url);

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_DIAL);
                    intent.setData(phoneNumber);
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {

                        Preferences prefs = HealthNotifierApplication.preferences;
                        Bus bus = HealthNotifierApplication.bus;
                        Map<String, Object> attributes = new HashMap<String, Object>();
                        attributes.put("AccountId", prefs.getAccountId());
                        attributes.put("Provider", prefs.getProvider());
                        attributes.put("PatientId", prefs.getCurrentPatientId());
                        attributes.put("PhoneNumber", phoneNumber.toString().replace("tel:", ""));
                        // TODO: better encode the link protocol to pass in some context?
                        bus.post(new AnalyticsEvent("Phone Dial", attributes));

                        startActivity(intent);
                    }
                    return true;
                }

                if (url.contains("mailto:")){
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    String email = url.replace("mailto:", "");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Hello From HealthNotifier");
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivity(intent);
                        Bus bus = HealthNotifierApplication.bus;
                        Map<String, Object> attributes = new HashMap<String, Object>();
                        attributes.put("Email", email);
                        bus.post(new AnalyticsEvent("Email", attributes));
                        return true;
                    }
                }

                if (url.contains("document/view/")) {
                    Intent intent = new Intent(getActivity(), DocumentViewerActivity.class);
                    String[] tA = url.split("/");
                    intent.putExtra("url", url);
                    intent.putExtra("DocumentId", tA[tA.length-2]);
                    intent.putExtra("FileIndex", tA[tA.length-1]);
                    startActivity(intent);
                    return true;
                }
                return false;
            }

            public void onPageFinished(WebView view, String url) {
                timerTabStart = new Date();
                // BOOYA SON
                // technically we need to actually interact with the tab bar itself, SON
                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                renderHtmlTab();
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
                Logcat.d("WEBVIEW ERROR SON" + errorCode);
                if(errorCode == 401){
                    Bus bus = HealthNotifierApplication.bus;
                    bus.post(new GenericEvent("onTokenExpired"));
                }
            }

            @TargetApi(23)
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Logcat.d("WEBVIEW ERROR SON" + error.getErrorCode());
                if (error.getErrorCode() == 401) {
                    Bus bus = HealthNotifierApplication.bus;
                    bus.post(new GenericEvent("onTokenExpired"));
                }
            }
        };

        mWvClient.setWebViewClient(myWebClient);

        Preferences prefs = HealthNotifierApplication.preferences;
        HashMap<String, String> headers = new HashMap<String, String>();
        // MEH
        headers.put("Authorization", "Bearer " + HealthNotifierAPI.getInstance().accessToken);
        //headers.put("X-Account-Token", prefs.getAuthToken());
        //headers.put("X-Account-Email", prefs.getEmail());
        // FIXME: doesn't have acccess to request.Request which sets a API_ENDPOINT from the config
        String url = mWebviewUrl;

        MainActivity activity = HealthNotifierApplication.getMainActivity();
        if(HealthNotifierApplication.getCurrentLocation()!=null){
            Uri requestUri = Uri.parse(url).buildUpon()
                    .appendQueryParameter("latitude", String.valueOf(HealthNotifierApplication.getCurrentLocation().getLatitude()))
                    .appendQueryParameter("longitude", String.valueOf(HealthNotifierApplication.getCurrentLocation().getLongitude()))
                    .build();
            url = requestUri.toString();
        }

        mWvClient.loadUrl(url, headers);
        // TODO: on load of webview, be sure to reset the current tab state SON

        // handle that error

    }

    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        // THIS IS A hACK TO KICK OFF A RELOAD OF THAT WEBVIEW SON, in cases where you add and remove documents
        if (event.eventName.equals("onPatientDataChanged")) {
            if(mWvClient != null){
                // this should hook the hookers aka for the loaders
                mWvClient.reload();
            }
        }
    }

    private void logTabView(){
        Bus bus = HealthNotifierApplication.bus;
        Map<String, Object> attributes = new HashMap<String, Object>();
        // array of tab names - lol on canonical location for this, SON
        String[] tabNames = {"Personal", "Medical", "Contact"};
        attributes.put("Name", tabNames[mTabIndex]);
        // TODO: track if the user actually scrolled
        attributes.put("Scrolled", false);
        // or just roll it up into interacted, which would capture any click or whatever
        Date timerEnd = new Date();
        long duration = timerEnd.getTime() - timerTabStart.getTime();
        attributes.put("ViewDuration", TimeUnit.MILLISECONDS.toSeconds(duration));
        bus.post(new AnalyticsEvent("Patient Tab View", attributes));
    }

    private void setTabs() {
        TabLayout tabLayout = (TabLayout) mRootView.findViewById(R.id.lifesquareTabs);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // analytics son
                logTabView();

                // reset it son
                mTabIndex = tab.getPosition();
                timerTabStart = new Date();

                // actually render it up now
                renderHtmlTab();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void renderHtmlTab(){
        switch (mTabIndex) {
            case 0:
                mWvClient.loadUrl("javascript:displayTabByIndex(0);");
                break;
            case 1:
                mWvClient.loadUrl("javascript:displayTabByIndex(1);");
                break;
            case 2:
                mWvClient.loadUrl("javascript:displayTabByIndex(2);");
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HealthNotifierApplication.bus.unregister(this);
        // mAPICallManager.cancelAllTasks();
    }

    // on resume etc
    @Override
    public void onResume(){
        timerTabStart = new Date();
        timerStart = new Date();
        super.onResume();
    }

    // restore those timers bizzle nizzle

}
