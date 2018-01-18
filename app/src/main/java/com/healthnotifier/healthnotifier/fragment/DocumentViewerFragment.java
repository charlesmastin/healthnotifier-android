package com.healthnotifier.healthnotifier.fragment;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.squareup.otto.Bus;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;

public class DocumentViewerFragment extends Fragment {

    private String mDocumentId;
    private String mFileIndex;
	private View mRootView;
	private WebView mWvClient;
    private JSONObject mDocumentJson;
    private Menu mMenu;
    private Handler mHandler;
    private Boolean mWebviewLoaded = false;
    private Date timerStart;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
        mDocumentId = getActivity().getIntent().getStringExtra("DocumentId");
        mFileIndex = getActivity().getIntent().getStringExtra("FileIndex");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_document_viewer, container, false);
        timerStart = new Date();
        LayoutHelper.initActionBar(getActivity(), mRootView, "");
		return mRootView;
	}

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadData();
    }

    private void doubleSecretInit(){
        try {
            if(!mDocumentJson.isNull("Title")){
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(mDocumentJson.getString("Title"));
            } else {
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(prettyPrintDocumentType(mDocumentJson.getString("Category")));
            }

            mWvClient = (WebView) mRootView.findViewById(R.id.wvContent);
            mWvClient.getSettings().setJavaScriptEnabled(true);
            mWvClient.getSettings().setBuiltInZoomControls(true);
            mWvClient.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

            //
            WebViewClient myWebClient = new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (!mWebviewLoaded) {
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        focusAnchorTag();
                        mWebviewLoaded = true;
                    }
                }
            };

            mWvClient.setWebViewClient(myWebClient);
            mWvClient.loadDataWithBaseURL("", mDocumentJson.getString("Html"), "text/html", "UTF-8", "");
        } catch(Exception e){

        }

    }

    private void loadData(){
        HealthNotifierAPI.getInstance().getDocument(mDocumentId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d("onFailure" + e.toString());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // OFFLINE AND SCREWED PRETTY MUCH
                        Toast.makeText(getContext(), "Unable to load Document", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    // access body here
                    try {
                        mDocumentJson = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                doubleSecretInit();
                            }
                        });
                    } catch(Exception e){
                        // so broken here
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Unable to load Document", Toast.LENGTH_SHORT).show();
                                getActivity().finish();
                            }
                        });
                    }

                } else {
                    Logcat.d("onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Unable to load Document", Toast.LENGTH_SHORT).show();
                            getActivity().finish();
                            // oh hell nothing to say much, and really need to close the activiy
                        }
                    });
                }
            }
        });
    }

	@Override
	public void onDestroyView() {
		// log analytics
		Bus bus = HealthNotifierApplication.bus;
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("DocumentId", mDocumentId);
		attributes.put("DocumentIndex", mFileIndex);

        Date timerEnd = new Date();
        long duration = timerEnd.getTime() - timerStart.getTime();
        attributes.put("ViewDuration", TimeUnit.MILLISECONDS.toSeconds(duration));

		attributes.put("LoadingComplete", mWebviewLoaded);
		bus.post(new AnalyticsEvent("Document View", attributes));
        Logcat.d("ADD ANALYTICS");
        // stop the stuffs
        if(mWvClient != null){
            mWvClient.stopLoading();
        }


		super.onDestroyView();
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void focusAnchorTag(){
        // because we're not doing a native view, we have to pass in the anchor via js to the webview, LOLZOR™
        if(mWvClient != null && !mFileIndex.equals("#file-0")){
            // find a better way
            if(Build.VERSION.SDK_INT >= 19) {
                mWvClient.evaluateJavascript("window.location.href = '" + mFileIndex + "';", null);
            }
        }
    }

    private String prettyPrintDocumentType(String documentType){
        switch(documentType){
            case "POLST" :
                return "POLST Form";
            case "ADVANCE_DIRECTIVE" :
                return "Advance Directive";
            case "IMAGING_RESULT" :
                return "Imaging Result";
            case "MEDICAL_NOTE" :
                return "Medical Note";
            case "LAB_RESULT" :
                return "Lab Result";
            default:
                return documentType;
        }
    }

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
