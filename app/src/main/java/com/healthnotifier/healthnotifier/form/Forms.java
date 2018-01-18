package com.healthnotifier.healthnotifier.form;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.adapter.TermAutoCompleteAdapter;
import com.healthnotifier.healthnotifier.model.ModelField;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.adapter.LinkedHashMapAdapter;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.squareup.otto.Bus;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// throwing code into the fire
public class Forms {

    public static void initForm(Context context, View view, ArrayList<ModelField> fields, JSONObject json) {
        // initial beast mode
        // TODO: across the board generic change event delegation, yea son, mop it up style
        for(ModelField object : fields) {
            // TODO: value coming out may not be string always

            if (object.formControl.equals("text")){
                String initialValue = "";
                if(object.dataType.equals("string")) {
                    try {
                        if(!json.isNull(object.attribute)){
                            initialValue = json.getString(object.attribute);
                        }
                    } catch(Exception e){
                        Logcat.d( "null on your nuts");
                    }
                }
                if(object.dataType.equals("integer")) {
                    try {
                        initialValue = Integer.toString(json.getInt(object.attribute));

                    } catch(Exception e){
                    }
                }
                if(object.dataType.equals("double")) {
                    try {
                        initialValue = Double.toString(json.getDouble(object.attribute));
                    } catch(Exception e){
                    }
                }
                if(object.dataType.equals("float")) {
                    try {
                        initialValue = Double.toString(json.getDouble(object.attribute));
                    } catch(Exception e){
                    }
                }
                EditText et = (EditText) view.findViewById(object.fieldId);
                if(et != null){
                    et.setText(initialValue);
                }else{
                    Logcat.d( "EditText Not Found!!!" + object.fieldId);
                }
            }

            if (object.formControl.equals("select")) {
                Spinner spinner = (Spinner) view.findViewById(object.fieldId);
                LinkedHashMapAdapter<String, String> spinnerAdaptor = new LinkedHashMapAdapter<String, String>(view.getContext(),
                        R.layout.spinner_item, object.values);
                spinner.setAdapter(spinnerAdaptor);
                // object.spinner = spinner;

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // lol bro
                        try {
                            Bus bus = HealthNotifierApplication.bus;
                            Map<String, Object> e = new HashMap<String, Object>();
                            e.put("Attribute", object.attribute);
                            e.put("Value", spinnerAdaptor.getItem(position).getKey());
                            bus.post(new GenericEvent("ModelFormChange", e));
                        } catch (Exception e) {
                            Logcat.d(object.attribute + " - OUR SELECT AND OUR ADAPTER ARE OUT OF SYNC");
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                // pull this out into a external interface, perhaps, yea son
                String initialValue = "";
                if(object.dataType.equals("string")) {
                    try {
                        initialValue = json.getString(object.attribute);
                        Forms.populateHashedSpinner(spinner, initialValue);
                    } catch(Exception e) {
                    }
                }
            }

            if (object.formControl.equals("checkbox")) {
                CheckBox checkbox = (CheckBox) view.findViewById(object.fieldId);
                // boolean on deeze nuts
                Boolean initialValue = false;
                // TODO: also check the default value of the model field definition
                try {
                    initialValue = json.getBoolean(object.attribute);
                } catch(Exception e){

                }
                checkbox.setChecked(initialValue);
            }

            if (object.formControl.equals("autocomplete")){
                // TODO: do the adapter build out here, when we refactor this method to be initForm vs populateFOrm
                AutoCompleteTextView ac = (AutoCompleteTextView) view.findViewById(object.fieldId);


                // TODO: populate initial string son! do it in the Forms.initForm method son, this is just the binding, but we should be moving that
                // work dat into a refactor on initialize form though son
                ac.setThreshold(2);
                // OK THIS IS 100% tightly coupled to the "terms" but realistically, we need it dynamic in the future to be
                // any data source adapter, if that makes sense
                // thus the key value bit of the "selected" item, needs standardization
                ac.setAdapter(new TermAutoCompleteAdapter(context, new ArrayList<JSONObject>(), object.autocompleteId));
                // loading indicator
                ac.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                        try {
                            JSONObject item = (JSONObject) adapterView.getItemAtPosition(position);
                            ac.setText(item.getString("title"));


                            Bus bus = HealthNotifierApplication.bus;
                            Map<String, Object> e = new HashMap<String, Object>();
                            e.put("Attribute", object.attribute);
                            e.put("Value", item.getString("title"));
                            bus.post(new GenericEvent("ModelFormChange", e));

                            // for now, this will "get it done" by scraping the well-formed text on the way out
                            // TODO: persist the value back into the model though son
                            // and if we want to "locally store" the value, we need to subclass this bitch (AutoCompleteTExtView)
                            // or we need to access the adapter or something in to form value serialization, that sounds dicey
                            // since we really want to discard and past "results" when hiding the widget

                        } catch(Exception e){
                            // f you
                        }
                    }
                });

                String initialValue = "";
                if(object.dataType.equals("string")) {
                    try {
                        if(!json.isNull(object.attribute)){
                            initialValue = json.getString(object.attribute);
                        }
                    } catch(Exception e){
                        Logcat.d( "null on your nuts");
                    }
                }
                if(!initialValue.equals("")){
                    ac.setText(initialValue);
                }
            }

            // date
            if(object.formControl.equals("datepicker")){
                String initialValue = "";
                if(object.dataType.equals("string")) {
                    try {
                        if(!json.isNull(object.attribute)){
                            initialValue = json.getString(object.attribute);
                        }
                    } catch(Exception e){
                        Logcat.d( "null on your nuts");
                    }
                }
                if(!initialValue.equals("")){
                    try {
                        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        Date date = (Date) format.parse(initialValue);
                        // the other formatter, back lol son
                        Format formatter = new SimpleDateFormat("MM/dd/yyyy");
                        initialValue = formatter.format(date);
                    } catch(Exception e){
                        Logcat.d("date conversion failed son");
                    }

                    // LOL convert the date format
                    // first make a date
                    // then back to string lolzone
                    ((EditText) view.findViewById(object.fieldId)).setText(initialValue);
                }
                // wire up dat click son
                // at least we can pre-populate until we can figure out the other sthizzle
                /*
                EditText et = (EditText) view.findViewById(object.fieldId);
                et.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
                */
            }

            // height
        }
    }

    public static void populateHashedSpinner(Spinner spinner, String value){
        LinkedHashMapAdapter<String, String> spinnerAdaptor = (LinkedHashMapAdapter<String, String>) spinner.getAdapter();
        for(int i=0; i < spinnerAdaptor.getCount(); i++) {
            if(value.trim().equals(spinnerAdaptor.getItem(i).getKey())){
                spinner.setSelection(i);
                break;
            }
        }
    }

    public static FormValidation validateForm(View view, ArrayList<ModelField> fields){
        FormValidation validator = new FormValidation();
        // iterate the fields, and add dem to the
        // validate against, the raw inputs? w/e
        // or against the JSON? w/e
        // consult iOS code so we can do it one way (ish)
        // easiest for sure, is to already run populateJson first (working on a copy or something)
        // then we just do simple checks to the JSON, don't have to care about the unique form controls
        // FML
        for(ModelField object : fields) {
            if(object.isRequired){
                // switch on type of field
                if (object.formControl.equals("text")){
                    // is not null
                    if(object.dataType.equals("string")) {
                        String value = ((EditText) view.findViewById(object.fieldId)).getText().toString();
                        Logcat.d( object.attribute + ':' + value);
                        if(value != null && value.length() >= 1){
                            // pass son
                        } else {
                            FieldValidation fieldError = new FieldValidation();
                            fieldError.field = object;
                            fieldError.message = object.getLabel() + " is required!";
                            validator.errors.add(fieldError);
                        }
                    }
                    // numbers and stuffs son
                }
                if (object.formControl.equals("select")){
                    // is not null - holy nuts gorillas bannana
                    // TODO: HUGE ASS ASSUMPTION about nullable 0 index
                    // TODO: not sure what to do
                    // TODO: naturally if we looked at the assigned JSON, we could avoid this hack at least here
                    Spinner spinner = (Spinner) view.findViewById(object.fieldId);
                    // OH BOY, the only reason this works is because there is no such thing as required and nullable
                    // however, it still assumes we have nullable visual UI to default an initial selection
                    // as opposed to getting the value and comparing against null

                    if(spinner.getSelectedItem() != null && spinner.getSelectedItemPosition() > 0){

                    } else {
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.field = object;
                        fieldError.message = object.getLabel() + " must be selected!";
                        validator.errors.add(fieldError);
                    }
                }
                if (object.formControl.equals("checkbox")){
                    // is true
                    if(((CheckBox) view.findViewById(object.fieldId)).isChecked()){

                    } else {
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.field = object;
                        fieldError.message = object.getLabel() + " must be checked!";
                        validator.errors.add(fieldError);
                    }

                }

                if (object.formControl.equals("autocomplete")){
                    // ok donkey piles, it's all gonna be strings
                    AutoCompleteTextView ac = (AutoCompleteTextView) view.findViewById(object.fieldId);
                    // the widget only supports String anyhow
                    String value = ac.getText().toString();
                    if(value != null && value.length() >= 1){
                        // pass son
                    } else {
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.field = object;
                        fieldError.message = object.getLabel() + " is required!";
                        validator.errors.add(fieldError);
                    }
                }

                if (object.formControl.equals("datepicker")){
                    // cheap check times, vs say, checking for null on the json blablablabl
                    String value = ((EditText) view.findViewById(object.fieldId)).getText().toString();
                    if(value.equals("")){
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.field = object;
                        fieldError.message = object.getLabel() + " is required!";
                        validator.errors.add(fieldError);
                    }
                }
                // are we not null basically
                // so doing things that are "plugins" and synthesized back to the class are tricky
                // e.g. height
                // dob, but we can one off these bitches in the calling class, woo son
            }
            // do the general validations here
            // email
            //if(Validators.isValidEmail()
            // phone
            //if(Validators.isValidPhone())
            // integer (keyboard jimmy tricks)
            // double (sim)
            // float (sim)
            // int (max)
            // int (min)
        }
        return validator;
    }

    public static JSONObject populateJson(View view, ArrayList<ModelField> fields, JSONObject json) {
        for(ModelField object : fields) {
            // TODO: value coming out may not be string always
            if (object.formControl.equals("text")){
                if(object.dataType.equals("string")) {
                    try {
                        json.put(object.attribute, ((EditText) view.findViewById(object.fieldId)).getText().toString());
                    } catch(Exception e){

                    }
                }
                if(object.dataType.equals("integer")) {
                    try {
                        json.put(object.attribute, Integer.valueOf(((EditText) view.findViewById(object.fieldId)).getText().toString()));
                    } catch(Exception e){

                    }
                }
                if(object.dataType.equals("double") || object.dataType.equals("float")) {
                    try {
                        json.put(object.attribute, Double.valueOf(((EditText) view.findViewById(object.fieldId)).getText().toString()));
                    } catch(Exception e){

                    }
                }
            }
            if (object.formControl.equals("select")){
                // vanilla case values binding only
                if(object.dataType.equals("string")) {
                    try {
                        // http://stackoverflow.com/questions/5424841/whats-the-correct-way-to-implement-key-value-pair-in-spinner-in-android
                        // https://github.com/ayvazj/hashadapter-android/blob/master/app/src/main/java/com/github/ayvazj/hashadapter/sample/MainActivity.java#L130
                        // yes this supports nulling it out son!
                        // TODO: use the modelfield definition to determing if empty string or nullable, lol town central
                        Map.Entry<String, String> item = (Map.Entry<String, String>) ((Spinner) view.findViewById(object.fieldId)).getSelectedItem();
                        if(item.getKey() == null){
                            json.put(object.attribute, JSONObject.NULL);
                        }else {
                            json.put(object.attribute, item.getKey());
                        }
                    } catch(Exception e){
                        // probs need to access the hashballs here
                        Logcat.d( e.toString());
                    }
                }
                // currently not supporting other things here
            }
            if (object.formControl.equals("autocomplete")){
                // TODO: magic attributes IMO, ICD9, ICD10, etc
                // handle on a per model basis with a custom callback me thinks
                // not critical for the time being, as this well-formed text is restorable on the server later
                // ALWAYS A STRING BUT WHATVER SON, protect these nuts from crashing down
                if(object.dataType.equals("string")) {
                    try {
                        json.put(object.attribute, ((AutoCompleteTextView) view.findViewById(object.fieldId)).getText().toString());
                    } catch(Exception e){

                    }
                }

            }

            if (object.formControl.equals("checkbox")){
                if(object.dataType.equals("boolean")) {
                    try {
                        json.put(object.attribute, ((CheckBox) view.findViewById(object.fieldId)).isChecked());
                    } catch(Exception e){
                        // probs need to access the hashballs here
                        Logcat.d( e.toString());
                    }
                }
            }
        }
        return json;
    }

    public static void defaultDisplayFormValidation(View view, Context context, FormValidation validator){
        // exactly what it says
        String message = "";
        for(FieldValidation fieldError : validator.errors) {
            message += fieldError.message + "\n";
        }
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle("Validation Errors");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}