package com.healthnotifier.healthnotifier.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.activity.CaptureLicenseActivity;
import com.healthnotifier.healthnotifier.activity.CaptureLifesquareActivity;
import com.healthnotifier.healthnotifier.activity.EditCollectionItemActivity;
import com.healthnotifier.healthnotifier.model.PatientResidence;
import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.adapter.CollectionAdapter;
import com.healthnotifier.healthnotifier.form.FieldValidation;
import com.healthnotifier.healthnotifier.model.CollectionMeta;
import com.healthnotifier.healthnotifier.model.Profile;
import com.healthnotifier.healthnotifier.form.FormValidation;
import com.healthnotifier.healthnotifier.form.Forms;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.Formatters;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.model.ModelField;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.adapter.LinkedHashMapAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;


public class EditProfileFragment extends Fragment {
    private View mRootView;
    private Handler mHandler;
    private String mPatientRawJson;
    private JSONObject mPatientJson;
    private String mPatientId;
    private ArrayList<ModelField> mFields;
    private Boolean mLocked = false;
    // collection bul
    private String mUpdateAction;
    private ArrayList<CollectionMeta> mCollections;
    private Menu mMenu;

    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private BottomSheetBehavior mBottomSheetBehavior;
    private Boolean jimmiesRustled = false;
    private String photoFileName = "dd_temp_capture_profile_photo.jpg";
    private String profilePhotoData = null; // meh meh meh
    private JSONObject mLicenseJson = null; // I forget why null first?
    //

    // TEMP HACK
    private Date mDate;

    private void importLicenseIntent(){
        // do the thing son import the stuffs broholio
        Intent intent = new Intent(getContext(), CaptureLicenseActivity.class);
        intent.putExtra("REQUESTOR", "profile");
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logcat.d("EditProfileFragment.onCreate");
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());

        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();

        mPatientRawJson = getActivity().getIntent().getStringExtra("PATIENT_JSON");
        try {
            mPatientJson = new JSONObject(mPatientRawJson);
            mPatientId = mPatientJson.getJSONObject("profile").getString("uuid");
        } catch(Exception e){
            mPatientJson = null;
        }

        //populate dem "fields" by config son bun hons
        mFields = new ArrayList<ModelField>();
        // TEMP stub this bizzle out
        Profile profile = new Profile();

        mFields.add(profile.firstName);
        mFields.add(profile.middleName);
        mFields.add(profile.lastName);
        mFields.add(profile.suffix);
        mFields.add(profile.birthdate);
        mFields.add(profile.organDonor);
        mFields.add(profile.discoverable);
        mFields.add(profile.demographicsPrivacy);
        mFields.add(profile.gender);
        mFields.add(profile.ethnicity);
        mFields.add(profile.biometricsPrivacy);
        mFields.add(profile.bloodType);
        mFields.add(profile.height);
        mFields.add(profile.weight);
        mFields.add(profile.pulse);
        mFields.add(profile.bpSystolic);
        mFields.add(profile.bpDiastolic);
        mFields.add(profile.hairColor);
        mFields.add(profile.eyeColor);

        mCollections = new ArrayList<CollectionMeta>();

        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "addresses";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("addresses"));
            collection.listId = R.id.lvProfileAddresses;
            // collection.adaptor = "image"
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }
        try {
            CollectionMeta collection = new CollectionMeta();
            collection.collectionId = "languages";
            collection.collectionJson = JSONHelper.jsonArrayToArrayList(mPatientJson.getJSONArray("languages"));
            collection.listId = R.id.lvProfileLanguages;
            // collection.adaptor = "image"
            this.mCollections.add(collection);
        } catch(Exception e){
            // hahahahahaha F YOU java json accessor
        }

        setHasOptionsMenu(true); // naa son
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logcat.d("EditProfileFragment.onCreateView");
        mRootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Personal Details");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mPatientJson != null){
            try {
                JSONObject profileNode = mPatientJson.getJSONObject("profile");
                Forms.initForm(getContext(), mRootView, mFields, profileNode);
            } catch (Exception e){
                // you're pretty much bent over
            }

            // birthday swiss army times
            try {
                // TODO move this into the Forms.initForm build out son
                JSONObject profileNode = mPatientJson.getJSONObject("profile");
                String birthdate = profileNode.getString("birthdate");
                EditText etDob = (EditText) mRootView.findViewById(R.id.etProfileBirthdate);
                if(birthdate.equals("0001-01-01")){
                    // null into the json so it tricks the validator, ish
                    profileNode.put("birthdate", JSONObject.NULL); // this will cause a server vaidation fail if by some chance we submit
                    // perhaps over the top, but we don't want this placeholder birthdate sticking around
                    // wipe from the text view
                    etDob.setText("");
                } else {
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                    mDate = (Date) format.parse(birthdate);
                    // the other formatter, back lol son

                    Format formatter = new SimpleDateFormat("MM/dd/yyyy");
                    etDob.setText(formatter.format(mDate));
                }

                // load profile pic
                ImageView profilePhotoView = (ImageView) mRootView.findViewById(R.id.ivProfilePhoto);
                // click handlers n such brobus
                final int size = 96;
                if(!profileNode.isNull("photo_url")){
                    mImageLoader.displayImage(profileNode.getString("photo_url"), profilePhotoView, mDisplayImageOptions, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            Bitmap circleCroppers86 = Files.getCroppedBitmap(loadedImage, size * 2);
                            ((ImageView) view).setImageBitmap(circleCroppers86);
                        }
                    });
                }

                profilePhotoView.setOnClickListener(new View.OnClickListener() {
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
                profilePhotoView.setOnLongClickListener(new View.OnLongClickListener(){
                    @Override
                    public boolean onLongClick(View v) {
                        clearProfilePhoto();
                        return true;
                    }
                });


                if(!profileNode.getBoolean("confirmed")){
                    ((LinearLayout) mRootView.findViewById(R.id.llLicenseImporter)).setVisibility(View.VISIBLE);

                    ImageView licensePhotoView = (ImageView) mRootView.findViewById(R.id.ivLicensePhoto);
                    licensePhotoView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            importLicenseIntent();
                        }
                    });
                    TextView licenseText = (TextView) mRootView.findViewById(R.id.tvLicenseHelp);
                    licenseText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            importLicenseIntent();
                        }
                    });
                } else {
                    ((LinearLayout) mRootView.findViewById(R.id.llLicenseImporter)).setVisibility(View.GONE);
                }

//                // TODO: only show this UI state during initial onboarding, aka when some real basic stuffs is missing
//                //is this ok to do this in here though? License import wiring though brolo
//                if(profileNode.isNull("first_name") || (!profileNode.isNull("first_name") && profileNode.getString("first_name").equals(""))){
//
//
//
//                } else {
//
//                }

            } catch (Exception e) {
                Logcat.d( e.toString());
            }

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
                        // damn bruh, so much cruft up in dis bitch
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

        }

        // straight comedy height "component"
        Spinner spinnerHeightFeet = (Spinner) mRootView.findViewById(R.id.spProfileHeightFeet);
        Spinner spinnerHeightInches = (Spinner) mRootView.findViewById(R.id.spProfileHeightInches);

        LinkedHashMap<String, String> valuesFeet = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> valuesInches = new LinkedHashMap<String, String>();

        valuesFeet.put(null, "-");
        valuesFeet.put("0", "0'");
        valuesFeet.put("1", "1'");
        valuesFeet.put("2", "2'");
        valuesFeet.put("3", "3'");
        valuesFeet.put("4", "4'");
        valuesFeet.put("5", "5'");
        valuesFeet.put("6", "6'");
        valuesFeet.put("7", "7'");

        valuesInches.put(null, "-");
        valuesInches.put("0", "0\"");
        valuesInches.put("1", "1\"");
        valuesInches.put("2", "2\"");
        valuesInches.put("3", "3\"");
        valuesInches.put("4", "4\"");
        valuesInches.put("5", "5\"");
        valuesInches.put("6", "6\"");
        valuesInches.put("7", "7'\"");
        valuesInches.put("8", "8'\"");
        valuesInches.put("9", "9'\"");
        valuesInches.put("10", "10'\"");
        valuesInches.put("11", "11'\"");

        LinkedHashMapAdapter<String, String> feetAdaptor = new LinkedHashMapAdapter<String, String>(mRootView.getContext(),
                R.layout.spinner_item, valuesFeet);
        spinnerHeightFeet.setAdapter(feetAdaptor);
        LinkedHashMapAdapter<String, String> inchesAdaptor = new LinkedHashMapAdapter<String, String>(mRootView.getContext(),
                R.layout.spinner_item, valuesInches);
        spinnerHeightInches.setAdapter(inchesAdaptor);

        // set da height son nuts
        try {
            JSONObject profileNode = mPatientJson.getJSONObject("profile");
            if(!profileNode.isNull("height")) {
                Double height = profileNode.getDouble("height");
                // convert that shit to ft-inc internally
                int totalInches = Formatters.centimetersToInches(height);
                int feet = Formatters.inchesToFeet(totalInches);
                int inches = Formatters.inchesToFootInches(totalInches);
                // LOL ZONE
                // if we remove the NULL we have to change this, since we're not setting by "VALUE"
                spinnerHeightFeet.setSelection(feet + 1);
                spinnerHeightInches.setSelection(inches + 1);
            }
        } catch(Exception e){
            // feet failed then
        }

        try {
            // Fing hate the notion of these try catch blocks, then again this node should be a class variable, duh, because we will miserably fail if it doesn't' exist
            JSONObject profileNode = mPatientJson.getJSONObject("profile");
            if(!profileNode.isNull("weight")) {
                Double weight = profileNode.getDouble("weight");
                int pounds = Formatters.kilogramsToPounds(weight);
                EditText et = (EditText) mRootView.findViewById(R.id.etProfileWeight);
                et.setText(String.valueOf(pounds));
            }
        } catch(Exception e){
            Logcat.d("your shee broke " + e.toString());
        }

        // plugin and specific hooks for custom controls and all dat and a bag of chips
        EditText et = (EditText) mRootView.findViewById(R.id.etProfileBirthdate);
        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDOBPickerDialog(v);
            }
        });

        // common but needing to DRY HER UP, collections instantiator code
        for(CollectionMeta collection: mCollections) {
            ListView listView = (ListView) mRootView.findViewById(collection.listId);
            CollectionAdapter adaptor = new CollectionAdapter(this.getContext(), collection.collectionJson, collection.collectionId, mPatientId);
            listView.setAdapter(adaptor);
            LayoutHelper.setListViewHeightBasedOnItems(listView);
        }
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

    private void launchCamera(){
        // at this point ask for permission
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        Uri theUri = Files.getPhotoFileUri(getContext(), photoFileName);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, theUri);
        // takePictureIntent.setData(theUri);
        if(Build.VERSION.SDK_INT <= 20){
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", Camera.CameraInfo.CAMERA_FACING_FRONT);
        }else{
            takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);// ahh yea, ghetto town
        }
        // da f https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        // https://stackoverflow.com/questions/21304489/how-to-open-private-files-saved-to-the-internal-storage-using-intent-action-view
        takePictureIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);// da f?
        if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Files.REQUEST_IMAGE_CAPTURE);
        }
    }

    private void showBottomSheet(){
        mRootView.findViewById(R.id.bsMediaCapture).setVisibility(View.VISIBLE);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        Logcat.d(String.valueOf(mBottomSheetBehavior.getPeekHeight()));
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

    private void clearProfilePhoto(){
        ((ImageView) mRootView.findViewById(R.id.ivProfilePhoto)).setImageResource(R.drawable.ic_add_a_photo_black_48dp);
        profilePhotoData = null;
        // but that doesn't mean we nulled an existing persisted set of data, lololbrolo
        // set dat to null in our "model"
        // according to iOS, we are suppose to chain our saves, if we have change for this here photo, lolbroz
        Snackbar.make(mRootView, "Profile Photo Cleared", Snackbar.LENGTH_SHORT).show();
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
                ImageView profilePhotoView = (ImageView) mRootView.findViewById(R.id.ivProfilePhoto);
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
                    profilePhotoView.setImageTintList(null);
                }

                Bitmap circleCroppers86 = Files.getCroppedBitmap(imageBitmap, size*4);
                profilePhotoView.setImageBitmap(circleCroppers86);

                // lololbro
                mRootView.findViewById(R.id.tvPhotoHelp).setVisibility(View.VISIBLE);

                imageFile.delete();
            }

        }

        if (resultCode == getActivity().RESULT_CANCELED) {
            if (requestCode == Files.REQUEST_IMAGE_CAPTURE) {
                File imageFile = null;
                Uri takenPhotoUri = Files.getPhotoFileUri(getContext(), photoFileName);
                imageFile = new File(takenPhotoUri.getPath());
            }
            //Toast.makeText(this, "Cancelled the activity", Toast.LENGTH_SHORT).show();
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

    // TODO: listen to bus form clicking da collection list items son
    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        // this is extremely ready to be retired, once we're able to get the proper fragment and instantiate it all from the forms helper
        if(event.eventName.equals("onDOBSet") && event.attributes != null) {
            EditText etDob = (EditText) mRootView.findViewById(R.id.etProfileBirthdate);
            Date d = (Date) event.attributes.get("Date");
            mDate = d;
            Format formatter = new SimpleDateFormat("MM/dd/yyyy");
            // need to pump that bad boy back into the umm attributes because we no longer pull from datepicker son
            // this is so weak ass, just build the form with the form manager already
            try {
                JSONObject profileNode = mPatientJson.getJSONObject("profile");
                profileNode.put("birthdate", formatter.format(d));
            } catch (Exception e){
                // you're pretty much bent over
            }

            etDob.setText(formatter.format(d));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_profile, menu);
        // tap that main Patient onboarding statemanger SON
        // honestly though it should only be continue if not confirmed, otherwise save
        mMenu = menu;
        try {
            if (!mPatientJson.getJSONObject("profile").getBoolean("confirmed")){
                mMenu.findItem(R.id.action_submit).setTitle("Continue");
            }
        } catch(Exception e){

        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: hook the dirty check for unsaved changes son, like iOS
                getActivity().finish();
                return true;
            case R.id.action_submit:
                onSave();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void returnToPatientActivity(){
        Intent intent;
        intent = new Intent(getContext(), PatientActivity.class);
        // we need some jimmy hacks to show the toast in Patient Activity, yea son
        intent.putExtra("PATIENT_ID", mPatientId);
        startActivity(intent);
    }

    private void saveProfileSuccess(){
        if (mLicenseJson == null) {
            Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
            returnToPatientActivity();
        } else {
            // do the address brosef brasef
            try {
                // this error handling is weak as balls
                JSONObject node = mLicenseJson.getJSONObject("results");
                // TODO: check for all required attributes, but, meh

                // this is highly suspect aka, manual validation here, but ok, here we go

                if(!node.isNull("address_line1")) {
                    // so to be super pesky, we could make an arbitrary data node, and pass it over to the collection form… so you can then save it
                    // which is different than the underlying profile behavior, w/e bro, can't have all the things
                    /*
                    Intent intent = new Intent(mContext, EditCollectionItemActivity.class);
                    intent.putExtra("PATIENT_UUID", mPatientId);
                    intent.putExtra("COLLECTION_ID", mCollectionId);
                    intent.putExtra("COLLECTION_ITEM_JSON", item.toString());
                    intent.putExtra("CALLING_ACTIVITY_NAME", mContext.getClass().getName());
                    mContext.startActivity(intent);
                    */
                    // rare exception bro, just present errors from server back in a toast, but w/e brolo

                    //JSONObject collectionNode = Forms.populateJson(mRootView, mFields, mCollectionItemJson);
                    PatientResidence instance = new PatientResidence();
                    JSONObject collectionNode = instance.getDefaultJSON();
                    try {

                        // now put all the fields we had brolo
                        collectionNode.put("address_line1", node.getString("address_line1"));
                        if(!node.isNull("address_line2")) {
                            collectionNode.put("address_line2", node.getString("address_line2"));
                        }
                        if(!node.isNull("city")) {
                            collectionNode.put("city", node.getString("city"));
                        }
                        if(!node.isNull("state_province")) {
                            // TODO: convert through values API if country is US only, else pass the raw value bro
                            collectionNode.put("state_province", node.getString("state_province"));
                        }
                        if(!node.isNull("postal_code")) {
                            // TODO: convert through values API if country is US only, else pass the raw value bro
                            collectionNode.put("postal_code", node.getString("postal_code"));
                        }
                        if(!node.isNull("country")) {
                            // TODO: convert through values API if country is US only, else pass the raw value bro
                            collectionNode.put("country", node.getString("country"));
                        }

                        // this should match whatever the new createPatient API defaults to
                        collectionNode.put("privacy", "provider");
                        collectionNode.put("residence_type", "HOME");// lol
                        collectionNode.put("lifesquare_location_type", "Other"); // no gating
                        // OK let's do it

                        // DO IT DO IT SON
                        HealthNotifierAPI.getInstance().updateCollection(mPatientId, instance.collectionName, collectionNode, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                // TODO: hook offline handler
                                // you outta luck son
                            }

                            @Override
                            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                                if (response.isSuccessful()) { // exactly what status codes determine this?
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
                                            returnToPatientActivity();
                                        }
                                    });
                                } else {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getActivity(), "Error saving address!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    Logcat.d( "onResponseError" + response.toString());
                                }
                            }
                        });

                    } catch(Exception e){
                        Logcat.d(e.getMessage());
                    }

                }

            } catch(Exception e){
                Logcat.d(e.getMessage());
            }
        }
    }

    private void onSave(){
        if(mLocked){
            return;
        }
        if(profilePhotoData != null){
            // chain that shizniz bra
            try {
                JSONObject payload = new JSONObject();
                JSONObject photoNode = new JSONObject();
                photoNode.put("Name", photoFileName);
                photoNode.put("Mimetype", "image/jpeg");// LOL BRIZZLE, this seems inconsistent
                photoNode.put("File", profilePhotoData);
                payload.put("ProfilePhoto", photoNode);

                mLocked = true;
                mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                mMenu.findItem(R.id.action_submit).setEnabled(false);
                final Snackbar snackProgress = Snackbar.make(mRootView, "Updating Profile Photo…", Snackbar.LENGTH_INDEFINITE);
                snackProgress.show();

                HealthNotifierAPI.getInstance().updateProfilePhoto(mPatientId, payload, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // we couldn't get a response or we timed out, offline, etc
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // OFFLINE BS SON
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error saving", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        Logcat.d( "onFailure" + e.toString());
                        //
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    snackProgress.dismiss();
                                    saveProfile();
                                }
                            });
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    snackProgress.dismiss();
                                    Snackbar.make(mRootView, "Error saving", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                            Logcat.d( "onResponseError" + response.toString());
                        }
                    }
                });

            } catch(Exception e){

            }
        } else {
            saveProfile();
        }
    }

    private void saveProfile(){

        // meh bro, first save photo

        // VALIDATE BIZZLE
        FormValidation validationResults = Forms.validateForm(mRootView, mFields);
        /*
        try {
            // this is safe to check directly against before whatever son
            JSONArray addresses = mPatientJson.getJSONArray("addresses");
            if(addresses.length() == 0){
                FieldValidation fieldError = new FieldValidation();
                fieldError.message = "Please add at least 1 residence!";
                validationResults.errors.add(fieldError);
            }
            //
            //
            // Spinner status brosauce™

        } catch(Exception e){

        }
        */
        // IF WE PASS
        if(validationResults.isValid()){
            // BIND BACK TO JSON?
            try {
                JSONObject profileNode = Forms.populateJson(mRootView, mFields, mPatientJson.getJSONObject("profile"));

                // class specific plugins
                // aka birthdate

                // aka height (ideally though the validator would have custom checked for this case, but w/e YOLO
                // if they are both null - aka 0 index, then toss it going back
                Spinner spinnerHeightFeet = (Spinner) mRootView.findViewById(R.id.spProfileHeightFeet);
                Spinner spinnerHeightInches = (Spinner) mRootView.findViewById(R.id.spProfileHeightInches);

                if(spinnerHeightFeet.getSelectedItemPosition() == 0 && spinnerHeightInches.getSelectedItemPosition() == 0){
                    // NULL TOWN DOWN
                    profileNode.put("height", JSONObject.NULL);
                } else {
                    if(spinnerHeightFeet.getSelectedItemPosition() > 0 && spinnerHeightInches.getSelectedItemPosition() > 0) {
                        try {
                            // all or nothing baby
                            int totalInches = 0;
                            // feet
                            totalInches += ((spinnerHeightFeet.getSelectedItemPosition() - 1) * 12);
                            // inches
                            totalInches += (spinnerHeightInches.getSelectedItemPosition() - 1);

                            Double centimeters = Formatters.inchesToCentimeters(totalInches);
                            profileNode.put("height", centimeters);
                        } catch (Exception e) {
                            Logcat.d("height serialization failed");
                        }
                    }
                }


                // aka weight
                String weightString = ((EditText) mRootView.findViewById(R.id.etProfileWeight)).getText().toString();
                // user nulled it or it was already null son
                if(weightString.equals("")){
                    profileNode.put("weight", JSONObject.NULL);
                } else {
                    try {
                        // hopefully we validate the crap out of this already anyhow, just JW and give it a shot son
                        Double kilograms = Formatters.poundsToKilograms(Integer.valueOf(weightString));
                        Logcat.d("Kilo down" + kilograms);
                        profileNode.put("weight", kilograms);
                    } catch (Exception e) {
                        Logcat.d("weight saving failed hard");
                    }
                }

                mLocked = true;
                mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                mMenu.findItem(R.id.action_submit).setEnabled(false);
                final Snackbar snackProgress = Snackbar.make(mRootView, "Updating Profile…", Snackbar.LENGTH_INDEFINITE);
                snackProgress.show();

                HealthNotifierAPI.getInstance().updateProfile(mPatientId, profileNode, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // we couldn't get a response or we timed out, offline, etc
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error saving", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        Logcat.d( "onFailure" + e.toString());
                        //
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    snackProgress.dismiss();
                                    saveProfileSuccess();
                                }
                            });
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    snackProgress.dismiss();
                                    Snackbar.make(mRootView, "Error saving", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                            Logcat.d( "onResponseError" + response.toString());
                        }
                    }
                });


            } catch (Exception e){

            }
        } else {
            mLocked = false;
            Forms.defaultDisplayFormValidation(mRootView, getContext(), validationResults);
        }
    }

    // TODO: DRY THIS BITCH UP
    // quick ghetto slap
    public void queryCollection(String patientId, String collectionId, String collectionName, String action){
        mUpdateAction = action;
        if(patientId.equals(mPatientId)){
            for(CollectionMeta collection: mCollections) {
                if(collectionId.equals(collection.collectionId)){
                    // so apparently it's a little involved at the moment to obtain the collectionName from the colletion meta, lol bones
                    // yea son, matvh it up son
                    HealthNotifierAPI.getInstance().getCollection(mPatientId, collectionName, new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getContext(), "Error loading collection. No network connection to server.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onResponse(Call call, okhttp3.Response response) throws IOException {

                            if (response.isSuccessful()) { // exactly what status codes determine this?
                                try {
                                    JSONArray collectionJSON = new JSONArray(response.body().string());
                                    // TODO: parse the response items here? or there?, meh
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateCollectionContainer(collectionId, collectionName, collectionJSON);
                                        }
                                    });
                                } catch (Exception e) {
                                    //
                                }
                            } else {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), "Error loading collection.", Toast.LENGTH_SHORT).show();
                                        // meh or is it our webservice vomiting 500's
                                    }
                                });
                            }
                        }
                    });

                    // hehehehe

                    break;
                }
            }
        } else {
            Logcat.d("queryCollection: wrong patient son");
        }
        // iterate the collections to find the meta object, in case we need extra deets
        // query that shit async
    }

    // pass in our VERB as well, so we can well form the message
    protected void updateCollectionContainer(String collectionId, String collectionName, JSONArray collectionJSON){
        // given some basic info, jimmy slip this into the container, adapter, and re-render UI
        // CoordinatorLayout cl = (CoordinatorLayout) mRootView.findViewById(R.id.cl1);
        String message = "Update Success";
        if(mUpdateAction.equals("create")){
            message = "Item Added!";
        }
        if(mUpdateAction.equals("update")){

        }
        if(mUpdateAction.equals("delete")){
            message = "Item Removed";
        }
        Snackbar.make(mRootView, message, Snackbar.LENGTH_SHORT).show();

        for(CollectionMeta collection: mCollections) {
            if(collection.collectionId.equals(collectionId)){
                ListView listView = (ListView) mRootView.findViewById(collection.listId);
                CollectionAdapter adapter = (CollectionAdapter) listView.getAdapter();
                adapter.replaceItems(JSONHelper.jsonArrayToArrayList(collectionJSON));
                adapter.notifyDataSetChanged();

                // INVALIDATE THE MF VIEW RECYCLER CACHE FOR THE LOVE OF …
                listView.setAdapter(adapter);

                try {
                    mPatientJson.put(collectionId, collectionJSON);
                } catch(Exception e){
                    Logcat.d("SYNCING JSON FAILED INTERNALLY");
                }

                LayoutHelper.setListViewHeightBasedOnItems(listView);

                break;
            }
        }
    }

    private void mergeParsedLicense(){
        // callbacks on callbacks on callbacks
        //

        try {
            // this error handling is weak as balls
            JSONObject node = mLicenseJson.getJSONObject("results");

            // wrap each with a hmm if let equivalent
            if(!node.isNull("first_name")) {
                ((EditText) mRootView.findViewById(R.id.etProfileFirstName)).setText(node.getString("first_name"));
            }
            if(!node.isNull("middle_name")) {
                ((EditText) mRootView.findViewById(R.id.etProfileMiddleName)).setText(node.getString("middle_name"));
            }
            if(!node.isNull("last_name")) {
                ((EditText) mRootView.findViewById(R.id.etProfileLastName)).setText(node.getString("last_name"));
            }
            if(!node.isNull("birthdate")) {
                // dob donkey conversion brolo
                // fake it with the event though? or not…
                String birthdate = node.getString("birthdate");
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                mDate = (Date) format.parse(birthdate);
                Format formatter = new SimpleDateFormat("MM/dd/yyyy");
                ((EditText) mRootView.findViewById(R.id.etProfileBirthdate)).setText(formatter.format(mDate));

                // double pump the birthdate straight into the bitzzle
                JSONObject profileNode = mPatientJson.getJSONObject("profile");
                profileNode.put("birthdate", formatter.format(mDate));

            }

            if(!node.isNull("height")) {
                // convert that shit to ft-inc internally
                int totalInches = node.getInt("height");
                int feet = Formatters.inchesToFeet(totalInches);
                int inches = Formatters.inchesToFootInches(totalInches);
                ((Spinner) mRootView.findViewById(R.id.spProfileHeightFeet)).setSelection(feet + 1);
                ((Spinner) mRootView.findViewById(R.id.spProfileHeightInches)).setSelection(inches + 1);
            }

            if(!node.isNull("weight")) {
                ((EditText) mRootView.findViewById(R.id.etProfileWeight)).setText(String.valueOf(node.getInt("weight")));
            }

            if(!node.isNull("gender")) {
                Forms.populateHashedSpinner((Spinner) mRootView.findViewById(R.id.spProfileGender), node.getString("gender"));
            }

            if(!node.isNull("eye_color")) {
                Forms.populateHashedSpinner((Spinner) mRootView.findViewById(R.id.spProfileEyeColor), node.getString("eye_color"));
            }

            if(!node.isNull("hair_color")) {
                Forms.populateHashedSpinner((Spinner) mRootView.findViewById(R.id.spProfileHairColor), node.getString("hair_color"));
            }

            // then save the form, lolzoin 2017™
            onSave();

        } catch(Exception e){
            Logcat.d(e.getMessage());
        }
    }

    private void handleParsedLicense(){
        // TODO: prompt with a preview of the data, meh, suck it up and just overwrite bro
        // confirm dialog with some previewing! due to the nature of the nested address, blbalala, etc jimmy jangles
        mergeParsedLicense();
    }

    public void handleLicenseScan(String content){

        // TODO: networking hooks son, then to JSON or whatever, then bind to the UI in a convenient manner, while "merging" with existing state?
        // aka overrite things yea son
        // but oh yes, it needs manual deserialization because we have now shipped a funky response schema and use it in iOS

        // snack progress

        // prepare the json bro
        try {
            JSONObject payload = new JSONObject();
            payload.put("data", content);

            mLocked = true;
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            mMenu.findItem(R.id.action_submit).setEnabled(false);
            final Snackbar snackProgress = Snackbar.make(mRootView, "Importing License…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            HealthNotifierAPI.getInstance().parseLicense(payload, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mLocked = false;
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            mMenu.findItem(R.id.action_submit).setEnabled(true);
                            snackProgress.dismiss();
                            Snackbar.make(mRootView, "Error importing", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                    Logcat.d( "onFailure" + e.toString());
                    //
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?

                        try {
                            mLicenseJson = null;
                            mLicenseJson = new JSONObject(response.body().string());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    mMenu.findItem(R.id.action_submit).setEnabled(true);
                                    snackProgress.dismiss();
                                    Snackbar.make(mRootView, "Import Succcess!", Snackbar.LENGTH_SHORT).show();
                                    handleParsedLicense();
                                }
                            });
                        } catch (Exception e) {
                            //
                        }
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                mMenu.findItem(R.id.action_submit).setEnabled(true);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error importing", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                        Logcat.d( "onResponseError" + response.toString());
                    }
                }
            });

        } catch(Exception e){

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
