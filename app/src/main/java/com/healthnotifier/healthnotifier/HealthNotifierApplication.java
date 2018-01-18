package com.healthnotifier.healthnotifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.iid.FirebaseInstanceId;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.otto.ThreadEnforcer;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import io.keen.client.android.AndroidKeenClientBuilder;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenProject;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.Call;
import okhttp3.Callback;

import com.healthnotifier.healthnotifier.activity.MainActivity;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.MemoryBoss;
import com.healthnotifier.healthnotifier.activity.LoginActivity;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.healthnotifier.healthnotifier.BuildConfig;



import org.json.JSONObject;

public class HealthNotifierApplication extends Application implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	private static HealthNotifierApplication mInstance;
    // FML
    private static MainActivity mActivity;

    // donkey down town
    private Handler mHandler;
    public static Preferences preferences;
    private static Location mLastLocation;
    protected static GoogleApiClient mGoogleApiClient;

    public HealthNotifierApplication() {
		mInstance = this;
	}

    MemoryBoss mMemoryBoss;

    // DANGEROUS PERMISSSIONS SON
    // TODO: move these to the helper class
    public static final int PERMISSION_CAMERA_CODE = 89;
    public static final int PERMISSION_LOCATION_CODE = 86;
    public static final int PERMISSION_READ_EXTERNAL_STORAGE_CODE = 420;
    // donkey down town

    // interesting hack
    public static int mainActivityDrawerPosition = 0;

	public static Bus bus = new Bus(ThreadEnforcer.ANY);

    private Timer mopupTaskTimer;

	@Override
	public void onCreate() {
		super.onCreate();

        preferences = new Preferences();

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

		// ActiveAndroid.initialize(this);

        // TODO: WTF is this?
		// force AsyncTask to be initialized in the main thread due to the bug:
		// http://stackoverflow.com/questions/4280330/onpostexecute-not-being-called-in-asynctask-handler-runtime-exception
		try {
			Class.forName("android.os.AsyncTask");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        Realm.init(this);
		RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name("healthnotifier2018")
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build();
		Realm.setDefaultConfiguration(realmConfiguration);

        // if we need migrations and we don't have them, zap branigan the heck out of it
        // Realm.deleteRealm(realmConfiguration);

        // TODO: pass the headers HealthNotifier-Client-Version and HealthNotifier-Client-Version-Name on the loader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
		.threadPoolSize(3)
		.threadPriority(Thread.NORM_PRIORITY - 2)
		.defaultDisplayImageOptions(DisplayImageOptions.createSimple())
		.build();

		ImageLoader.getInstance().init(config);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mMemoryBoss = new MemoryBoss();
            registerComponentCallbacks(mMemoryBoss);
        }

        mHandler = new Handler(Looper.getMainLooper());

        HealthNotifierAPI.getInstance().setClientVersionBuild(String.valueOf(versionCode));
        HealthNotifierAPI.getInstance().setClientVersionName(versionName);

        // TODO: create a custom ImageLoader class / etc, and config the headers here? vs each implementation

        if (!KeenClient.isInitialized()) {

            KeenClient client = new AndroidKeenClientBuilder(this).build();

            String projectId, writeKey;

            if (Config.API_ROOT.equals(Config.RELEASE_API_ROOT)) {
                // Production
                projectId = BuildConfig.LSQ_RELEASE_KEEN_PROJECT_ID;
                writeKey = BuildConfig.LSQ_RELEASE_KEEN_WRITE_KEY;
            } else {
                // DEV
                projectId = BuildConfig.LSQ_DEBUG_KEEN_PROJECT_ID;
                writeKey = BuildConfig.LSQ_DEBUG_KEEN_PROJECT_ID;
            }

            KeenProject project = new KeenProject(projectId, writeKey, null);
            client.setDefaultProject(project);

            // KeenLogging.enableLogging();
            // client.setDebugMode(true);

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("client_build", versionCode);
            map.put("client_version", versionName);
            map.put("user_agent", "${keen.user_agent}");
            map.put("ip_address", "${keen.ip}");
            client.setGlobalProperties(map);

            /*
            GlobalPropertiesEvaluator evaluator = new GlobalPropertiesEvaluator() {
                public Map<String, Object> getGlobalProperties(String eventCollection) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("user_agent", "${keen.user_agent}");
                    return map;
                }
            };
            client.setGlobalPropertiesEvaluator(evaluator);
            */

            KeenClient.initialize(client);

            buildGoogleApiClient();
        }

        HealthNotifierAPI.getInstance().loadValues();

        bus.register(this);

    }

	public static Context getContext() {
		return mInstance;
	}

    private void startMopping(){
        mopupTaskTimer = new Timer();
        mopupTaskTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                expireScanHistory();
            }
        }, 0, 5000);
    }

	private void expireScanHistory() {

	}

    // FML
    public static void setMainActivity(MainActivity a){
        mActivity = (MainActivity) a;
    }

    // FML
    public static MainActivity getMainActivity(){
        return mActivity;
    }

	@Override
	public void onTerminate() {
		super.onTerminate();
        unregisterComponentCallbacks(mMemoryBoss);
        // ActiveAndroid.dispose();
	}

    // ok this is for auto login ONLY
    public static void persistUser(JSONObject response){
        // this is so disturbing
        try {
            Logcat.d(response.toString());
            Preferences pref = HealthNotifierApplication.preferences;
            pref.setAccountId(response.getString("AccountId"));
            //
            // other crap
            // be ok with setting null, better be
            pref.setMobilePhone(response.getString("MobilePhone"));

            //
            pref.setEmail(response.getString("Email"));
            pref.setProvider(response.getBoolean("Provider"));
            pref.setProviderCredentialStatus(response.getString("ProviderCredentialStatus"));
            // permissions, lol retired bro
            // names, forget about it, meaningless

            // is it safe to read immediately???
            // HealthNotifierAPI.getInstance().setAuth(pref.getEmail(), pref.getAuthToken());
            HealthNotifierApplication.persistDeviceToken();

        } catch(Exception e){
            Logcat.d("we're totally sauced");
            // please just crash the app now
        }
    }

    public static void persistDeviceToken(){
        // at this point it is safe to add / update the device brizzle
        String token = FirebaseInstanceId.getInstance().getToken();
        if(token != null && !token.equals("")){
            // TODO: error handling in here, meh meh meh
            HealthNotifierAPI.getInstance().addDeviceToken(token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {

                }
            });
        } else {
            Logcat.d("was unable to get token when persisting, hmm, problematic");
        }

    }

    public void loginUser(String email, String password){

        /*
        HealthNotifierAPI.getInstance().login(email, password, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // send back a failure event
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        JSONObject loginResponse = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    persistUser(loginResponse);
                                    Intent intent = new Intent(getContext(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } catch(Exception e){
                                    // JSON access is such a drag son
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
                        }
                    });
                }
            }
        });
        */
        // use events going back out for any particular handling
        // ok, just don't worry about failed network here
        // networking
        // then persist into our storage
        // then store in our API for future requests son
    }

    public void logoutUserOnServer(){
        HealthNotifierAPI.getInstance().logout(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // get the current activity and view bro
                // send a snackbar up in that bitch
                // or maybe just a toast
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    // jump threads bitch
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Preferences pref = HealthNotifierApplication.preferences;
                            Bus bus = HealthNotifierApplication.bus;
                            Map<String, Object> event = new HashMap<String, Object>();
                            event.put("AccountId", pref.getAccountId());
                            event.put("Provider", pref.getProvider());
                            bus.post(new AnalyticsEvent("Logout", event));

                            // TODO: remove all the things!
                            pref.setAuthToken(null);
                            HealthNotifierAPI.getInstance().removeAcessToken();
                            // this is enough for now though

                            Intent intent = new Intent(getContext(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
                } else {
                    // well not much we should do here
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }
            }
        });

    }

    public void fetchUser(){
        // call the get, persist, and den ensure main activity done be re-laying itself out
        // use some form of notification
        // change up the menu build in main activity to be based on resume too… since I believe it's singletop
        String account_id = HealthNotifierApplication.preferences.getAccountId();
        HealthNotifierAPI.getInstance().getAccount(account_id, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // send back a failure event
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        JSONObject loginResponse = new JSONObject(response.body().string());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    persistUser(loginResponse);
                                    // broadcast son
                                    Bus bus = HealthNotifierApplication.bus;
                                    bus.post(new GenericEvent("onFetchUser"));
                                } catch(Exception e){
                                    // JSON access is such a drag son
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
                        }
                    });
                }
            }
        });
    }

    // PLAY SERVICES STUFFS
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Logcat.d("Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause){
        Logcat.d("Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // http://stackoverflow.com/questions/34531867/android-request-permission-fused-location-api-return-todo-cannot-resolve
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                // broadcast brizzle
                Bus bus = HealthNotifierApplication.bus;
                Map<String, Object> attributes = new HashMap<String, Object>();
                attributes.put("Latitude", mLastLocation.getLatitude());
                attributes.put("Longitude", mLastLocation.getLongitude());
                bus.post(new GenericEvent("onLocationUpdate", attributes));
            } else {
                // no loco for you, lol
            }
        } catch(SecurityException e){

        }
    }

    public static void enableLocationServices(){
        mGoogleApiClient.connect();
    }

    public static void disableLocationServices(){
        // spin it down though
        mGoogleApiClient.disconnect();
    }

    public static Location getCurrentLocation(){
        // http://stackoverflow.com/questions/34531867/android-request-permission-fused-location-api-return-todo-cannot-resolve
        if(mLastLocation != null && mGoogleApiClient.isConnected()){
            // do an in place update and fetch it again synchronously
            try {
                Location tLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (tLastLocation != null) {
                    mLastLocation = tLastLocation;
                }
                return mLastLocation;
            } catch(SecurityException e){
                return null;
            }
        } else if (mLastLocation!= null){
            return mLastLocation;
        } else {
            return null;
        }
    }

    // on start (for providers)
    // TODO: do this only for providers, any only as needed, for privacy sake
    // mGoogleApiClient.connect();

    // on restart
    // mGoogleApiClient.connect();

    // on stop
    /*
    if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
     */

    public void logoutUserOnDevice(){
        Logcat.d("logoutUserOnDevice");
        // TODO: wipe out scanHistory, yea bro bra
        // new Delete().from(RecentLifesquare.class).execute();
        // actually we want to go back to the main activity though, bro
        // TODO: check from fresh install, this is likely gonna crash
        preferences.clearPreferences();
        // zero the networking library too
        // anything that has reference
        // a little over kill though
    }

    @Subscribe public void handleGenericEvent(GenericEvent event) {
        // Logcat.d("Application.handleGenericEvent(" + event.eventName +")");
        // LOGOUT BRO
        if(event.eventName.equals("Logout")) {
            logoutUserOnServer();
            // order matters here
            logoutUserOnDevice();
        }

        if(event.eventName.equals("onDeauthorized")){
            logoutUserOnDevice();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        if(event.eventName.equals("FetchUser")) {
            fetchUser();
        }

        if(event.eventName.equals("AutoLogin")) {
            loginUser(event.attributes.get("Email").toString(),
                    event.attributes.get("Password").toString());
        }

        if(event.eventName.equals("onAppBackground")){
            Logcat.d("LSQ.APP Background");
            KeenClient.client().sendQueuedEventsAsync();
        }
        if(event.eventName.equals("onAppForeground")){
            Logcat.d("LSQ.APP Foreground");
        }

        // handle all the network fails, which basically result in going offline and then sending back to the login, yikes.
        if(event.eventName.equals("API.onCancelled")){

        }
        if(event.eventName.equals("API.onSocketTimeout")){
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("ACTION", "timeout");
            startActivity(intent);
        }
        if(event.eventName.equals("API.onSocketException")){
            /*
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("ACTION", "timeout");
            startActivity(intent);
            */
        }
        if(event.eventName.equals("onTokenExpired")) {
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("ACTION", "logout");
            startActivity(intent);
        }
        if(event.eventName.equals("API.onUnknownHost")){
            /*
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("ACTION", "unknownhost");
            startActivity(intent);
            */
        }
    }

    // this is where we hack our analytics consumer on, maybe a bad spot
    @Subscribe public void handleAnalytics(AnalyticsEvent event) {
        // we could put any sort of vendor up in here, our very own segment.io

        Map<String, Object> keenProperties = new HashMap<String, Object>();
        List<Object> addons = new ArrayList<Object>();
        Map<String, Object> uaParser = new HashMap<String, Object>();
        uaParser.put("name", "keen:ua_parser");
        Map<String, Object> ip = new HashMap<String, Object>();
        ip.put("ua_string", "user_agent");
        uaParser.put("input", ip);
        uaParser.put("output", "parsed_user_agent");
        addons.add(uaParser);

        Map<String, Object> ipToGeo = new HashMap<String, Object>();
        ipToGeo.put("name", "keen:ip_to_geo");
        Map<String, Object> ipToGeoInput = new HashMap<String, Object>();
        ipToGeoInput.put("ip", "ip_address");
        ipToGeo.put("input", ipToGeoInput);
        ipToGeo.put("output", "ip_geo_info");
        addons.add(ipToGeo);

        keenProperties.put("addons", addons);

        Map<String, Object> attributes = event.attributes;
        if(attributes == null || attributes.size() == 0){
            attributes = new HashMap<String, Object>();
        }

        // naturally we only do these if we have AuthToken, based on logging analytics when logged out (login, registration, restore accounts)
        if(preferences.getAuthToken() != null) {
            if (!attributes.containsKey("AccountId")) {
                attributes.put("AccountId", preferences.getAccountId());
            }
            if (!attributes.containsKey("Provider")) {
                attributes.put("Provider", preferences.getProvider());
            }
            if (!attributes.containsKey("PatientId")) {
                attributes.put("PatientId", preferences.getCurrentPatientId());
            }
        }

        // this is a no attribute non-auth event
        // Keen requires a single attribute, or at least it used to
        // this is a special homage to Localytics
        if(attributes.size() == 0){
            attributes.put("PooClick", true);
        }

        KeenClient.client().queueEvent(event.eventName, attributes, keenProperties);
    }


}