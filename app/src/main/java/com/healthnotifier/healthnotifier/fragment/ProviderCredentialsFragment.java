package com.healthnotifier.healthnotifier.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Validators;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProviderCredentialsFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;
    private Menu mMenu;

    // UI and data bindings
    private EditText licenseNumber;
    private EditText licenseBoard;
    private EditText licenseState;
    private String lState;
    private Date licenseExpiration;
    private EditText supervisorName;
    private EditText supervisorEmail;
    private EditText supervisorPhone;
    private EditText supervisorExt;

    private int targetPhoto = -1; // this is a lolzor hack
    private ImageView licensePhoto1;
    private ImageView licensePhoto2;
    private String licensePhoto1Data = null;
    private String licensePhoto2Data = null;

    private String photoFileName = "dd_temp_capture.jpg";

    private Date timerStart;

    // HACKTOWN USA™
    private Boolean mLocked = false;
    private Boolean jimmiesRustled = false;

    private AlertDialog stateDialog;

    private BottomSheetBehavior mBottomSheetBehavior;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new android.os.Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_provider_credentials, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Register as Provider");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        timerStart = new Date();
        if(mRootView != null) {


            // sheet slitz
            View bottomSheet = mRootView.findViewById(R.id.bsMediaCapture);
            mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

            bottomSheet.post(new Runnable() {
                @Override
                public void run() {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            });

            final Context context = getContext();
            // wire your sheet buttons son buns
            LinearLayout llActionCamera = (LinearLayout) mRootView.findViewById(R.id.llActionCamera);
            // TODO: prompt for dat permission
            llActionCamera.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    checkCameraPermissions();
                }
            });

            LinearLayout llActionUpload = (LinearLayout) mRootView.findViewById(R.id.llActionUpload);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                llActionUpload.setVisibility(View.GONE);
            } else {
                llActionUpload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkGalleryPermissions();
                    }
                });
            }

            TextView tvTitle = (TextView) mRootView.findViewById(R.id.tvSheetTitle);
            tvTitle.setText("Add License Document");


            licenseNumber = (EditText) mRootView.findViewById(R.id.etCredentialsNumber);
            licenseBoard = (EditText) mRootView.findViewById(R.id.etCredentialsBoard);
            licenseState = (EditText) mRootView.findViewById(R.id.etCredentialsState);
            supervisorName = (EditText) mRootView.findViewById(R.id.etCredentialsName);
            supervisorEmail = (EditText) mRootView.findViewById(R.id.etCredentialsEmail);
            supervisorPhone = (EditText) mRootView.findViewById(R.id.etCredentialsPhone);
            supervisorExt = (EditText) mRootView.findViewById(R.id.etCredentialsExtension);

            // bs focus hack town
            licenseNumber.setOnFocusChangeListener(focusListener);
            licenseBoard.setOnFocusChangeListener(focusListener);
            licenseState.setOnFocusChangeListener(focusListener);
            supervisorName.setOnFocusChangeListener(focusListener);
            supervisorPhone.setOnFocusChangeListener(focusListener);
            supervisorExt.setOnFocusChangeListener(focusListener);


            licensePhoto1 = (ImageView) mRootView.findViewById(R.id.licensePhoto1);
            licensePhoto2 = (ImageView) mRootView.findViewById(R.id.licensePhoto2);

            licensePhoto1.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    targetPhoto = 1;
                    if (!mLocked)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            checkCameraPermissions();
                        } else {
                            showBottomSheet();
                        }
                }
            });

            // TODO: better UX handling, but this works, we should show a confirm menu or some biz
            licensePhoto1.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    clearPhoto(1);
                    return true;
                }
            });

            licensePhoto2.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    targetPhoto = 2;
                    if (!mLocked)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            checkCameraPermissions();
                        } else {
                            showBottomSheet();
                        }
                }
            });

            // TODO: better UX handling, but this works, we should show a confirm menu or some biz
            licensePhoto2.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    clearPhoto(2);
                    return true;
                }
            });


            EditText et = (EditText) mRootView.findViewById(R.id.etExpiration);
            et.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExpirationPicker(v);
                }
            });

            licenseBoard.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        showStatePicker(v);
                        return true;
                    }
                    return false;
                }
            });

            licenseState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showStatePicker(v);
                }
            });

            licenseState.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        showExpirationPicker(v);
                        return true;
                    }
                    return false;
                }
            });


        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_provider_credentials, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void showExpirationPicker(View v) {
        CredentialsExpirationPickerFragment newFragment = new CredentialsExpirationPickerFragment();

        if(licenseExpiration != null) {
            Bundle bundle = new Bundle();
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            bundle.putString("INITIAL_DATE", formatter.format(licenseExpiration));
            newFragment.setArguments(bundle);
        }

        newFragment.show(getFragmentManager(), "datePicker");
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
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    // TODO: explain it, hmm and ask for it
                    Snackbar sb = Snackbar.make(mRootView, "Permission required to take your photo", Snackbar.LENGTH_INDEFINITE);
                    sb.setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sb.dismiss();
                            requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_CAMERA_CODE);
                        }
                    });
                    sb.show();
                } else {
                    requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_CAMERA_CODE);
                }
            } else {
                launchCamera();
            }
        } else {
            // authorized on install
            // TODO: we should still check though
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case HealthNotifierApplication.PERMISSION_CAMERA_CODE: {
                Logcat.d("camera something or other");
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Logcat.d("DONK STANK");
                    launchCamera();
                } else {
                    Snackbar.make(mRootView, "Photo upload disabled", Snackbar.LENGTH_LONG).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void checkGalleryPermissions(){
        // for now, we have no checks
        launchGallery();
    }

    private void launchGallery(){
        Intent takePictureIntent = new Intent();
        takePictureIntent.setType("image/jpeg");
        takePictureIntent.setAction(Intent.ACTION_GET_CONTENT);
        takePictureIntent.addCategory(Intent.CATEGORY_OPENABLE);
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Files.REQUEST_GALLERY);
        }
    }

    private void launchCamera(){
        // at this point ask for permission
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Files.getPhotoFileUri(getContext(), photoFileName));
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Files.REQUEST_IMAGE_CAPTURE);
        }
        /*
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Files.getPhotoFileUri(context, photoFileName));
                    if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, Files.REQUEST_IMAGE_CAPTURE);
                    }
         */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_submit:
                if (!mLocked)
                    save();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // blablablal
    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                jimmiesRustled = true;
            }
        }
    };

    private void clearPhoto(int id){
        // YES, I am really doing this, it works, and I have limited time. Thanks.
        // TODO: how to get to the menu_ic_camera in a different R namespace, LOLZORSREX™
        if(id == 1){
            licensePhoto1.setImageResource(R.drawable.ic_add_a_photo_black_48dp);
            // licensePhoto1.setImageResource(R.drawable.ic_add_a_photo_black_24dp);
            licensePhoto1Data = null;
        }
        if(id == 2){
            licensePhoto2.setImageResource(R.drawable.ic_add_a_photo_black_48dp);
            // licensePhoto2.setImageResource(R.drawable.ic_add_a_photo_black_24dp);
            licensePhoto2Data = null;
        }
        Snackbar.make(mRootView, "Photo Cleared", Snackbar.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mRootView.findViewById(R.id.bsMediaCapture).setVisibility(View.GONE);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        if (resultCode == getActivity().RESULT_OK){
            File imageFile = null;
            if (requestCode == Files.REQUEST_GALLERY){
                Uri photoUri = data.getData();
                if(photoUri != null){
                    String pathFromMediaStore = Files.getPathFromMediaUri(getContext(), photoUri);
                    if (pathFromMediaStore != null){
                        imageFile = new File(pathFromMediaStore);
                    }
                }
            }

            if (requestCode == Files.REQUEST_IMAGE_CAPTURE){
                Uri takenPhotoUri = Files.getPhotoFileUri(getContext(), photoFileName);
                imageFile = new File(takenPhotoUri.getPath());
            }

            if(imageFile != null && imageFile.exists()) {
                BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
                Bitmap rotated = Files.rotateBitmapOrientation(imageFile.getAbsolutePath());
                mBitmapOptions.inSampleSize = 16;
                Bitmap imageBitmap = null;
                ImageView tImageView = licensePhoto1;
                String tData = "";
                if(targetPhoto == 1) {
                    tImageView = licensePhoto1;
                } else {
                    tImageView = licensePhoto2;
                }
                if(Build.VERSION.SDK_INT >= 21) {
                    tImageView.setImageTintList(null);
                }
                if(rotated == null){
                    tData = Files.fileToString(imageFile);
                }else {
                    tData = Files.bitmapToString(rotated, Bitmap.CompressFormat.JPEG, Config.JPEG_COMPRESSION);
                }
                byte[] decodedString = Base64.decode(tData, Base64.DEFAULT);
                imageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, mBitmapOptions);
                tImageView.setImageBitmap(imageBitmap);
                // FML SON
                if(targetPhoto == 1) {
                    licensePhoto1Data = tData;
                } else {
                    licensePhoto2Data = tData;
                }
                imageFile.delete();
            }

        }

        if (resultCode == getActivity().RESULT_CANCELED){
            // Toast.makeText(this, "Cancelled the activity", Toast.LENGTH_SHORT).show();
        }
    }

    private void processLicenseImage(File imageFile){
        // DRY TIMES
    }

    private void forceKeyboard(){
        InputMethodManager inputManager = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }

    private void showBottomSheet(){
        mRootView.findViewById(R.id.bsMediaCapture).setVisibility(View.VISIBLE);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        // remove focus from edit text with this "hack"
        LinearLayout myLayout = (LinearLayout) getActivity().findViewById(R.id.llJW);
        myLayout.requestFocus();

        // slap that keyboard down as well, as an additional slap
        InputMethodManager inputManager = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(getView().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

        // THERE IS SOME CRAZY layout bug going on with the design support bottomsheet
        // after a keyboard has been shown, it's like the view chokes out
        // it looses it's top position, and without the wrapper layout, lost it's sides
        if(jimmiesRustled){
            LinearLayout slapWrap = (LinearLayout) mRootView.findViewById(R.id.jimmySlaps);
            final float scale = getContext().getResources().getDisplayMetrics().density;
            int t = (int) (-42 * scale * 0.5f);
            slapWrap.setPadding(0, t, 0, 0);
        }
    }

    private void hideBottomSheet(){
        // lolzy
        if(mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void showStatePicker(View v){
        CredentialsStatePickerFragment newFragment = new CredentialsStatePickerFragment();

        if(lState != null) {
            Bundle bundle = new Bundle();
            bundle.putString("INITIAL_STATE", lState);
            newFragment.setArguments(bundle);
        }

        newFragment.show(getFragmentManager(), "statePicker");

    }

    private void save(){
        if(mLocked){
            return;
        }
        if(isValid()){
            mLocked = true;
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            mMenu.findItem(R.id.action_submit).setEnabled(false);
            final Snackbar snackProgress = Snackbar.make(mRootView, "Saving credentials…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();
            // mMenu action_submit
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            String formattedLicenseExpiration = formatter.format(licenseExpiration);

            try {
                JSONObject payload = new JSONObject();
                payload.put("LicenseNumber", licenseNumber.getText().toString());
                payload.put("LicenseBoard", licenseBoard.getText().toString());
                payload.put("State", licenseState.getText().toString());
                payload.put("Expiration", formattedLicenseExpiration);
                payload.put("SupervisorName", supervisorName.getText().toString());
                payload.put("SupervisorEmail", supervisorEmail.getText().toString());
                payload.put("SupervisorPhone", supervisorPhone.getText().toString());
                payload.put("SupervisorExt", supervisorExt.getText().toString());
                JSONArray files = new JSONArray();

                if(licensePhoto1Data != null){
                    JSONObject f = new JSONObject();
                    f.put("File", licensePhoto1Data);
                    f.put("Name", "credentials-captured-1.jpg");
                    f.put("Mimetype", "image/jpg");
                    files.put(f);
                }
                if(licensePhoto2Data != null){
                    JSONObject f = new JSONObject();
                    f.put("File", licensePhoto2Data);
                    f.put("Name", "credentials-captured-2.jpg");
                    f.put("Mimetype", "image/jpg");
                    files.put(f);
                }
                if(files.length() > 0){
                    payload.put("CredentialFiles", files);
                }
                // on with the API calls
                HealthNotifierAPI.getInstance().registerProvider(payload, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // we couldn't get a response or we timed out, offline, etc
                        Logcat.d("onFailure" + e.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // TODO: global offline UX
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                mMenu.findItem(R.id.action_submit).setEnabled(true);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Network offline :( ", Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            try {
                                JSONObject recoveryResponse = new JSONObject(response.body().string());
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        HealthNotifierApplication.preferences.setProviderCredentialStatus("PENDING");// lol brolo

                                        Bus bus = HealthNotifierApplication.bus;
                                        Map<String, Object> attributes = new HashMap<String, Object>();
                                        attributes.put("Provider", false); // super explicit, in case there was some snafu
                                        Date timerEnd = new Date();
                                        long duration = timerEnd.getTime() - timerStart.getTime();
                                        attributes.put("ViewDuration", TimeUnit.MILLISECONDS.toSeconds(duration));
                                        bus.post(new AnalyticsEvent("Register Provider", attributes));
                                        // Toast it up son
                                        Toast.makeText(getActivity(), "Your credentials have been submitted and will be reviewed in the next 3 business days!", Toast.LENGTH_LONG).show();
                                        // segue, somewhere / do something or not
                                        // or get fancy with a special intent back to… bro, hard to do, hard to do
                                        // well, we can listen in MainActivity and show some dem snack bars bro, it's totally doable bro bra brizzle

                                        // we need umm startIntent back to Main in order to "force" the refresh of said status, lolzors, or of course, we could send one of those hacks events

                                        getActivity().finish();
                                    }
                                });
                            } catch(Exception e) {
                                Logcat.d("it didn't work out " + e.toString());
                            }

                        } else {
                            try {
                                JSONObject responseJSON = new JSONObject(response.body().string());
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mLocked = false;
                                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                        mMenu.findItem(R.id.action_submit).setEnabled(true);
                                        snackProgress.dismiss();
                                        String message = "Error saving credentials";
                                        Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            } catch(Exception e){
                                // lol yea so much fail
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "Credentials save failed. Please contact support@domain.com", Toast.LENGTH_SHORT).show();
                                        getActivity().finish();
                                    }
                                });
                            }
                        }
                    }
                });

            } catch(Exception e){

            }
            /*
            Preferences pref = HealthNotifierApplication.preferences;
            ProviderCredentialRequest request = new ProviderCredentialRequest(entity, pref.getAuthToken(), pref.getEmail());
            // the next time the Account fragment is rendered the option to submit will not exist
            pref.setProviderCredentialStatus("PENDING");
            // that said, known issue, this version doesn't show the status anywhere, yet

            */

        }
    }

    private Boolean isValid() {

        if (licenseNumber.getText().length() < 1) {
            forceKeyboard();
            licenseNumber.requestFocus();
            Snackbar.make(mRootView, "Please enter your License Number", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (licenseBoard.getText().length() < 1) {
            forceKeyboard();
            licenseBoard.requestFocus();
            Snackbar.make(mRootView, "Please enter your Licensing Board", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (licenseState.getText().length() < 1) {
            Snackbar.make(mRootView, "Please enter your Licensing State", Snackbar.LENGTH_SHORT).show();
            showExpirationPicker(null);
            return false;
        }

        if(licenseExpiration == null){
            Snackbar.make(mRootView, "Please enter your License Expiration", Snackbar.LENGTH_SHORT).show();
            showExpirationPicker(null);
            return false;
        }

        if (supervisorName.getText().length() < 1) {
            forceKeyboard();
            supervisorName.requestFocus();
            Snackbar.make(mRootView, "Please enter your Supervisor’s Name", Snackbar.LENGTH_SHORT).show();
            return false;
        }


        if (!Validators.isValidEmail(supervisorEmail.getText().toString())) {
            forceKeyboard();
            supervisorEmail.requestFocus();
            Snackbar.make(mRootView, "Please enter your Supervisor’s Email", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (!Validators.isValidPhone(supervisorPhone.getText().toString())) {
            forceKeyboard();
            supervisorPhone.requestFocus();
            Snackbar.make(mRootView, "Please enter your Supervisor’s Phone", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        /*
        CheckBox terms = (CheckBox) mRootView.findViewById(R.id.cbRegistrationTerms);
        if (!terms.isChecked()) {
            Toast.makeText(getActivity(), "Please review and agree to our Terms of Use", Toast.LENGTH_LONG).show();
            return false;
        }
        */

        return true;
    }

    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        // oh boy son, this is ALL wrong doh, we need our "date picker" field
        // also we need to refactor this entire form to use a model, OMG the irony
        if(event.eventName.equals("onCredentialsExpirationSet") && event.attributes != null) {
            EditText et = (EditText) mRootView.findViewById(R.id.etExpiration);
            Date d = (Date) event.attributes.get("Date");
            licenseExpiration = d;
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            et.setText(formatter.format(d));
            if (supervisorName.getText().length() < 1) {
                supervisorName.requestFocus();
            }
        }
        if(event.eventName.equals("onCredentialsStateSet") && event.attributes != null) {
            String s = (String) event.attributes.get("State");
            lState = s;
            licenseState.setText(s);
            if (licenseExpiration == null) {
                showExpirationPicker(null);
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
