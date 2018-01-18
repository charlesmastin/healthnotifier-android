package com.healthnotifier.healthnotifier.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.EditContactsActivity;
import com.healthnotifier.healthnotifier.activity.EditEmergencyActivity;
import com.healthnotifier.healthnotifier.activity.EditMedicalActivity;
import com.healthnotifier.healthnotifier.activity.EditProfileActivity;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.form.FormValidation;
import com.healthnotifier.healthnotifier.form.Forms;
import com.healthnotifier.healthnotifier.adapter.LinkedHashMapAdapter;
import com.healthnotifier.healthnotifier.model.CollectionItem;
import com.healthnotifier.healthnotifier.model.ModelField;
import com.healthnotifier.healthnotifier.model.PatientAllergy;
import com.healthnotifier.healthnotifier.model.PatientCondition;
import com.healthnotifier.healthnotifier.model.PatientEmergencyContact;
import com.healthnotifier.healthnotifier.model.PatientHospital;
import com.healthnotifier.healthnotifier.model.PatientImmunization;
import com.healthnotifier.healthnotifier.model.PatientInsurance;
import com.healthnotifier.healthnotifier.model.PatientLanguage;
import com.healthnotifier.healthnotifier.model.PatientMedication;
import com.healthnotifier.healthnotifier.model.PatientPharmacy;
import com.healthnotifier.healthnotifier.model.PatientPhysician;
import com.healthnotifier.healthnotifier.model.PatientProcedure;
import com.healthnotifier.healthnotifier.model.PatientResidence;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Subscribe;

import org.json.JSONArray;
import org.json.JSONObject;

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

/**
 * Created by charles on 1/30/17.
 */

public class EditCollectionItemFragment extends Fragment {

    private View mRootView;
    private Handler mHandler;
    private String mPatientId;
    private String mCollectionId; // passed in from bundle nuts
    private String mCollectionName; // checp town vs generics for the instance
    private String mCollectionItemRawJson;
    private JSONObject mCollectionItemJson;
    private ArrayList<ModelField> mFields;
    private Boolean mEditMode = false;
    private Boolean mDirty = false;
    private String mInstanceTitle;

    // LUL, way to go android
    private String mCallingActivityName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logcat.d("EditCollectionItemFragment.onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mHandler = new Handler(Looper.getMainLooper());
        Intent intent = getActivity().getIntent();
        // TODO: wrap it, oh well, I suppose if we don't send this a crash is "lolkay"
        mPatientId = intent.getStringExtra("PATIENT_UUID");
        mCollectionItemRawJson = intent.getStringExtra("COLLECTION_ITEM_JSON");
        mCollectionId = intent.getStringExtra("COLLECTION_ID"); // this is not for the item, but for the collection, like instance.collection_name
        mCallingActivityName = intent.getStringExtra("CALLING_ACTIVITY_NAME"); // yup, hacking our dynamic back
        try {
            mCollectionItemJson = new JSONObject(mCollectionItemRawJson);
            mEditMode = true;
        } catch(Exception e){
            mCollectionItemJson = null;
            // AKA later, spin up the appropriate "model" "instance" and use default JSON son
        }
        // if we used an interface we could generic type to the interface and do default logic at the end, waa waaa waa
        mFields = new ArrayList<ModelField>();

        CollectionItem modelInstance = null; // balls

        // TODO: human readable model instance title, lolzone

        if(mCollectionId.equals("addresses")){
            PatientResidence instance = new PatientResidence();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.address1);
            mFields.add(instance.address2);
            mFields.add(instance.city);
            mFields.add(instance.state);
            // mFields.add(instance.stateSelect); // SORRY PEOPLE, we could autocomplete lol, hahahahahahahahahahah
            mFields.add(instance.zip);
            mFields.add(instance.country);
            mFields.add(instance.residenceType);
            mFields.add(instance.lifesquareLocation);
            // mFields.add(instance.lifesquareLocationOther); F THAT SON
            mFields.add(instance.mailingAddress);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("country", "US");
                    mCollectionItemJson.put("privacy", "provider");
                    mCollectionItemJson.put("residence_type", "HOME");// lol
                    mCollectionItemJson.put("lifesquare_location_type", "Other"); // no gating
                } catch(Exception e){

                }
            }
        }
        if(mCollectionId.equals("languages")){
            // not a subclass of collection Item, oh oh do some one-offs in here
            PatientLanguage instance = new PatientLanguage();
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.code);
            mFields.add(instance.proficiency);
            mCollectionName = instance.collectionName;
            // one off son nuts
            if(mCollectionItemJson == null){
                mCollectionItemJson = instance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("language_code", "en");
                    mCollectionItemJson.put("language_proficiency", "NATIVE");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("medications")){
            PatientMedication instance = new PatientMedication();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.medication);
            mFields.add(instance.dose);
            mFields.add(instance.frequency);
            mFields.add(instance.quantity);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("allergies")){
            PatientAllergy instance = new PatientAllergy();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.allergen);
            mFields.add(instance.reaction);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("conditions")){
            PatientCondition instance = new PatientCondition();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.healthEvent);
            mFields.add(instance.startDate);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("procedures")){
            PatientProcedure instance = new PatientProcedure();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.healthEvent);
            mFields.add(instance.startDate);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("immunizations")){
            PatientImmunization instance = new PatientImmunization();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.healthEvent);
            mFields.add(instance.startDate);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("insurances")){
            PatientInsurance instance = new PatientInsurance();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.orgName);
            mFields.add(instance.phone);
            mFields.add(instance.policyCode);
            mFields.add(instance.groupCode);
            mFields.add(instance.firstName);
            mFields.add(instance.lastName);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("care_providers")){
            PatientPhysician instance = new PatientPhysician();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.firstName);
            mFields.add(instance.lastName);
            mFields.add(instance.phone);
            mFields.add(instance.specialization);
            mFields.add(instance.facilityName);
            mFields.add(instance.address1);
            mFields.add(instance.address2);
            mFields.add(instance.city);
            mFields.add(instance.state);
            mFields.add(instance.stateSelect);
            mFields.add(instance.zip);
            mFields.add(instance.country);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("country", "US");
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("hospitals")){
            PatientHospital instance = new PatientHospital();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.name);
            mFields.add(instance.phone);
            mFields.add(instance.address1);
            mFields.add(instance.city);
            mFields.add(instance.state);
            mFields.add(instance.stateSelect);
            mFields.add(instance.zip);
            mFields.add(instance.country);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("country", "US");
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("pharmacies")){
            PatientPharmacy instance = new PatientPharmacy();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.name);
            mFields.add(instance.phone);
            mFields.add(instance.address1);
            mFields.add(instance.city);
            mFields.add(instance.state);
            mFields.add(instance.stateSelect);
            mFields.add(instance.zip);
            mFields.add(instance.country);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("country", "US");
                    mCollectionItemJson.put("privacy", "provider");
                } catch(Exception e){

                }
            }
        }

        if(mCollectionId.equals("emergency")){
            PatientEmergencyContact instance = new PatientEmergencyContact();
            modelInstance = instance;
            mInstanceTitle = titlePrefix() + instance.humanizedName;
            mFields.add(instance.firstName);
            mFields.add(instance.lastName);
            mFields.add(instance.relationship);
            mFields.add(instance.phone);
            mFields.add(instance.email);
            mFields.add(instance.powerOfAttorney);
            mFields.add(instance.nextOfKin);
            mFields.add(instance.privacy);
            if(mCollectionItemJson == null && modelInstance != null){
                mCollectionItemJson = modelInstance.getDefaultJSON();
                try {
                    mCollectionItemJson.put("privacy", "public");
                } catch(Exception e){

                }
            }
        }

        //DRY TOWN USA - population 1
        if(modelInstance != null) {
            mCollectionName = modelInstance.collectionName;
        }
        // revisit this when we have a legit defaultJSON generation
        /*
        if(mCollectionItemJson == null && modelInstance != null){
            mCollectionItemJson = modelInstance.getDefaultJSON();
        }
        */

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Integer fragmentId = 0;
        if(mCollectionId.equals("addresses")){
            fragmentId = R.layout.fragment_edit_patient_residence;
        }
        if(mCollectionId.equals("languages")){
            fragmentId = R.layout.fragment_edit_patient_language;
        }
        if(mCollectionId.equals("medications")){
            fragmentId = R.layout.fragment_edit_patient_medication;
        }
        if(mCollectionId.equals("allergies")){
            fragmentId = R.layout.fragment_edit_patient_allergy;
        }
        if(mCollectionId.equals("conditions")){
            fragmentId = R.layout.fragment_edit_patient_condition;
        }
        if(mCollectionId.equals("procedures")){
            fragmentId = R.layout.fragment_edit_patient_procedure;
        }
        if(mCollectionId.equals("immunizations")){
            fragmentId = R.layout.fragment_edit_patient_immunization;
        }
        if(mCollectionId.equals("insurances")){
            fragmentId = R.layout.fragment_edit_patient_insurance;
        }
        if(mCollectionId.equals("care_providers")){
            fragmentId = R.layout.fragment_edit_patient_physician;
        }
        if(mCollectionId.equals("hospitals")){
            fragmentId = R.layout.fragment_edit_patient_hospital;
        }
        if(mCollectionId.equals("pharmacies")){
            fragmentId = R.layout.fragment_edit_patient_pharmacy;
        }
        if(mCollectionId.equals("emergency")){
            fragmentId = R.layout.fragment_edit_patient_emergency;
        }
        // only if we have a match, bwaaa
        mRootView = inflater.inflate(fragmentId, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mCollectionItemJson != null){
            // ok, this case should be valid 100% of the time though
            Forms.initForm(getContext(), mRootView, mFields, mCollectionItemJson);
            // do hooks on setup plugins and all that and a bag of chips
        }

        // TODO: event binding
        Button deleteButton = (Button) mRootView.findViewById(R.id.btDeleteCollectionItem);
        if(mEditMode) {

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    promptDeleteCollectionItem();
                }

                ;
            });
        } else {
            deleteButton.setVisibility(View.GONE);
        }
        // capture the back and hook the dirty check

        // bind the clicks on da autocomplete fields son
        for(ModelField object : mFields) {
            if(object.formControl.equals("datepicker")){


                EditText et = (EditText) mRootView.findViewById(object.fieldId);
                et.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RegistrationDOBPickerFragment newFragment = new RegistrationDOBPickerFragment();
                        // blablbla, public attributes, since this is "OUR THING", no need to hack intents and bundle nuts
                        newFragment.mAttribute = object.attribute;

                        // too much logic up in this bitch
                        try {
                            // THIS IS REASONABLY REDUNDANT AS HELL
                            // TODO: optimize that shee
                            if(!mCollectionItemJson.isNull(object.attribute)){
                                String initialValue = mCollectionItemJson.getString(object.attribute);
                                // is it a vanilla date in "server format"
                                if(String.valueOf(initialValue.charAt(4)).equals("-")){
                                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                                    Date date = (Date) format.parse(initialValue);
                                    Format formatter = new SimpleDateFormat("MM/dd/yyyy");
                                    initialValue = formatter.format(date);
                                } else {
                                    // or is it a dirty date in "client format"
                                    // just send that shee on through the pipe
                                }
                                Bundle bundle = new Bundle();
                                bundle.putString("INITIAL_DATE", initialValue);
                                newFragment.setArguments(bundle);
                            }



                        } catch(Exception e){
                            Logcat.d("date conversion failed son");
                        }


                        // send existing value over the pipes
                        // existing value gonna be a String son
                        // showTermPicker(object);
                        newFragment.show(getFragmentManager(), "datePicker");
                    }
                });



            }
        }

        // plugin for therapy change and initial value though
        // presumably we wire up a default change event somewhere deep town down town
        if(mCollectionId.equals("medications")) {
            // check dat der existing value son
            // double meh on deeze nuts
            try {
                String initialValue = mCollectionItemJson.getString("therapy");
                if(initialValue != null && !initialValue.equals("")){
                    queryMedicationDose(initialValue);
                }
            } catch(Exception e){
                // nothing to say here son
            }
        }

        // set title now son, lololololzor
        try {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            actionBar.setTitle(mInstanceTitle);
        } catch(Exception e){
            Logcat.d("what the " + e.toString());
        }
        // ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(mInstanceTitle);
    }

    private String titlePrefix(){
        if(mEditMode){
            return "Edit ";
        }else {
            return "Add ";
        }
    }

    private void populateMedicationDose(JSONArray routes){
        // then make that shee a LinkedHashMap
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        for(int i = 0;i<routes.length();i++){
            try {
                String item = routes.get(i).toString();
                values.put(item, item);
            } catch(Exception e){

            }
        }
        // fill her up because we know the hardcoded location of all this here shee shee
        Spinner spinner = (Spinner) mRootView.findViewById(R.id.spPatientMedicationDose);
        LinkedHashMapAdapter<String, String> spinnerAdaptor = new LinkedHashMapAdapter<String, String>(getContext(),
                R.layout.spinner_item, values);
        spinner.setAdapter(spinnerAdaptor);
        // spinnerAdaptor.notifyDataSetChanged();
        // spinner.getAdapter().addAll
        // balls son
    }

    private void queryMedicationDose(String medicationName){
        // ok, we don't even care about da field, since we know the one condition it exists in
        HealthNotifierAPI.getInstance().medicationDose(medicationName, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Error getting dose :(", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    // read dem body son

                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray routes = json.getJSONArray("routes");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // pass back dem doses my brosef
                                populateMedicationDose(routes);
                            }
                        });
                    } catch(Exception e){
                        // you be f'd son, just FML already
                    }

                } else {
                    Logcat.d( "onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "Error getting doses :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_collection_item, menu);
        // mMenu = menu; I guess this is so we can config it later???
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // switch on the mCallingActivityName
                // yup
                // TODO: intercept and check for DIRTY just like iOS since we're aborting, this is all we need
                // to work approximately like iOS we need to have 100% form UI coverage for onChange
                getActivity().finish(); // lazy town USA
                return true;
            case R.id.action_save_collection_item:
                // promptDelete();
                saveCollectionItem();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showTermPicker(ModelField field){
        // yea balls on that son
        // pimp the existing values son
        String value = null;
        try {
            value = mCollectionItemJson.getString(field.attribute);
        } catch(Exception e){

        }
        AutocompleteTermPickerFragment newFragment = new AutocompleteTermPickerFragment();
        newFragment.mAutocompleteId = field.autocompleteId;
        newFragment.mPatientId = mPatientId;
        if(value != null) {
            // LOL TOO LAZY TO USE THE BUNDL SON
            newFragment.mInitialValue = value;
            Bundle bundle = new Bundle();
            bundle.putString("INITIAL_VALUE", value);
            newFragment.setArguments(bundle);
        }

        newFragment.show(getFragmentManager(), "termsPicker");
    }

    private void promptDeleteCollectionItem(){
        AlertDialog alertDialog = new AlertDialog.Builder(this.getContext()).create();
        alertDialog.setTitle("Delete Item?");
        // alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        deleteCollectionItem();
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

    private void deleteCollectionItem(){
        try {
            mCollectionItemJson.put("_destroy", true);
            HealthNotifierAPI.getInstance().updateCollection(mPatientId, mCollectionName, mCollectionItemJson, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // TODO: hook default offline handler
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) { // exactly what status codes determine this?
                        try {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    finishWithIntent(true);
                                }
                            });
                        } catch (Exception e) {

                        }
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Error deleting", Toast.LENGTH_SHORT).show();
                            }
                        });
                        Logcat.d( "onResponseError" + response.toString());
                    }
                }
            });
        } catch(Exception e){

        }
    }

    private void saveCollectionItem(){
        // run dat validator
        FormValidation validationResults = Forms.validateForm(mRootView, mFields);

        if(validationResults.isValid()){
            try {
                JSONObject collectionNode = Forms.populateJson(mRootView, mFields, mCollectionItemJson);

                HealthNotifierAPI.getInstance().updateCollection(mPatientId, mCollectionName, collectionNode, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // TODO: hook offline handler
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    finishWithIntent(false);
                                }
                            });
                        } else {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), "Error saving", Toast.LENGTH_SHORT).show();
                                }
                            });
                            Logcat.d( "onResponseError" + response.toString());
                        }
                    }
                });


            } catch (Exception e){

            }
        } else {
            Forms.defaultDisplayFormValidation(mRootView, getContext(), validationResults);
        }
    }

    private void finishWithIntent(Boolean deleted){
        Intent intent = null;
        // if we can't find the intent, go all the way back to the Patient screen, lol
        switch(mCallingActivityName){
            case "com.healthnotifier.healthnotifier.activity.EditContactsActivity":
                intent = new Intent(getContext(), EditContactsActivity.class);
                break;
            case "com.healthnotifier.healthnotifier.activity.EditMedicalActivity":
                intent = new Intent(getContext(), EditMedicalActivity.class);
                break;
            case "com.healthnotifier.healthnotifier.activity.EditProfileActivity":
                intent = new Intent(getContext(), EditProfileActivity.class);
                break;
            case "com.healthnotifier.healthnotifier.activity.EditEmergencyActivity":
                intent = new Intent(getContext(), EditEmergencyActivity.class);
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
            intent.putExtra("COLLECTION_NAME", mCollectionName); // in the future, we should remove said dependency
            intent.putExtra("PATIENT_ID", mPatientId);
            startActivity(intent);
        } else {
            Logcat.d("FAILED AT FINDING THE CALLING ACTIVITY");
            getActivity().finish();
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

    // GHETTO TOWN PART DOS
    @Subscribe
    public void handleGenericEvents(GenericEvent event) {
        if (event.eventName.equals("ModelFormChange") && event.attributes != null) {
            String modelAttribute = event.attributes.get("Attribute").toString();
            if(modelAttribute.equals("therapy")){
                queryMedicationDose(event.attributes.get("Value").toString());
            }
        }
        // ghetto double pumps
        if (event.eventName.equals("onDOBSet") && event.attributes != null) {
            try {
                // conver date back to string? ish, double donkey pumps
                Format formatter = new SimpleDateFormat("MM/dd/yyyy");
                Date d = (Date) event.attributes.get("Date");
                String attribute = event.attributes.get("Attribute").toString();
                mCollectionItemJson.put(attribute, formatter.format(d));

                // TODO: update the text input to have the "toString" version son
                for(ModelField object : mFields) {
                    if(object.attribute.equals(attribute)){
                        ((EditText) mRootView.findViewById(object.fieldId)).setText(formatter.format(d));
                        break;
                    }
                }

            } catch(Exception e){

            }
        }
    }

}
