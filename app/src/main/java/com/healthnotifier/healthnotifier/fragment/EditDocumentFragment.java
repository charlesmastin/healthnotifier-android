package com.healthnotifier.healthnotifier.fragment;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.EditMedicalActivity;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.adapter.CapturedFilesAdapter;
import com.healthnotifier.healthnotifier.adapter.DocumentFilesAdapter;
import com.healthnotifier.healthnotifier.form.FieldValidation;
import com.healthnotifier.healthnotifier.form.FormValidation;
import com.healthnotifier.healthnotifier.form.Forms;
import com.healthnotifier.healthnotifier.adapter.LinkedHashMapAdapter;
import com.healthnotifier.healthnotifier.utility.Files;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Created by charles on 2/13/17.
 */

public class EditDocumentFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;
    private String mPatientId;
    private Menu mMenu;
    private String mCollectionId = "directives";
    private JSONObject mCollectionItem = null;
    private JSONObject mDD = null;//the actual full DD via the document API so misleading
    private String mCallingActivityName;
    private Boolean mLocked = false;
    private String photoFileName = "dd_temp_capture.jpg";
    private CapturedFilesAdapter mAdapter;
    private DocumentFilesAdapter mAdapterEdit;
    private ArrayList<HashMap<String, Object>> mFiles; // TODO: this is a sloppy mess of extra type checks, just make it ArrayList<JSONObject>
    private Boolean mEditMode = false;
    private Boolean mDirty = false;
    private String mSource = "camera"; // this is quick hack

    private FloatingActionButton fab1, fab2; //hmm interesting syntax, I might like this
    private Animation anFabOpen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
        Intent intent = getActivity().getIntent();
        if(intent.hasExtra("PATIENT_UUID")){
            mPatientId = intent.getStringExtra("PATIENT_UUID");
        }
        if(intent.hasExtra("COLLECTION_ID")) {
            mCollectionId = intent.getStringExtra("COLLECTION_ID");
            if(intent.hasExtra("COLLECTION_ITEM_JSON")){
                mEditMode = true;
                try {
                    mCollectionItem = new JSONObject(intent.getStringExtra("COLLECTION_ITEM_JSON"));
                } catch(Exception e){

                }
            }
        }
        // LOL BRO ON non final static strings
        if(intent.hasExtra("CALLING_ACTIVITY_NAME")){
            mCallingActivityName = intent.getStringExtra("CALLING_ACTIVITY_NAME");
        }
        mFiles = new ArrayList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logcat.d("EditProfileFragment.onCreateView");
        mRootView = inflater.inflate(R.layout.fragment_edit_document, container, false);
        String title = "Add Item";
        if(mCollectionId.equals("directives")){
            if(mEditMode){
                title = "Edit Directive";
            } else {
                title = "Add Directive";
            }
        }
        if(mCollectionId.equals("documents")){
            if(mEditMode){
                title = "Edit Document";
            } else {
                title = "Add Document";
            }
        }
        LayoutHelper.initActionBar(getActivity(), mRootView, title);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mEditMode){
            // TODO there is exactly 0 recovery from a failed document load
            // TODO: dry up the bailzone3000 error handler as a runnable

            mRootView.findViewById(R.id.scrollView1).setVisibility(View.INVISIBLE);
            // load up your document via GET API,
            try {
                String uuid = mCollectionItem.getString("uuid");
                HealthNotifierAPI.getInstance().getDocument(uuid, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // we couldn't get a response or we timed out, offline, etc
                        Logcat.d("onFailure" + e.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // OFFLINE AND SCREWED PRETTY MUCH
                                Toast.makeText(getContext(), "Unable to load Document", Toast.LENGTH_SHORT).show();
                                getActivity().finish();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            // access body here
                            try {
                                mDD = new JSONObject(response.body().string());
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mRootView.findViewById(R.id.scrollView1).setVisibility(View.VISIBLE);
                                        doubleSecretInit();
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
            } catch(Exception e){

            }
        }else{
            doubleSecretInit();
        }
    }

    private void doubleSecretInit(){
        // once we have all data and all that shiz shaz

        // ok this here is the passed in MODE, chip chope down that last bit
        String mode = mCollectionId.substring(0, mCollectionId.length() - 1);
        if(mode.equals("document")){
            ((TextView) mRootView.findViewById(R.id.tvDocumentType)).setText("Document Type *");
        } else {
            ((TextView) mRootView.findViewById(R.id.tvDocumentType)).setText("Directive Type *");
            mRootView.findViewById(R.id.jwDocumentTitle).setVisibility(View.GONE);
        }

        Spinner spinner = (Spinner) mRootView.findViewById(R.id.spDocumentType);
        LinkedHashMapAdapter<String, String> spinnerAdaptor = new LinkedHashMapAdapter<String, String>(getContext(),
                R.layout.spinner_item, HealthNotifierAPI.getInstance().getValues(mode));
        spinner.setAdapter(spinnerAdaptor);

        // TODO: default to authorized viewers son
        Spinner spinner2 = (Spinner) mRootView.findViewById(R.id.spDocumentPrivacy);
        LinkedHashMapAdapter<String, String> spinnerAdaptor2 = new LinkedHashMapAdapter<String, String>(getContext(),
                R.layout.spinner_item, HealthNotifierAPI.getInstance().getValues("privacy"));
        spinner2.setAdapter(spinnerAdaptor2);

        //((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Add Document");
        // set dat state
        // change handler on dem document type
        // on dem privacy
        // file for days son
        // init dem dam fabs son

        ((Button) mRootView.findViewById(R.id.btAddItem)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: wire bottom sheet in this bitch
                checkCameraPermissions();
            }
        });

        /*
        anFabOpen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab1 = (FloatingActionButton) mRootView.findViewById(R.id.fabActionCamera);
        if(fab1 != null) {
            fab1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkCameraPermissions();
                }
            });
        }

        fab2 = (FloatingActionButton) mRootView.findViewById(R.id.fabActionLibrary);

        if(fab2 != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fab2.setVisibility(View.GONE);
            }else {
                fab2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkStoragePermissions();
                    }
                });
            }
        }
        // ghetto timers seems animation is clashing on it's nuts
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // animating at the same time will have to do, ugg
                        fab1.startAnimation(anFabOpen);
                        fab2.startAnimation(anFabOpen);
                    }
                }, 150);
        */



        if(mEditMode){
            ((Button) mRootView.findViewById(R.id.btDeleteCollectionItem)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    promptDelete();
                }
            });
            // remove add item for now
            mRootView.findViewById(R.id.btAddItem).setVisibility(View.GONE);

            // initial values on da spinners and what not
            // basically if this doesn't work, we should finish the activity though
            try {
                Forms.populateHashedSpinner(spinner, mCollectionItem.getString("category"));
                Forms.populateHashedSpinner(spinner2, mCollectionItem.getString("privacy"));
                if(mCollectionId.equals("documents")){
                    if(!mCollectionItem.isNull("title")) {
                        ((EditText) mRootView.findViewById(R.id.etDocumentTitle)).setText(mCollectionItem.getString("title"));
                    }
                }
                // TODO: fire off a request to get all the info about the document via the GET document API SON BUN HON
                ArrayList<JSONObject> files = JSONHelper.jsonArrayToArrayList(mDD.getJSONArray("Files"));
                mAdapterEdit = new DocumentFilesAdapter(getContext(), files);// right now just simmy wreck it in there
                ListView listView = (ListView) mRootView.findViewById(R.id.lvFiles);
                listView.setAdapter(mAdapterEdit);
                // first height based on children, then we need a "loaded:complete" event from within the adapter so we can give it a final smack down, yea son
                // LayoutHelper.setListViewHeightBasedOnItems(listView); // ogLVJW as other option bro
                // jimmy hack the situtation because we know the fixed amount, IF we're gonna do the crop stop stank wank bank

                ViewGroup.LayoutParams params = listView.getLayoutParams();
                // this is in DP, so hopefully that works son
                int totalHeight = 0;
                totalHeight = mAdapterEdit.getCount() * (96 + 16);
                params.height = (totalHeight + (listView.getDividerHeight() * (mAdapterEdit.getCount() - 1))) * 2; // huh wtf, DP / SP / BREAK DOWN FAKEDOWN
                listView.setLayoutParams(params);
                listView.requestLayout();


            } catch(Exception e){

            }

        } else {
            mAdapter = new CapturedFilesAdapter(getContext(), mFiles);
            ListView listView = (ListView) mRootView.findViewById(R.id.lvFiles);
            listView.setAdapter(mAdapter);
            //mMenu.findItem(R.id.action_delete_document).setVisible(false);
            mRootView.findViewById(R.id.btDeleteCollectionItem).setVisibility(View.GONE);
            Forms.populateHashedSpinner(spinner2, "provider"); // YEA SON
        }

        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case HealthNotifierApplication.PERMISSION_READ_EXTERNAL_STORAGE_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    browseGallery();
                } else {
                    Snackbar.make(mRootView, "Permission required to browse your gallery", Snackbar.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case HealthNotifierApplication.PERMISSION_CAMERA_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    launchCamera();
                } else {
                    Snackbar.make(mRootView, "Permission required to user your camera", Snackbar.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
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
                    // Toast.makeText(getContext(), "QR scanning requires the camera", Toast.LENGTH_LONG);
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

    private void checkStoragePermissions(){
        if (!mLocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // permission check though
                // TODO: DRY THIS UP SILICA GEL ON YOUR NUTS EDITION
                String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
                if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    // do we need rationale
                    if (shouldShowRequestPermissionRationale(permission)) {

                        // TODO: explain it, hmm and ask for it
                        Snackbar sb = Snackbar.make(mRootView, "Permission required to browse your gallery", Snackbar.LENGTH_INDEFINITE);
                        sb.setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sb.dismiss();
                                requestPermissions(new String[]{ permission }, HealthNotifierApplication.PERMISSION_READ_EXTERNAL_STORAGE_CODE);
                            }
                        });
                        sb.show();

                    } else {
                        requestPermissions(new String[]{permission}, HealthNotifierApplication.PERMISSION_READ_EXTERNAL_STORAGE_CODE);
                    }
                } else {
                    browseGallery();
                }
            } else {
                // authorized on install
                // TODO: we should still check though
                browseGallery();
            }
        }
    }

    private void launchCamera(){
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Files.getPhotoFileUri(getActivity(), photoFileName));
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Files.REQUEST_IMAGE_CAPTURE);
        }
    }

    private void browseGallery(){
        Intent takePictureIntent = new Intent();
        takePictureIntent.setType("image/*"); // hopefully no gifs come in, lol
        //String[] mimetypes = {"image/jpeg", "image/png"};
        //takePictureIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        takePictureIntent.setAction(Intent.ACTION_GET_CONTENT);
        takePictureIntent.addCategory(Intent.CATEGORY_OPENABLE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, Files.REQUEST_GALLERY);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_document, menu);
        mMenu = menu;
        if(!mEditMode){
            mMenu.findItem(R.id.action_delete_document).setVisible(false);// lol bro
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(!mLocked){
                    // TODO: alert if tossing away unsaved changes
                    getActivity().finish();
                }
                return true;
            case R.id.action_save_document:
                if (!mLocked)
                    onSave();
                return true;
            case R.id.action_delete_document:
                if (!mLocked)
                    promptDelete();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        File imageFile = null;
        Logcat.d("onActivityResult: " + requestCode + "-" + resultCode + "-");
        if (resultCode == RESULT_OK){

            if (requestCode == Files.REQUEST_GALLERY){
                Uri photoUri = data.getData();
                try {
                    Logcat.d("gallery read" + photoUri.toString());
                }catch (Exception e){
                    Logcat.d("gallery read fail" + e.toString());
                }
                Context context = getContext();
                if(photoUri != null){
                    String pathFromMediaStore = Files.getPathFromMediaUri(context, photoUri);
                    try {
                        Logcat.d("gallery read path" + pathFromMediaStore.toString());
                    }catch (Exception e){
                        Logcat.d("gallery read path fail" + e.toString());
                    }
                    if (pathFromMediaStore != null){
                        imageFile = new File(pathFromMediaStore);
                        // TODO: be more specific, it might be from dropbox or picassa, or flickr or some crap
                        mSource = "gallery";
                    }
                }
            }

            if (requestCode == Files.REQUEST_IMAGE_CAPTURE){
                Uri takenPhotoUri = Files.getPhotoFileUri(getContext(), photoFileName);
                imageFile = new File(takenPhotoUri.getPath());
                mSource = "camera";
            }

            if(imageFile != null && imageFile.exists()) {
                HashMap<String, Object> attributes = new HashMap<String, Object>();
                // rotation as needed for things like tablets
                Bitmap rotated = Files.rotateBitmapOrientation(imageFile.getAbsolutePath());
                if(rotated == null){
                    attributes.put("File", Files.fileToString(imageFile));
                }else {
                    attributes.put("File", Files.bitmapToString(rotated, Bitmap.CompressFormat.JPEG, Config.JPEG_COMPRESSION));// play with da compression homie
                }
                attributes.put("Name", "dd-temp-capture.jpg");
                attributes.put("Mimetype", "image/jpeg");
                imageFile.delete();

                mFiles.add(attributes);
                mAdapter.notifyDataSetChanged();

                ((TextView) mRootView.findViewById(R.id.tvParts)).setVisibility(View.VISIBLE);

                ListView listView = (ListView) mRootView.findViewById(R.id.lvFiles);
                LayoutHelper.ogLVJW(listView);
            } else {

            }
        }

        if (resultCode == RESULT_CANCELED){
        }

    }

    // TODO: loader / spinner / locker during network transaction son
    private void onDelete(){
        // scrub it through confirmation though
        if(mLocked){
            return;
        }
        mLocked = true;
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        mMenu.findItem(R.id.action_delete_document).setEnabled(false);
        mMenu.findItem(R.id.action_save_document).setEnabled(false);
        mRootView.findViewById(R.id.btDeleteCollectionItem).setEnabled(false);

        final Snackbar snackProgress = Snackbar.make(mRootView, "Deleting your document…", Snackbar.LENGTH_INDEFINITE);
        snackProgress.show();

        try {
            String uuid = mCollectionItem.getString("uuid");
            HealthNotifierAPI.getInstance().deleteDocument(uuid, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // we couldn't get a response or we timed out, offline, etc
                    Logcat.d("onFailure" + e.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO: global offline UX
                            mMenu.findItem(R.id.action_delete_document).setEnabled(true);
                            mMenu.findItem(R.id.action_save_document).setEnabled(true);
                            mRootView.findViewById(R.id.btDeleteCollectionItem).setEnabled(true);
                            mLocked = false;
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            snackProgress.dismiss();
                            Snackbar.make(mRootView, "Error deleting document :( ", Snackbar.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // TODO: analytics bro
                                finishWithIntent(true);

                            }
                        });
                    } else {
                        Logcat.d("onResponseError" + response.toString());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mMenu.findItem(R.id.action_delete_document).setEnabled(true);
                                mMenu.findItem(R.id.action_save_document).setEnabled(true);
                                mRootView.findViewById(R.id.btDeleteCollectionItem).setEnabled(true);
                                mLocked = false;
                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error deleting document :( ", Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        } catch(Exception e){

        }

    }

    private void promptDelete(){
        // snack bar or alert dialog, you decide, epic rap battle edition
        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle("Really Delete?");
        // alertDialog.setMessage(getString(R.string.dialog_delete_profile_message));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // call dat delete
                        onDelete();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private FormValidation validateForm(){
        FormValidation validator = new FormValidation();
        Spinner spinnerPrivacy = (Spinner) mRootView.findViewById(R.id.spDocumentPrivacy);
        if(spinnerPrivacy.getSelectedItem() != null && spinnerPrivacy.getSelectedItemPosition() > 0){

        } else {
            FieldValidation fieldError = new FieldValidation();
            fieldError.message = "Privacy must be selected!";
            validator.errors.add(fieldError);
        }
        Spinner spinnerCategory = (Spinner) mRootView.findViewById(R.id.spDocumentType);
        if(spinnerCategory.getSelectedItem() != null && spinnerCategory.getSelectedItemPosition() > 0){

        } else {
            FieldValidation fieldError = new FieldValidation();
            fieldError.message = "Type must be selected!";
            validator.errors.add(fieldError);
        }
        if(!mEditMode){
            if(mFiles.size() == 0){
                FieldValidation fieldError = new FieldValidation();
                fieldError.message = "Please add at least 1 page!";
                validator.errors.add(fieldError);
            }
        }
        return validator;
    }

    private void onSave(){
        if(mLocked){
            return;
        }

        FormValidation validator = validateForm();
        if(!validator.isValid()){
            Forms.defaultDisplayFormValidation(mRootView, getContext(), validator);
            return;
        }

        // do validations, and if legit status man,
        // prep that payload son
        // send on to our newly minted API
        // handle dem networking code

        // inline validation though, different based on each case though

        mLocked = true;
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        mMenu.findItem(R.id.action_save_document).setEnabled(false);

        if(mEditMode){
            final Snackbar snackProgress = Snackbar.make(mRootView, "Updating your document…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            try {
                // DRY IT UP OR DIE
                JSONObject payload = new JSONObject();
                Map.Entry<String, String> itemPrivacy = (Map.Entry<String, String>) ((Spinner) mRootView.findViewById(R.id.spDocumentPrivacy)).getSelectedItem();
                payload.put("Privacy", itemPrivacy.getKey());
                Map.Entry<String, String> itemCategory = (Map.Entry<String, String>) ((Spinner) mRootView.findViewById(R.id.spDocumentType)).getSelectedItem();
                payload.put("Category", itemCategory.getKey());// oh this is so amusing, w/e FML
                // Title if relevant, check against empty string / w/e
                if(mCollectionId.equals("documents")){
                    String title = ((EditText) mRootView.findViewById(R.id.etDocumentTitle)).getText().toString();
                    if(!title.equals("")) {
                        payload.put("Title", title);
                    }
                }

                HealthNotifierAPI.getInstance().updateDocument(mCollectionItem.getString("uuid"), payload, new Callback() {
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
                                mMenu.findItem(R.id.action_save_document).setEnabled(true);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error updating document :( ", Snackbar.LENGTH_LONG).show();
                                // context.finish();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {

                                    // ANALYTICS BRO
                                    finishWithIntent(false);

                                }
                            });
                        } else {
                            Logcat.d("onResponseError" + response.toString());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    mMenu.findItem(R.id.action_save_document).setEnabled(true);
                                    snackProgress.dismiss();
                                    Snackbar.make(mRootView, "Error updating document :( ", Snackbar.LENGTH_LONG).show();
                                    // context.finish();
                                }
                            });
                        }
                    }
                });

            } catch(Exception e){

            }

        } else {
            // add it up son
            // assemble dem json
            // TODO: magic class binding on category / privacy meh
            Logcat.d("BEGINNING DOC SUBMISSION: TIME CHECK SON");

            final Snackbar snackProgress = Snackbar.make(mRootView, "Uploading your document…", Snackbar.LENGTH_INDEFINITE);
            snackProgress.show();

            try {
                JSONObject payload = new JSONObject();
                payload.put("PatientId", mPatientId);
                Map.Entry<String, String> itemPrivacy = (Map.Entry<String, String>) ((Spinner) mRootView.findViewById(R.id.spDocumentPrivacy)).getSelectedItem();
                payload.put("Privacy", itemPrivacy.getKey());
                Map.Entry<String, String> itemCategory = (Map.Entry<String, String>) ((Spinner) mRootView.findViewById(R.id.spDocumentType)).getSelectedItem();
                payload.put("DirectiveType", itemCategory.getKey());

                // Title if relevant, check against empty string / w/e
                if(mCollectionId.equals("documents")){
                    String title = ((EditText) mRootView.findViewById(R.id.etDocumentTitle)).getText().toString();
                    if(!title.equals("")) {
                        payload.put("Title", title);
                    }
                }

                // magic files array son, makes me desire to just store ArrayList<JSONObject> lol bra
                JSONArray filesNode = new JSONArray();
                for(HashMap<String, Object> fileItem : mFiles) {
                    // convert that shizzle to base64 here and now son
                    JSONObject fileNode = new JSONObject();
                    fileNode.put("File", fileItem.get("File").toString()); // TODO: compress now / or later? but somewhere to optimize performance
                    fileNode.put("Name", fileItem.get("Name").toString());
                    fileNode.put("Mimetype", fileItem.get("Mimetype").toString());
                    filesNode.put(fileNode);
                }

                payload.put("Files", filesNode);

                // ok let's touch the API now, already
                HealthNotifierAPI.getInstance().createDocument(payload, new Callback() {
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
                                mMenu.findItem(R.id.action_save_document).setEnabled(true);
                                snackProgress.dismiss();
                                Snackbar.make(mRootView, "Error creating document :( ", Snackbar.LENGTH_LONG).show();
                                // context.finish();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {

                                    // ANALYTICS BRO
                                    finishWithIntent(false);

                                }
                            });
                        } else {
                            Logcat.d("onResponseError" + response.toString());
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mLocked = false;
                                    mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                    mMenu.findItem(R.id.action_save_document).setEnabled(true);
                                    snackProgress.dismiss();
                                    Snackbar.make(mRootView, "Error creating document :( ", Snackbar.LENGTH_LONG).show();
                                    // context.finish();
                                }
                            });
                        }
                    }
                });

            } catch(Exception e){
                Logcat.d("ballsdeep" + e.toString());
            }
        }

    }

    private void finishWithIntent(Boolean deleted){
        Intent intent = null;
        // if we can't find the intent, go all the way back to the Patient screen, lol
        switch(mCallingActivityName){
            case "com.healthnotifier.healthnotifier.activity.EditMedicalActivity":
                intent = new Intent(getContext(), EditMedicalActivity.class);
                break;
            case "com.healthnotifier.healthnotifier.activity.PatientActivity":
                // NOTE: we will simply reload the entire enchillada when we go there
                // Due to the lack of duplicating the intent consumer and collection refresh code yet again
                // LOLBRO
                // if we were boss, we could build some of that into the adapters, IMO
                // so they could async reload
                intent = new Intent(getContext(), PatientActivity.class);
                break;
            default:
                // no matches, go to the patient screen as a last resort
                break;
        }

        if(intent != null) {
            String action = "create";
            if (mEditMode) {
                action = "update";
            }
            if (deleted) {
                action = "delete";
            }
            intent.putExtra("EVENT", "CollectionUpdate");
            intent.putExtra("COLLECTION_ACTION", action);
            intent.putExtra("COLLECTION_ID", mCollectionId);
            intent.putExtra("COLLECTION_NAME", mCollectionId); // in the future, we should remove said dependency
            intent.putExtra("PATIENT_ID", mPatientId);
            startActivity(intent);
        } else {
            Logcat.d("FAILED AT FINDING THE CALLING ACTIVITY");
            getActivity().finish();
        }

    }

}
