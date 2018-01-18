package com.healthnotifier.healthnotifier.activity;

import java.util.ArrayList;
import java.util.List;

import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.adapter.MainNavigationAdapter;

import com.healthnotifier.healthnotifier.fragment.FindLifesquaresFragment;
import com.healthnotifier.healthnotifier.fragment.InboxFragment;
import com.healthnotifier.healthnotifier.fragment.PatientFragment;
import com.healthnotifier.healthnotifier.fragment.RecentScansFragment;
import com.healthnotifier.healthnotifier.fragment.AccountFragment;
import com.healthnotifier.healthnotifier.fragment.PatientsFragment;
import com.healthnotifier.healthnotifier.fragment.ScanFragment;
import com.healthnotifier.healthnotifier.model.MainNavigationItem;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.healthnotifier.healthnotifier.utility.GenericEvent;

import com.squareup.otto.Bus;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.squareup.otto.Subscribe;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private RelativeLayout mDrawer;
    private Handler mHandler;
	private CharSequence mTitle;

	private MainNavigationAdapter mAdapter;
    private ArrayList<MainNavigationItem> mMainNavigationItems = new ArrayList<MainNavigationItem>();

    private void configureMainMenu(){
        // TODO: this is a static definition, we could move this somewhere else though
        // honestly we should ditch the calls to the R.string
        MainNavigationItem itemProfiles = new MainNavigationItem();
        itemProfiles.id = "profiles";
        itemProfiles.label = getText(R.string.menu_profiles).toString();
        itemProfiles.icon = R.drawable.ic_account_circle_black_24dp;

        MainNavigationItem itemScan = new MainNavigationItem();
        itemScan.id = "scan";
        itemScan.label = getText(R.string.menu_scan_lifesquare).toString();
        itemScan.icon = R.drawable.ic_logomark_white;

        MainNavigationItem itemSearch = new MainNavigationItem();
        itemSearch.id = "search";
        itemSearch.label = getText(R.string.menu_find_lifesquare).toString();
        itemSearch.icon = R.drawable.ic_location_searching_black_24dp;

        MainNavigationItem itemHistory = new MainNavigationItem();
        itemHistory.id = "history";
        itemHistory.label = getText(R.string.menu_recent_scans).toString();
        itemHistory.icon = R.drawable.ic_history_black_24dp;

        MainNavigationItem itemInbox = new MainNavigationItem();
        itemInbox.id = "inbox";
        itemInbox.label = getText(R.string.menu_inbox).toString();
        itemInbox.icon = R.drawable.ic_history_black_24dp; // TODO: icon

        MainNavigationItem itemSettings = new MainNavigationItem();
        itemSettings.id = "settings";
        itemSettings.label = getText(R.string.menu_settings).toString();
        itemSettings.icon = R.drawable.ic_settings_black_24dp; // TODO: icon

        // wipe the slate son
        mMainNavigationItems = new ArrayList<MainNavigationItem>();
        mMainNavigationItems.add(itemProfiles);
        mMainNavigationItems.add(itemScan);
        if(HealthNotifierApplication.preferences.getProvider()){
            mMainNavigationItems.add(itemSearch);
            mMainNavigationItems.add(itemHistory);
        }
        // TODO: only if we have unreads son / whatever
        // mMainNavigationItems.add(itemInbox);
        mMainNavigationItems.add(itemSettings);
    }

    private void renderMainMenu(){
        configureMainMenu();
        // not the job of the adaptor, just feed that shit a different list
        mAdapter = new MainNavigationAdapter(this, mMainNavigationItems);
        mDrawerList.setAdapter(mAdapter);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mHandler = new Handler(Looper.getMainLooper());

        // huh what? create this somewhere else, even do this is our entry point bro
        Fabric.with(this, new Crashlytics());
		setContentView(R.layout.activity_main);
        // bind it
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

		mTitle = getTitle();
        Preferences pref = HealthNotifierApplication.preferences;

        // given all the new material, hotness, this probably needs updating too
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer_list);
		mDrawer = (RelativeLayout) findViewById(R.id.left_drawer);

        renderMainMenu();

		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(
                this,
		        mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
		) {
            // we no longer visibly show the drawer nested below the menu, so changing the title to LifeSticker is moot
			public void onDrawerClosed(View view) {
                // getSupportActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				// getSupportActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.addDrawerListener(mDrawerToggle);

        // get on dat bus, back of the bus, #toomuch ?


        Intent intent = getIntent();
        Bundle bd = intent.getExtras();

        if(bd != null) {
            // this is why we need network calls happening in services and not in activities
            String action = (String) bd.getString("ACTION");
            if (action != null && action.equals("logout")){
                Toast.makeText(this, "Authentication Token Expired, please log in again", Toast.LENGTH_SHORT).show();
                Bus bus = HealthNotifierApplication.bus;
                bus.post(new GenericEvent("Logout"));
                return;
            }
            // this should really be handled in each and every activity network section, yea?
            if (action != null && action.equals("timeout")){
                // logoutUser();
                Toast.makeText(this, "Network timed out, please check your connection.", Toast.LENGTH_SHORT).show();;
                return;
            }
            if (action != null && action.equals("unknownhost")){
                // meh totally unsure of this state
                Bus bus = HealthNotifierApplication.bus;
                bus.post(new GenericEvent("Logout"));
                Toast.makeText(this, "Unknown host, HealthNotifier servers may be down for maintenance. Please contact support@domain.com.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (savedInstanceState == null) {
            // launch the first thingy
            // TODO: sort out the consistent savedInstanceState lol, so glad we have to do this manually, WTF ANDROID, weakass sauce
            selectItem(HealthNotifierApplication.mainActivityDrawerPosition);
        } else {
            selectItem(HealthNotifierApplication.mainActivityDrawerPosition);
        }

        HealthNotifierApplication.bus.register(this);

        HealthNotifierApplication.setMainActivity(this);

        // if we're a provider ask now, lolo and we can presumably bail out at this point, if Location Is Not Enable as provider
        // or we can lock those features
        // don't Cblock in a high-stakes scenario / such as an actual emergency scan
        // capture only upon app launch for providers
        // tease it out for non-providers as necessary, lol, which is basically never
        // we don't care about the status change of a registering provider at this point, although we should ask at that point too
        // since we're unlikely to logout and back in…
        if(HealthNotifierApplication.preferences.getProvider()){
            // defer slightly bro briz
            // should probably be able to do this bitch inline, but w/e
            Logcat.d("should ask I guess, as a provider");
            mHandler.removeCallbacks(delayedLocationPermissionCheck);
            mHandler.postDelayed(delayedLocationPermissionCheck, 1000);
            // if we wait too long they can navigate away brizzle, lol that would be unclutch
        }
	}

    Runnable delayedLocationPermissionCheck = new Runnable() {
        @Override
        public void run() {
            checkLocationPermissions();
        }
    };

    private void checkLocationPermissions(){
        // ironically enough, at the moment, since we're pre-empting the check, we don't need to handle the response
        // except maybe the deny response
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Logcat.d("Checking if we need to ask");
            String permission = Manifest.permission.ACCESS_COARSE_LOCATION;
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // do we need rationale
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    // we could drop into the current selected fragment and use da mRootView or something
                    final MainActivity tActivity = this;
                    // TODO: explain it, hmm and ask for it
                    Snackbar sb = Snackbar.make(findViewById(R.id.content_frame), "Provider use requires location services!", Snackbar.LENGTH_INDEFINITE);
                    sb.setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sb.dismiss();
                            ActivityCompat.requestPermissions(tActivity, new String[]{ permission }, HealthNotifierApplication.PERMISSION_LOCATION_CODE);
                        }
                    });
                    sb.show();

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{ permission }, HealthNotifierApplication.PERMISSION_LOCATION_CODE);
                }
            } else {
                if(HealthNotifierApplication.getCurrentLocation() != null){

                } else {
                    HealthNotifierApplication.enableLocationServices();
                }
            }
        } else {
            // if we're a legacy android os before 6 just get to steppin
            if(HealthNotifierApplication.getCurrentLocation() != null){

            } else {
                HealthNotifierApplication.enableLocationServices();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case HealthNotifierApplication.PERMISSION_LOCATION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    HealthNotifierApplication.enableLocationServices();
                } else {
                    // indefinite maybe? with button for more info / help / backrubs?
                    Snackbar.make(findViewById(R.id.content_frame), "Location specific features disabled!", Snackbar.LENGTH_LONG).show();
                }
                return;
            }
            case HealthNotifierApplication.PERMISSION_CAMERA_CODE: {
                // totally not sure who is asking for it, but it's 99% certain to the be the ScanFragment
                // FML
                try {
                    FragmentManager manager = getSupportFragmentManager();
                    ScanFragment fragment = (ScanFragment) manager.findFragmentById(R.id.content_frame);
                    fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
                } catch(Exception e){
                    Logcat.d("YOUR STEEZE BE SCREWED " + e.toString());
                }
            }
            // OMG it's being gobbled up though
        }

        // hot damn SO
        // http://stackoverflow.com/questions/32890702/request-runtime-permissions-from-v4-fragment-and-have-callback-go-to-fragment
        // forward down the chains until life sucks less
        /*
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
        */
    }



	@Override
    protected void onPause() {
        // MIGHT BE
        Bus bus = HealthNotifierApplication.bus;
        bus.post(new GenericEvent("onAppBackground"));
        super.onPause();
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
        // TODO: A better handling of this
        // when logging in and out between a provider and non-provider, we have a "saved" position that is no longer relevant
        // SO, we have to discard
        // A naive check would be if the position is out of bounds, default to 0 which is the "Profiles"
        if(position > mMainNavigationItems.size() - 1){
            position = 0;
        }
        // END of twerk town

		if (mAdapter != null) {
			mAdapter.setSelectedPosition(position);
            HealthNotifierApplication.mainActivityDrawerPosition = position;
		}

        Preferences pref = HealthNotifierApplication.preferences;
        Fragment fragment = null;

        // blablabla, probably could have just cast the selectedValue or something whatever
        MainNavigationItem selectedItem = mMainNavigationItems.get(position);
        switch (selectedItem.id){
            case "profiles":
                fragment = new PatientsFragment();
                break;
            case "scan":
                fragment = new ScanFragment();
                break;
            case "search":
                if(pref.getProvider()) {
                    fragment = new FindLifesquaresFragment();
                }
                break;
            case "history":
                // why not, regular users could totally do this
                fragment = new RecentScansFragment();
                break;
            case "inbox":
                fragment = new InboxFragment();
                break;
            case "settings":
                fragment = new AccountFragment();
                break;
        }

		if(fragment != null){
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, fragment.getClass().getSimpleName())
					.commit();

			mDrawerList.setItemChecked(position, true);
            // TODO: get yourself that override title though because yea son
			setTitle(selectedItem.label); // CharSequence from string though? not necessary IS-A
		}
		mDrawerLayout.closeDrawer(mDrawer);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

    @Override
    protected void onRestart() {
        super.onRestart();
        Bus bus = HealthNotifierApplication.bus;
        bus.post(new GenericEvent("onAppForeground"));
    }

	@Override
	protected void onStop() {
		super.onStop();
        Bus bus = HealthNotifierApplication.bus;
        bus.post(new GenericEvent("onAppBackground"));

	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logcat.d("main.onDestroy");
        HealthNotifierApplication.bus.unregister(this);
    }

    @Subscribe
    public void handleGenericEvent(GenericEvent event) {
        if (event.eventName.equals("onFetchUser")) {
            renderMainMenu();
        }
        // iOS port of OpenTab - presumably this could live in da list adapter doe.
        if (event.eventName.equals("OpenMainFragment")) {
            /*
            // check da id attribute, and scrub it down through the list adaptor to re-issue da command doe
            // iterate the current items doe, so many ways to do it doe
            for(int i=0; i < mMainNavigationItems.size(); i++){
                try {
                    if (mMainNavigationItems.get(i).id.equals(event.attributes.get("id").toString())) {
                        mAdapter.setSelectedPosition(i);
                        HealthNotifierApplication.mainActivityDrawerPosition = i;
                        break;
                    }
                } catch (Exception e){
                    // FML
                    Logcat.d(e.toString());
                }
            }
            */
        }
    }

}
