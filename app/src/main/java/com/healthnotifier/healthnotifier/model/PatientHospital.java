package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

import org.json.JSONObject;

/**
 * Created by charles on 1/30/17.
 */

public class PatientHospital extends CollectionItem {
    public ModelField name;
    public ModelField phone;
    public ModelField address1;
    public ModelField city;
    public ModelField state;
    public ModelField stateSelect;
    public ModelField zip;
    public ModelField country;

    public PatientHospital(){
        this.collectionName = "patient_medical_facilities";

        this.name = new ModelField();
        this.name.attribute = "name";
        this.name.label = "Name";
        this.name.isRequired = true;
        this.name.fieldId = R.id.etPatientHospitalName;

        this.phone = new ModelField();
        this.phone.attribute = "phone";
        this.phone.label = "Phone Number";
        this.phone.fieldId = R.id.etPatientHospitalPhone;
        //this.phone.keyboard = UIKeyboardType.PhonePad;

        this.address1 = new ModelField();
        this.address1.attribute = "address_line1";
        this.address1.label = "Street Address";
        this.address1.fieldId = R.id.etPatientResidenceAddress1;

        this.city = new ModelField();
        this.city.attribute = "city";
        this.city.label = "City";
        this.city.fieldId = R.id.etPatientResidenceCity;

        this.stateSelect = new ModelField();
        this.stateSelect.attribute = "state_province";
        this.stateSelect.label = "State / Province";
        this.stateSelect.formControl = "select";
        this.stateSelect.isRequired = false;
        this.stateSelect.values = HealthNotifierAPI.getInstance().getValues("state");
        this.stateSelect.fieldId = R.id.spPatientHospitalState;

        this.state = new ModelField();
        this.state.attribute = "state_province";
        this.state.label = "State";
        this.state.isRequired = false;
        this.state.fieldId = R.id.etPatientHospitalState;

        this.zip = new ModelField();
        this.zip.attribute = "postal_code";
        this.zip.label = "Postal Code";
        this.zip.fieldId = R.id.etPatientHospitalZip;

        this.country = new ModelField();
        this.country.attribute = "country";
        this.country.label = "Country";
        this.country.formControl = "select";
        this.country.isRequired = false;
        this.country.values = HealthNotifierAPI.getInstance().getValues("country");
        this.country.fieldId = R.id.spPatientHospitalCountry;

        this.privacy.fieldId = R.id.spPatientHospitalPrivacy;

        this.humanizedName = "Hospital";
        
    }

    @Override
    public JSONObject getDefaultJSON(){
        JSONObject json = super.getDefaultJSON();
        try {
            json.put("medical_facility_type", "HOSPITAL");
        } catch(Exception e){
            // donkey stains
        }
        return json;
    }

}
