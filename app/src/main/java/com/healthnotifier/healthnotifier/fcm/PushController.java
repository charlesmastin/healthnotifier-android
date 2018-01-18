package com.healthnotifier.healthnotifier.fcm;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.firebase.messaging.RemoteMessage.Notification;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.activity.LifesquareActivity;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Bus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by charles on 3/20/17.
 */

public class PushController extends Object {

    private Handler mHandler;
    // basically a port from the iOS side…
    // this is a single class in order to DRY up the background and foreground processing
    // basically a router for doing the work

    // store a reference to the notification
    // store a reference to the data
    public Map<String, String> data;
    public Notification notification;
    // for our intent (aka transitioning interaction)
    public Intent intent; // we're gonna have to reassemble our stuffs
    // we don't get the top level data attribute, but all the pieces chucked in, thanks FCM API team

    // a forward from the MessagingService.onMessageReceived
    public void handleForeground(Context context){

        // LOL BRO
        mHandler = new Handler(Looper.getMainLooper());

        // context of foreground / background doh
        // we know this is like 99% for show, since nobody just sits there with the app open
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); out of activity context though
        if(data.get("event") != null){
            String event = data.get("event");
            switch(event) {
                case "test":
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (notification != null && notification.getBody() != null) {
                                Toast.makeText(context, notification.getBody().toString(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    break;
                case "patient-network-request":
                    // sync
                    // TODO: notify - but in snackbar or alert, with option to proceed… mmkay
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (notification != null && notification.getBody() != null) {
                                Toast.makeText(context, notification.getBody().toString(), Toast.LENGTH_LONG).show();
                            }
                            Bus bus = HealthNotifierApplication.bus;
                            Map<String, Object> attributes = new HashMap<String, Object>();
                            attributes.put("PatientId", data.get("granter_uuid"));
                            bus.post(new GenericEvent("FetchPatient", attributes));
                        }
                    });
                    // direct towards patient
                    break;
                case "patient-network-granted":
                    // sync
                    // notify - in da snackbar doe, but hard to say how we're going to get a reference to the target view
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (notification != null && notification.getBody() != null) {
                                Toast.makeText(context, notification.getBody().toString(), Toast.LENGTH_LONG).show();
                            }
                            Bus bus = HealthNotifierApplication.bus;
                            Map<String, Object> attributes = new HashMap<String, Object>();
                            attributes.put("PatientId", data.get("auditor_uuid"));
                            bus.post(new GenericEvent("FetchPatient", attributes));
                        }
                    });
                    // option to view
                    break;
                case "patient-network-revoked":
                    // sync
                    //
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (notification != null && notification.getBody() != null) {
                                Toast.makeText(context, notification.getBody().toString(), Toast.LENGTH_LONG).show();
                            }
                            Bus bus = HealthNotifierApplication.bus;
                            Map<String, Object> attributes = new HashMap<String, Object>();
                            attributes.put("PatientId", data.get("auditor_uuid"));
                            bus.post(new GenericEvent("FetchPatient", attributes));
                        }
                    });
                    break;
                case "provider-status":
                    // account sync, aka LifesquarePreferences
                    // should re-init tabs here
                    // notify
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (notification != null && notification.getBody() != null) {
                                Toast.makeText(context, notification.getBody().toString(), Toast.LENGTH_LONG).show();
                                Bus bus = HealthNotifierApplication.bus;
                                bus.post(new GenericEvent("FetchUser"));
                            }
                        }
                    });
                    break;
                case "postscan":
                    // notify
                    // option to view
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (notification != null && notification.getBody() != null) {
                                Toast.makeText(context, notification.getBody().toString(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }

    // a forward from the main/launcher activity (LoginActivity) intent to launch w/ extras
    public void handleTransitioning(Context context){
        // TODO: handle dem context doh
        if(intent != null && intent.hasExtra("event")){
            String event = intent.getStringExtra("event");
            switch(event) {
                case "test":
                    Logcat.d("test event in transitioning though");
                    break;
                case "patient-network-request": {
                    // reload patient in question
                    // direct towards patient view
                    Intent new_intent = new Intent(context, PatientActivity.class);
                    new_intent.putExtra("PATIENT_ID", intent.getStringExtra("granter_uuid").toString());
                    context.startActivity(new_intent);
                    break;
                }
                case "patient-network-granted": {
                    // reload patient in question
                    // direct towards patient view
                    Logcat.d("YEA SON");
                    Intent new_intent = new Intent(context, PatientActivity.class);
                    new_intent.putExtra("PATIENT_ID", intent.getStringExtra("auditor_uuid").toString());
                    context.startActivity(new_intent);
                    break;
                }
                case "patient-network-revoked": {
                    // reload patient in question
                    Bus bus = HealthNotifierApplication.bus;
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put("PatientId", data.get("auditor_uuid"));
                    bus.post(new GenericEvent("FetchPatient", attributes));
                    // sad face
                    break;
                }
                case "provider-status": {
                    // account sync
                    Bus bus = HealthNotifierApplication.bus;
                    bus.post(new GenericEvent("FetchUser"));
                    break;
                }
                case "postscan": {
                    // load dem lifesquare
                    Intent new_intent = new Intent(context, LifesquareActivity.class);
                    new_intent.putExtra("PATIENT_NAME", "");
                    new_intent.putExtra("PATIENT_ID", intent.getStringExtra("patient_uuid").toString());
                    String url = Config.API_ROOT + "lifesquares/" + intent.getStringExtra("lifesquare").toString() + "/webview";
                    new_intent.putExtra("PATIENT_WEBVIEW_URL", url);
                    context.startActivity(new_intent);
                    break;
                }
                default:
                    break;
            }
        } else {
            // it's basically a future schema, or a generic message with no data payload
        }
    }

    // for pure data background notifications
    // handle background
    public void handleBackground(){

    }

}
