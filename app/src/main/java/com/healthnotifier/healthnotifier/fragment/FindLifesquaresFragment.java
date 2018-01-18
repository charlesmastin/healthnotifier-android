package com.healthnotifier.healthnotifier.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.MainActivity;
import com.healthnotifier.healthnotifier.adapter.FindLifesquaresAdapter;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;

public class FindLifesquaresFragment extends Fragment {

	private View mRootView;
	private Boolean mLocked = false;
    private Handler mHandler;
    private Menu mMenu;
	private FindLifesquaresAdapter mAdapter;
	private ArrayList<JSONObject> mLifesquares = new ArrayList<JSONObject>();

    private Boolean mListeningForLocationUpdate = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_find_lifesquares, container, false);
        return mRootView;
	}

	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        mAdapter = new FindLifesquaresAdapter(getActivity(), mLifesquares);
        ListView lvSearch = (ListView) mRootView.findViewById(R.id.lvSearch);
        lvSearch.setAdapter(mAdapter);

        EditText etSearch = (EditText) mRootView.findViewById(R.id.etSearch);
        etSearch.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideKeyboard();
                    searchLifesquares(v.getText().toString());
                }
                return handled;
            }

        });
        checkLocationPermissions();
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_find_lifesquares, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_location_search:
                checkLocationPermissions();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // LET's umm basically remove this because of how often we nagg the crap out of the user
    // since we want to cut down on potential bugs with so much code
    // TODO: optimize, and remove
    private void checkLocationPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                // do we need rationale
                if (shouldShowRequestPermissionRationale(permission)) {
                    // TODO: explain it, hmm and ask for it
                    Snackbar sb = Snackbar.make(mRootView, "Permission required to use nearby location", Snackbar.LENGTH_INDEFINITE);
                    sb.setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sb.dismiss();
                            requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_LOCATION_CODE);
                        }
                    });
                    sb.show();
                } else {
                    requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_LOCATION_CODE);
                }
            } else {
                if(HealthNotifierApplication.getCurrentLocation() != null){
                    loadNearby();
                } else {
                    HealthNotifierApplication.enableLocationServices();
                    // // we needs us a GenericEvent from the Application duh duh duh
                }
            }
        } else {
            if(HealthNotifierApplication.getCurrentLocation() != null){
                loadNearby();
            } else {
                HealthNotifierApplication.enableLocationServices();
                // TODO: notify and then queue that shit, aka, F dis shee
                // we needs us a GenericEvent from the Application duh duh duh
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case HealthNotifierApplication.PERMISSION_LOCATION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    HealthNotifierApplication.enableLocationServices();
                    // queue that up son
                    // loadNearby();
                } else {
                    Snackbar.make(mRootView, "Nearby search disabled", Snackbar.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // and umm, perhaps reduce the nagging rate while currently logged in bro briz
                    // given that this is a "fragment" that would be needed, else each time they return it's gonna spam bomb dem
                }
                return;
            }
        }
    }

    private void doubleSecretInit(String mode){
        // jimmy slap dem results back in da view son
        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
        mAdapter.replaceItems(mLifesquares);
        mAdapter.notifyDataSetChanged();
        // meh, balls™
        setLifesquaresCount();
        // analytics up in this
        Bus bus = HealthNotifierApplication.bus;
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("Results", mLifesquares.size());
        bus.post(new AnalyticsEvent(mode, attributes));
    }

    private void loadNearby(){
        mLocked = true;
        // we probably want a way to go back to this, like clicking the Nearby bizzle, or having a Menu item to go back to nearby only, w/e FML
        if(HealthNotifierApplication.getCurrentLocation() == null){
            Snackbar.make(mRootView, "Location Disabled or Unavailable", Snackbar.LENGTH_LONG).show();
            return;
        }
        mListeningForLocationUpdate = false;

        ((EditText) mRootView.findViewById(R.id.etSearch)).setText("");
        // TODO: visual state change son

        // meh
        HealthNotifierAPI.getInstance().nearbyLifesquares(HealthNotifierApplication.getCurrentLocation().getLatitude(), HealthNotifierApplication.getCurrentLocation().getLongitude(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d("onFailure" + e.toString());
                mLocked = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // OFFLINE AND SCREWED PRETTY MUCH
                        Toast.makeText(getContext(), "Network offline", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                mLocked = false;
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    // access body here
                    try {
                        JSONObject responseJson = new JSONObject(response.body().string());
                        mLifesquares = JSONHelper.jsonArrayToArrayList(responseJson.getJSONArray("Lifesquares"));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                doubleSecretInit("Nearby");
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

	private void hideKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(
				Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private void searchLifesquares(String keywords) {
        mListeningForLocationUpdate = false;//for now bro
		mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
		Preferences pref = HealthNotifierApplication.preferences;
		MainActivity activity = (MainActivity) getActivity();
        Double latitude = null;
        Double longitude = null;
        if(HealthNotifierApplication.getCurrentLocation()!=null){
            latitude = HealthNotifierApplication.getCurrentLocation().getLatitude();
            longitude = HealthNotifierApplication.getCurrentLocation().getLongitude();
        }
        HealthNotifierAPI.getInstance().searchLifesquares(keywords, latitude, longitude, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d("onFailure" + e.toString());
                mLocked = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // OFFLINE AND SCREWED PRETTY MUCH
                        Toast.makeText(getContext(), "Network offline", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                mLocked = false;
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    // access body here
                    try {
                        JSONObject responseJson = new JSONObject(response.body().string());
                        mLifesquares = JSONHelper.jsonArrayToArrayList(responseJson.getJSONArray("Lifesquares"));
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                doubleSecretInit("Search");
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

    private void setLifesquaresCount() {
        TextView tvLifesquaresCount = (TextView) mRootView.findViewById(R.id.tvLifesquaresCount);
        tvLifesquaresCount.setText(mLifesquares.size() + " " + getString(R.string.find_lifesquares));
    }

    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        if (event.eventName.equals("onLocationUpdate") && event.attributes != null) {
            if(mListeningForLocationUpdate){
                loadNearby();
            }
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
