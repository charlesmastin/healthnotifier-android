package com.healthnotifier.healthnotifier.fragment;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.adapter.ScanHistoryAdapter;
import com.healthnotifier.healthnotifier.model.ScanHistory;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.squareup.otto.Bus;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class RecentScansFragment extends Fragment {
	private final String SCANNED_ITEM = "scanned_item";
	private View mRootView;
	private ScanHistoryAdapter mAdapter;
    private Realm realm;
    private RealmResults<ScanHistory> realmResults;
    public static final long HOUR = 3600*1000;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_recent_scans, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_history:
                clearScans();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_recent_scans, container, false);
		return mRootView;
	}

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);

        loadData();
        ListView lv = (ListView) mRootView.findViewById(R.id.lvRecentScans);
        mAdapter = new ScanHistoryAdapter(getActivity(), realmResults);
        lv.setAdapter(mAdapter);
        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();

        // analytics
        Bus bus = HealthNotifierApplication.bus;
        Map<String, Object> attributes = new HashMap<String, Object>();
        bus.post(new AnalyticsEvent("History View", attributes));

        // set timeout and remove items bro
        removeExpiredScans();

    }

    // on da resume bro

    @Override
    public void onStart() {
        super.onStart();
        realm = Realm.getDefaultInstance();
        // removeExpiredScans();
    }

    @Override
    public void onStop() {
        super.onStop();
        realm.close();
    }

	private void loadData() {
        RealmQuery<ScanHistory> query = realm.where(ScanHistory.class);
        // we could to async but it's not needed
        realmResults = query.findAll();
        // TODO: schedule removal but we could just do it casually on view
		// RecentLifesquare.removeOldLifesquares();
        if(realmResults.size() > 0){
            mRootView.findViewById(R.id.tvEmptyList).setVisibility(View.GONE);
        } else {
            mRootView.findViewById(R.id.tvEmptyList).setVisibility(View.VISIBLE);
        }
	}

    private void clearScans() {
        RealmResults results = realm.where(ScanHistory.class).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();
        mAdapter.notifyDataSetChanged();
    }

    private void removeExpiredScans() {
        // TODO: hit this via the timer example in the Application class
        Preferences pref = HealthNotifierApplication.preferences;
        Long hours = 1L;
        if(pref.getProvider()){
            hours = 6L;
        }
        Date now = new Date();
        Date endDate = new Date(now.getTime() - (hours * HOUR));
        RealmResults results = realm.where(ScanHistory.class).lessThan("createdAt", endDate).findAll();
        realm.beginTransaction();
        results.deleteAllFromRealm();
        realm.commitTransaction();
        mAdapter.notifyDataSetChanged();
    }
	
}
