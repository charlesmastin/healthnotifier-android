package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientPharmacy extends CollectionItem {
    public ModelField name;
    public ModelField phone;
    public ModelField address1;
    public ModelField city;
    public ModelField state;
    public ModelField stateSelect;
    public ModelField zip;
    public ModelField country;

    public PatientPharmacy(){
        this.collectionName = "patient_pharmacies";

        this.name = new ModelField();
        this.name.attribute = "name";
        this.name.label = "Name";
        this.name.isRequired = true;
        this.name.fieldId = R.id.etPatientPharmacyName;

        this.phone = new ModelField();
        this.phone.attribute = "phone";
        this.phone.label = "Phone Number";
        this.phone.fieldId = R.id.etPatientPharmacyPhone;
        //this.phone.keyboard = UIKeyboardType.PhonePad;

        this.address1 = new ModelField();
        this.address1.attribute = "address_line1";
        this.address1.label = "Street Address";
        this.address1.fieldId = R.id.etPatientPharmacyAddress1;

        this.city = new ModelField();
        this.city.attribute = "city";
        this.city.label = "City";
        this.city.fieldId = R.id.etPatientPharmacyCity;

        this.stateSelect = new ModelField();
        this.stateSelect.attribute = "state_province";
        this.stateSelect.label = "State / Province";
        this.stateSelect.formControl = "select";
        this.stateSelect.isRequired = false;
        this.stateSelect.values = HealthNotifierAPI.getInstance().getValues("state");
        this.stateSelect.fieldId = R.id.spPatientPharmacyState;

        this.state = new ModelField();
        this.state.attribute = "state_province";
        this.state.label = "State";
        this.state.isRequired = false;
        this.state.fieldId = R.id.etPatientPharmacyState;

        this.zip = new ModelField();
        this.zip.attribute = "postal_code";
        this.zip.label = "Postal Code";
        this.zip.fieldId = R.id.etPatientPharmacyZip;

        this.country = new ModelField();
        this.country.attribute = "country";
        this.country.label = "Country";
        this.country.formControl = "select";
        this.country.isRequired = false;
        this.country.values = HealthNotifierAPI.getInstance().getValues("country");
        this.country.fieldId = R.id.spPatientPharmacyCountry;

        this.privacy.fieldId = R.id.spPatientPharmacyPrivacy;

        this.humanizedName = "Pharmacy";

    }

}
