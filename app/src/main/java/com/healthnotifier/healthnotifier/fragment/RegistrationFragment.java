package com.healthnotifier.healthnotifier.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
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
import com.healthnotifier.healthnotifier.utility.AnalyticsEvent;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Validators;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * A placeholder fragment containing a simple view.
 */
public class RegistrationFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;
    private Menu mMenu;

    // misc camera stuffs
    private ImageView mImageView;
    private String photoFileName = "dd_temp_capture.jpg";

    // UI and data bindings
    private EditText firstName;
    private EditText lastName;
    private Date mDate;
    private EditText phone;
    private EditText email;
    private EditText password;
    private String profilePhotoData = ""; // meh WTF should be null // TODO: on your feet son
    private int failures = 0;

    // HACKTOWN USA™
    private Boolean mLocked = false;
    private Date timerStart;
    private Boolean jimmiesRustled = false;

    private BottomSheetBehavior mBottomSheetBehavior;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_registration, menu);
        mMenu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_registration:
                if (!mLocked)
                    save();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchCamera(){
        // at this point ask for permission
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Files.getPhotoFileUri(getContext(), photoFileName));
        if(Build.VERSION.SDK_INT <= 20){
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", Camera.CameraInfo.CAMERA_FACING_FRONT);
        }else{
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);// ahh yea, ghetto town
        }
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Files.REQUEST_IMAGE_CAPTURE);
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

    // DRY THIS BIZZLE UP
    public void showDOBPickerDialog(View v) {
        //hideBottomSheet();
        RegistrationDOBPickerFragment newFragment = new RegistrationDOBPickerFragment();
        newFragment.mAttribute = "birthdate";
        if(mDate != null) {
            Bundle bundle = new Bundle();
            // ANDROID wouldn't want to support passing in objects, that would be to convenient
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            bundle.putString("INITIAL_DATE", formatter.format(mDate));
            newFragment.setArguments(bundle);
        }

        newFragment.show(getFragmentManager(), "datePicker");
    }

    // blablablal
    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                jimmiesRustled = true;
            }
        }
    };

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_registration, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Create Account");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        timerStart = new Date();
        // sheet slitz
        View bottomSheet = mRootView.findViewById(R.id.bsMediaCapture);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheet.post(new Runnable() {
            @Override
            public void run() {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        firstName = (EditText) mRootView.findViewById(R.id.etRegistrationFirstName);
        lastName = (EditText) mRootView.findViewById(R.id.etRegistrationLastName);
        email = (EditText) mRootView.findViewById(R.id.etRegistrationEmail);
        phone = (EditText) mRootView.findViewById(R.id.etRegistrationMobilePhone);
        password = (EditText) mRootView.findViewById(R.id.etRegistrationPassword);

        // crank it down
        firstName.setOnFocusChangeListener(focusListener);
        lastName.setOnFocusChangeListener(focusListener);
        email.setOnFocusChangeListener(focusListener);
        phone.setOnFocusChangeListener(focusListener);
        password.setOnFocusChangeListener(focusListener);


        phone.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    showDOBPickerDialog(v);
                    return true;
                }
                return false;
            }
        });

        // check if there is incoming email son son
        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        Bundle bd = intent.getExtras();

        if(bd != null) {
            String tEmail = (String) bd.get("EMAIL");
            if(tEmail != null && tEmail.length() > 0){
                email.setText(tEmail);
            }
        }

        EditText et = (EditText) mRootView.findViewById(R.id.etDob);
        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDOBPickerDialog(v);
            }
        });


        ImageView profilePhoto = (ImageView) mRootView.findViewById(R.id.ivProfilePhoto);
        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkCameraPermissions();
                } else {
                    showBottomSheet();
                }
            }
        });

        // TODO: better UX handling, but this works, we should show a confirm menu or some biz
        profilePhoto.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                clearProfilePhoto();
                return true;
            }
        });

        TextView terms = (TextView) mRootView.findViewById(R.id.tvTermsLink);
        terms.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.domain.com/terms/"));
                startActivity(browserIntent);
            }
        });

        mImageView = (ImageView) mRootView.findViewById(R.id.ivProfilePhoto);


        final Context context = getContext();

        // wire your sheet buttons son buns
        LinearLayout llActionCamera = (LinearLayout) mRootView.findViewById(R.id.llActionCamera);
        // FIRST CAMERA PERMISSIONS CHECK SON SON SON SON SON SON SONS
        // TODO: PERMISSIONS CODE HERE
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
                    Intent takePictureIntent = new Intent();
                    takePictureIntent.setType("image/jpeg");
                    takePictureIntent.setAction(Intent.ACTION_GET_CONTENT);
                    takePictureIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, Files.REQUEST_GALLERY);
                    }
                }
            });
        }

        TextView tvTitle = (TextView) mRootView.findViewById(R.id.tvSheetTitle);
        tvTitle.setText("Add Profile Photo");

        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (profilePhotoData.equals("") && actionId == EditorInfo.IME_ACTION_NEXT) {
                    // password.setEnabled(false);



                    showBottomSheet();
                    // password.setEnabled(true);
                    return true;
                }
                if (!profilePhotoData.equals("") && actionId == EditorInfo.IME_ACTION_NEXT) {
                    // submit??
                    save();
                    return true;
                }
                return false;
            }
        });

        // TODO: bind hideBottomSheet to all focus handlers of the text inputs, LOL FAILZORS DOT COMZORS

        mRootView.findViewById(R.id.tvPhotoHelp).setVisibility(View.GONE);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // are we fragmenting up in this
        mRootView.findViewById(R.id.bsMediaCapture).setVisibility(View.GONE);
        hideBottomSheet();
        if (resultCode == getActivity().RESULT_OK) {
            File imageFile = null;
            if (requestCode == Files.REQUEST_GALLERY) {
                Uri photoUri = data.getData();
                if (photoUri != null) {
                    String pathFromMediaStore = Files.getPathFromMediaUri(getContext(), photoUri);
                    if (pathFromMediaStore != null) {
                        imageFile = new File(pathFromMediaStore);
                    }
                }
            }

            if (requestCode == Files.REQUEST_IMAGE_CAPTURE) {
                Uri takenPhotoUri = Files.getPhotoFileUri(getContext(), photoFileName);
                imageFile = new File(takenPhotoUri.getPath());
            }

            if (imageFile != null && imageFile.exists()) {

                // rotation as needed for things like tablets
                Bitmap rotated = Files.rotateBitmapOrientation(imageFile.getAbsolutePath());
                Bitmap imageBitmap = null;

                int size = 96;
                BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
                mBitmapOptions.inSampleSize = 16;

                if (rotated == null) {
                    profilePhotoData = Files.fileToString(imageFile); // missind our compression bro bra
                } else {
                    profilePhotoData = Files.bitmapToString(rotated, Bitmap.CompressFormat.JPEG, Config.JPEG_COMPRESSION);
                }

                byte[] decodedString = Base64.decode(profilePhotoData, Base64.DEFAULT);
                imageBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length, mBitmapOptions);

                if (Build.VERSION.SDK_INT >= 21) {
                    mImageView.setImageTintList(null);
                }

                Bitmap circleCroppers86 = Files.getCroppedBitmap(imageBitmap, size*4);
                mImageView.setImageBitmap(circleCroppers86);

                mRootView.findViewById(R.id.tvPhotoHelp).setVisibility(View.VISIBLE);

                imageFile.delete();
            }

        }

        if (resultCode == getActivity().RESULT_CANCELED) {
            // Toast.makeText(this, "Cancelled the activity", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearProfilePhoto(){
        mImageView.setImageResource(R.drawable.ic_add_a_photo_black_48dp);
        // mImageView.setImageResource(R.drawable.ic_add_a_photo_black_24dp);
        profilePhotoData = "";
        Snackbar.make(mRootView, "Profile Photo Cleared", Snackbar.LENGTH_SHORT).show();
    }

    private void save(){
        if(isValid()){
            mLocked = true;
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            // TODO: sanitize by stripping, this is already validated
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            String dateString = formatter.format(mDate);

            // hell yea son
            final Snackbar snackProgress = Snackbar.make(mRootView, "Creating your account…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            mMenu.findItem(R.id.action_save_registration).setEnabled(false);

            try {
                JSONObject payload = new JSONObject();
                payload.put("FirstName", firstName.getText().toString());
                payload.put("LastName", lastName.getText().toString());
                payload.put("Email", email.getText().toString());
                payload.put("MobilePhone", phone.getText().toString());
                payload.put("Password", password.getText().toString());
                payload.put("DOB", dateString);

                HashMap<String, String> attributes = null;
                if(profilePhotoData.length() > 0){
                    JSONObject fileNode = new JSONObject();
                    fileNode.put("File", profilePhotoData);
                    fileNode.put("Name", "profile-captured.jpg");
                    fileNode.put("Mimetype", "image/jpg");
                    payload.put("ProfilePhoto", fileNode);
                }

                // ok let's touch the API now, already
                HealthNotifierAPI.getInstance().createAccount(payload, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // we couldn't get a response or we timed out, offline, etc
                        Logcat.d("onFailure" + e.toString());
                        // mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // TODO: GLOBAL NETWORK OFFLINE UX here, though son
                                // then class specific stuffs
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                mMenu.findItem(R.id.action_save_registration).setEnabled(true);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error creating account:( ", Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            try {
                                JSONObject responseJSON = new JSONObject(response.body().string());
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Bus bus = HealthNotifierApplication.bus;
                                        Map<String, Object> attributes = new HashMap<String, Object>();
                                        // this is a pointless try catch, exception being someone accidentally changes the API response
                                        try {
                                            attributes.put("AccountId", responseJSON.getString("account_id")); // this is funny, but we probably have the correct account ID now, probably
                                        } catch(Exception e){
                                            attributes.put("AccountId", 420); // this is funny, but we probably have the correct account ID now, probably
                                        }
                                        attributes.put("ValidationFails", failures);
                                        if(profilePhotoData.length() > 0){
                                            attributes.put("Photo", true);
                                        }else {
                                            attributes.put("Photo", false);
                                        }
                                        Date timerEnd = new Date();
                                        long duration = timerEnd.getTime() - timerStart.getTime();
                                        attributes.put("ViewDuration", TimeUnit.MILLISECONDS.toSeconds(duration));
                                        bus.post(new AnalyticsEvent("Register User", attributes));

                                        // TODO: snackbars during transition states bro bra
                                        Toast.makeText(getActivity(), "Your account has been created! Please continue setup.", Toast.LENGTH_LONG).show();

                                        //Bus bus = HealthNotifierApplication.bus;
                                        Map<String, Object> attributes2 = new HashMap<String, Object>();
                                        attributes2.put("Email", email.getText().toString());
                                        attributes2.put("Password", password.getText().toString());
                                        bus.post(new GenericEvent("AutoLogin", attributes2));
                                        /*
                                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                                        // THIS IS A TEMP HACK WORKAROUND TO AUTO FILL STUFFS and login, cheap, hahahahahahahaah
                                        intent.putExtra("EMAIL", email.getText().toString());
                                        intent.putExtra("PASSWORD", password.getText().toString());
                                        startActivity(intent);
                                        */
                                        // apparently we're already logged in somewhere deep down, maybe, whatever though

                                        // TODO: show thank you screenw
                                        //


                                    }
                                });
                            } catch(Exception e){
                                // balls son
                            }


                        } else {
                            Logcat.d("onResponseError" + response.toString());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    mMenu.findItem(R.id.action_save_registration).setEnabled(true);
                                    snackProgress.dismiss();

                                    String message = "Registration failed :(";
                                    // check with iOS / API docs for additional errors brizzzle
                                    if(response.code() == 400){
                                        // this is weak as hell
                                        // TODO: weak ass
                                        // also this will not fit in a Snack Bark brizzle
                                        message = "An existing account may exist with that email, or your phone is not a mobile phone (no landlines).";
                                    }
                                    failures += 1;
                                    Snackbar.make(mRootView, message, Snackbar.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });

            } catch(Exception e){
                Logcat.d("ballsdeep" + e.toString());
            }

        } else {
            failures += 1;
        }
    }

    private Boolean isValid() {

        if (firstName.getText().length() < 1) {
            forceKeyboard();
            firstName.requestFocus();
            Snackbar.make(mRootView, "Please enter your First Name", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (lastName.getText().length() < 1) {
            forceKeyboard();
            lastName.requestFocus();
            Snackbar.make(mRootView, "Please enter your Last Name", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (!Validators.isValidEmail(email.getText().toString())) {
            forceKeyboard();
            email.requestFocus();
            Snackbar.make(mRootView, "Please enter your Email", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (!Validators.isValidPhone(phone.getText().toString())) {
            forceKeyboard();
            phone.requestFocus();
            Snackbar.make(mRootView, "Please enter your Mobile Phone", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if(mDate == null){
            // simulate the click SON
            Snackbar.make(mRootView, "Please enter your DOB", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        if (!Validators.isValidPassword(password.getText().toString())) {
            forceKeyboard();
            password.requestFocus();
            Snackbar.make(mRootView, "Password must be at least 8 characters long and contain either a number or a symbol e.g. #!*", Snackbar.LENGTH_LONG).show();
            return false;
        }

        CheckBox terms = (CheckBox) mRootView.findViewById(R.id.cbRegistrationTerms);
        if (!terms.isChecked()) {
            Snackbar.make(mRootView, "Please review and agree to our Terms of Use", Snackbar.LENGTH_LONG).show();
            return false;
        }

        // TODO: require da photo son, no messin round
        // TODO: use an opt-out confirm
        /*
        if(profilePhotoData.equals("")){
            // TODO: simulate the click SON
            showBottomSheet();
            // right it's clearly non optional at this point
            //
            //
            Toast.makeText(getActivity(), "Please add your photo", Toast.LENGTH_SHORT).show();
            return false;
        }
        */


        return true;
    }

    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        if(event.eventName.equals("onDOBSet") && event.attributes != null) {
            EditText etDob = (EditText) mRootView.findViewById(R.id.etDob);
            Date d = (Date) event.attributes.get("Date");
            mDate = d;
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            etDob.setText(formatter.format(d));

            if (!Validators.isValidPassword(password.getText().toString())) {
                password.requestFocus();
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
