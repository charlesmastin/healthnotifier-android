package com.healthnotifier.healthnotifier.fragment;
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.CheckoutActivity;
import com.healthnotifier.healthnotifier.activity.LifesquareActivity;
import com.healthnotifier.healthnotifier.model.Lifesquare;
import com.healthnotifier.healthnotifier.model.ScanHistory;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;
import com.healthnotifier.healthnotifier.utility.Validators;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;
import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import okhttp3.Call;
import okhttp3.Callback;

import static com.healthnotifier.healthnotifier.HealthNotifierApplication.bus;


/**
 * Created by charles on 4/14/16.
 */
public class ScanFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;

    private CompoundBarcodeView barcodeView;
    private Menu mMenu;
    private Boolean scanBlockSpf40 = false;
    private Boolean mAuthorized = false;

    private Realm realm;

    // meh
    public Boolean mScanOnCapture = true; // if true, will look up the patient via the scan API and send the intent on
    // if false, will send a capture intent only, but this is more in the parent activity, lol bro

    private void logLifesquareScan(JSONObject item){
        Lifesquare lifesquare = new Lifesquare();
        lifesquare.setNode(item);
        ScanHistory historyItem = new ScanHistory();
        historyItem.name = lifesquare.getName();
        historyItem.lifesquareId = lifesquare.getLifesquareId();
        historyItem.patientId = lifesquare.getPatientId();
        historyItem.address = lifesquare.getFormattedAddress();
        historyItem.lifesquareLocation = lifesquare.getLifesquareLocation();
        historyItem.profilePhoto = lifesquare.getProfilePhoto();
        historyItem.createdAt = new Date();
        realm.beginTransaction();
        realm.copyToRealm(historyItem);
        realm.commitTransaction();
    }

    private void handleCapturedCode(String code, String mode){
        // meh

        if(mScanOnCapture){
            // OG behavior
            // interact with API
            // let's just do it up in here
            Double latitude = null;
            Double longitude = null;
            if(HealthNotifierApplication.getCurrentLocation() != null){
                latitude = HealthNotifierApplication.getCurrentLocation().getLatitude();
                longitude = HealthNotifierApplication.getCurrentLocation().getLongitude();
            }
            HealthNotifierAPI.getInstance().getLifesquare(code, latitude, longitude, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Logcat.d( "onFailure" + e.toString());
                            // Toast.makeText(getActivity(), "Server Offline" + e.toString(), Toast.LENGTH_LONG).show();
                            Snackbar.make(mRootView, "Server Offline", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        try {
                            JSONObject scanResult = new JSONObject(response.body().string());
                            // mehmeh mhe


                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // bacon wrap your exceptions
                                    try {
                                        Bus bus = HealthNotifierApplication.bus;

                                        logLifesquareScan(scanResult);

                                        scanBlockSpf40 = false;

                                        Preferences pref = HealthNotifierApplication.preferences;

                                        Map<String, Object> event = new HashMap<String, Object>();
                                        // this one in for iOS compat
                                        event.put("Lifesquare", scanResult.getString("LifesquareId"));
                                        // this is the new identifier, since there is no "active" patient scanning in the context
                                        event.put("PatientId", scanResult.getString("PatientId"));
                                        String eventName = "Scan";
                                        if (mode.equals("Code"))
                                            eventName = "Code Entry";
                                        bus.post(new AnalyticsEvent(eventName, event));

                                        Intent intent = new Intent(getContext(), LifesquareActivity.class);
                                        intent.putExtra("PATIENT_ID", scanResult.getString("PatientId"));
                                        intent.putExtra("PATIENT_NAME", scanResult.getString("FirstName") + " " + scanResult.getString("LastName"));
                                        intent.putExtra("PATIENT_WEBVIEW_URL", scanResult.getString("WebviewUrl"));
                                        startActivity(intent);


                                    }catch(Exception e){
                                        Logcat.d(e.toString());
                                    }
                                }
                            });

                        } catch(Exception e){
                            Logcat.d(e.toString());
                        }

                    } else {
                        // well not much we should do here

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar sb = Snackbar.make(mRootView, "Invalid LifeSticker", Snackbar.LENGTH_INDEFINITE);
                                // Just testing it out though
                                sb.setAction("Dismiss", new View.OnClickListener(){
                                    @Override
                                    public void onClick(View v){
                                        sb.dismiss();
                                        scanBlockSpf40 = false;
                                        onResume();
                                    }
                                });
                                sb.show();
                                // launch the intent.. if we even need to jump dem threads?
                            }
                        });
                    }
                }
            });
        } else {
            // capture only mode
            // somehow return to calling activity with the captured code bro
            // currently only da Checkout Activity is supported bro
            Intent intent = new Intent(getContext(), CheckoutActivity.class);
            intent.putExtra("CAPTURED_LIFESQUARE_CODE", code);
            startActivity(intent); // may work may not
        }
        /*
        Bus bus = HealthNotifierApplication.bus;
        Map<String, Object> e = new HashMap<String, Object>();
        e.put("Code", code);
        // or code entry
        if(mode.equals("Scan")) {
            bus.post(new GenericEvent("Scan", e));
        }
        if(mode.equals("CodeEntry")) {
            bus.post(new GenericEvent("CodeEntry", e));
        }
        if(mode.equals("RFID")) {
            bus.post(new GenericEvent("RFIDCapture", e));
        }
        */
    }

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() != null) {
                String scanString = result.getText().toUpperCase();
                if (scanString.contains("LSQR.NET")) {
                    scanBlockSpf40 = true;
                    barcodeView.pause();
                    String lifesquareId = scanString.substring(scanString.lastIndexOf("/") + 1, scanString.length());
                    handleCapturedCode(lifesquareId, "Scan");
                } else {
                    scanBlockSpf40 = true;
                    barcodeView.pause();
                    Snackbar sb = Snackbar.make(mRootView, "Invalid QR Code. Not a LifeSticker", Snackbar.LENGTH_INDEFINITE);
                    sb.setAction("Dismiss", new View.OnClickListener(){
                        @Override
                        public void onClick(View v){
                            sb.dismiss();
                            scanBlockSpf40 = false;
                            onResume();
                        }
                    });
                    sb.show();
                }
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mHandler = new Handler(Looper.getMainLooper());
        // TODO: REALM LIFECYCLE
        realm = Realm.getDefaultInstance(); // or use another instance son
        // setRetainInstance(true);
    }

    // handle dem permission requests son buns
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Logcat.d("DA F");
        switch (requestCode) {
            case HealthNotifierApplication.PERMISSION_CAMERA_CODE: {
                Logcat.d("WARMER");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    beginAuthorizedScanning();


                    // oh hell, just brute forget this sucker and relaunch this activity
                    // ok, get creative…aka, send a generic event, that MainActivity listens for that re-launches this fragment
                    // OMG so much ghetto sauce up in here doe
                    // permission was granted, yay!

                    // TODO: yea son,
                    /*
                    Logcat.d("POSTING DA MESSSSSAHE");
                    Map<String, Object> attributes = new HashMap<String, Object>();
                    attributes.put("id", "scan"); // corresponding to da
                    HealthNotifierApplication.bus.post(new GenericEvent("OpenMainFragment", attributes));
                    */
                    // same as iOS - open tab

                    /*

                    // attempt to jimmy force it…
                    barcodeView.pause();
                    barcodeView.resume();
                    */
                } else {
                    Snackbar.make(mRootView, "QR scanning disabled. Use \"Enter Code\" to view a LifeSticker.", Snackbar.LENGTH_LONG).show();
                    // Toast.makeText(getContext(), "QR scanning disabled. Use \"Enter Code\" to view a LifeSticker.", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_scan, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barcodeView = (CompoundBarcodeView) mRootView.findViewById(R.id.barcode_scanner);
        checkCameraPermissions();
    }

    private void checkCameraPermissions(){
        // Consider popping this in the document guy
        // SDK wrapper and permission but sonn
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // permission check though
            // TODO: DRY THIS UP SILICA GEL ON YOUR NUTS EDITION
            String permission = Manifest.permission.CAMERA;
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                // do we need rationale
                if (shouldShowRequestPermissionRationale(permission)) {
                    // TODO: explain it, hmm and ask for it
                    Snackbar sb = Snackbar.make(mRootView, "QR scanning requires the camera", Snackbar.LENGTH_INDEFINITE);
                    sb.setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sb.dismiss();
                            requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_CAMERA_CODE);
                            Logcat.d("REQUESTING DAT SHEEEE");
                        }
                    });
                    sb.show();
                    // Toast.makeText(getContext(), "QR scanning requires the camera", Toast.LENGTH_LONG);
                } else {
                    requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_CAMERA_CODE);
                }
            } else {
                beginAuthorizedScanning();
            }
        } else {
            // authorized on install
            // TODO: we should still check though
            beginAuthorizedScanning();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_scan, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(getContext(), CheckoutActivity.class);
                intent.putExtra("CAPTURED_LIFESQUARE_CODE", ""); // basically use this as our null "cancel" action, vs sending a discreet extra
                startActivity(intent);
                return true;
            case R.id.action_input_code:
                promptCode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void beginAuthorizedScanning(){
        mAuthorized = true;// potentially a misleading statement here though
        barcodeView.decodeContinuous(callback);
        // TODO: upon init of permission, the first scan was not responding
        // BUG
    }

    private void promptCode(){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_enter_code, null);
        dialogBuilder.setView(dialogView);

        // wire up that edit text son
        final EditText etLifesquareCode = (EditText) dialogView.findViewById(R.id.etLifesquareCode);

        // Nahh man
        // etLifesquareCode.setFilters(new InputFilter[] {new InputFilter.AllCaps()});


        // handle the IME.GO
        etLifesquareCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                                       @Override
                                                       public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                                           boolean handled = false;
                                                           if (actionId == EditorInfo.IME_ACTION_GO) {
                                                               String code = etLifesquareCode.getText().toString().toUpperCase();
                                                               handled = parseCode(code);
                                                           }
                                                           return handled;
                                                       }
                                                   });


        dialogBuilder.setTitle("Enter LifeSticker Code");

        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                // do your thing son
                String code = etLifesquareCode.getText().toString().toUpperCase();
                parseCode(code);
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int which){
                barcodeView.resume();
            }
        });

        AlertDialog b = dialogBuilder.create();
        b.show();

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        etLifesquareCode.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(etLifesquareCode, InputMethodManager.SHOW_IMPLICIT);
                    }
                }, 150);


        // pause the qr code viewing
        barcodeView.pause();

    }

    private boolean parseCode(String code){
        if(Validators.isValidLifesquare(code)){
            //Bus bus = HealthNotifierApplication.bus;
            //Map<String, Object> e = new HashMap<String, Object>();
            //e.put("Code", code);
            //bus.post(new GenericEvent("CodeEntry", e));
            //MainActivity activity = (MainActivity) getActivity();
            //activity.scanLifesquare(code, "Code");
            handleCapturedCode(code, "CodeEntry");

            return true;
        } else {
            Snackbar.make(mRootView, "Invalid LifeSticker Format", Snackbar.LENGTH_LONG).show();
            // Toast.makeText(getContext(), "Invalid LifeSticker Format", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public void onResume() {
        if(!scanBlockSpf40) {
            barcodeView.resume();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        barcodeView.pause();
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        bus.register(this);
        if(!scanBlockSpf40) {
            barcodeView.resume();
        }
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onStop() {
        super.onStop();
        bus.unregister(this);
        barcodeView.pause();
        realm.close();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        if(!scanBlockSpf40) {
            barcodeView.resume();
        }
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    // TODO: pass through stuffs like volume toggle so it does the torch lolzors™
    /*
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event);
    }
    */

}
