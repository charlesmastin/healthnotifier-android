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
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.healthnotifier.healthnotifier.activity.EditProfileActivity;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.realm.Realm;
import okhttp3.Call;
import okhttp3.Callback;

import static com.healthnotifier.healthnotifier.HealthNotifierApplication.bus;


/**
 * Created by charles on 10/4/17.
 */
public class CaptureLicenseFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;

    private CompoundBarcodeView barcodeView;
    private Menu mMenu;
    private Boolean scanBlockSpf40 = false;
    private Boolean mAuthorized = false;

    private Realm realm;

    // meh
    public Boolean mScanOnCapture = false; // if true, will look up the patient via the scan API and send the intent on
    // if false, will send a capture intent only, but this is more in the parent activity, lol bro


    private BarcodeCallback callback = new BarcodeCallback() {

        // TODO: setup for PDF417 spec though
        // https://github.com/journeyapps/zxing-android-embedded/issues/142

        @Override
        public void barcodeResult(BarcodeResult result) {
            // validate formats though bro
            //
            // basically send anything onwards
            String scanString = result.getText();
            // meh check for some basic US license specifics and ensure it's legit(ish)
            Boolean valid = false;
            if (result.getText() != null) {
                // don't go too crazy now
                // TODO: robust this a bit, case sensitivity??
                if(scanString.toUpperCase().contains("ANSI") && scanString.toUpperCase().contains("DL")){
                    valid = true;
                }
            }

            if(valid){

                // do those things
                scanBlockSpf40 = true;
                barcodeView.pause();

                // TODO: determine calling intent more intenlligently…meh
                Intent intent = new Intent(getContext(), EditProfileActivity.class);
                intent.putExtra("EVENT", "LicenseScan");
                intent.putExtra("CONTENT", result.getText());
                startActivity(intent);

            } else {
                scanBlockSpf40 = true;
                barcodeView.pause();
                Snackbar sb = Snackbar.make(mRootView, "Invalid QR Code. Not a valid U.S. License", Snackbar.LENGTH_INDEFINITE);
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
        mRootView = inflater.inflate(R.layout.fragment_scan_license, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barcodeView = (CompoundBarcodeView) mRootView.findViewById(R.id.barcode_scanner);
        // oh hell yes
        String characterSet = Intents.Scan.CHARACTER_SET;
        Set<BarcodeFormat> decodeFormats = new HashSet<BarcodeFormat>();
        decodeFormats.add(BarcodeFormat.PDF_417);
        Map<DecodeHintType, Object> decodeHints = new HashMap<>();
        decodeHints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        decodeHints.put(DecodeHintType.CHARACTER_SET, characterSet);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(decodeFormats, decodeHints, characterSet, false));

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

    /*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_scan, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }
    */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
                //Intent intent = new Intent(getContext(), CheckoutActivity.class);
                //intent.putExtra("CAPTURED_LIFESQUARE_CODE", ""); // basically use this as our null "cancel" action, vs sending a discreet extra
                //startActivity(intent);
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
