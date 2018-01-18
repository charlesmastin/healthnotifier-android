package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientPhysician extends CollectionItem {

    public ModelField facilityName;
    public ModelField specialization;
    public ModelField address1;
    public ModelField address2;
    public ModelField city;
    public ModelField state;
    public ModelField stateSelect;
    public ModelField zip;
    public ModelField country;
    public ModelField phone;
    public ModelField firstName;
    public ModelField lastName;

    public PatientPhysician(){
        this.collectionName = "patient_care_providers";

        this.facilityName = new ModelField();
        this.facilityName.attribute = "medical_facility_name";
        this.facilityName.label = "Facility Name";
        this.facilityName.fieldId = R.id.etPatientPhysicianFacilityName;

        this.specialization = new ModelField();
        this.specialization.attribute = "care_provider_class";
        this.specialization.label = "Specialization";
        this.specialization.formControl = "select";
        this.specialization.values = HealthNotifierAPI.getInstance().getValues("patient_care_provider", "care_provider_class");
        this.specialization.fieldId = R.id.spPatientPhysicianSpecialization;

        this.phone = new ModelField();
        this.phone.attribute = "phone1";
        this.phone.label = "Phone Number";
        this.phone.fieldId = R.id.etPatientPhysicianPhone;
        //this.phone.keyboard = UIKeyboardType.PhonePad;

        this.address1 = new ModelField();
        this.address1.attribute = "address_line1";
        this.address1.label = "Street Address";
        this.address1.fieldId = R.id.etPatientPhysicianAddress1;

        this.address2 = new ModelField();
        this.address2.attribute = "address_line2";
        this.address2.label = "Suite";
        this.address2.fieldId = R.id.etPatientPhysicianAddress2;

        this.city = new ModelField();
        this.city.attribute = "city";
        this.city.label = "City";
        this.city.fieldId = R.id.etPatientPhysicianCity;

        this.stateSelect = new ModelField();
        this.stateSelect.attribute = "state_province";
        this.stateSelect.label = "State / Province";
        this.stateSelect.formControl = "select";
        this.stateSelect.isRequired = false;
        this.stateSelect.values = HealthNotifierAPI.getInstance().getValues("state"); // currently just US son;
        this.stateSelect.fieldId = R.id.spPatientPhysicianState;

        this.state = new ModelField();
        this.state.attribute = "state_province";
        this.state.label = "State / Province";
        this.state.isRequired = false;
        this.state.fieldId = R.id.etPatientPhysicianState;

        this.zip = new ModelField();
        this.zip.attribute = "postal_code";
        this.zip.label = "Postal Code";
        this.zip.fieldId = R.id.etPatientPhysicianZip;

        this.country = new ModelField();
        this.country.attribute = "country";
        this.country.label = "Country";
        this.country.formControl = "select";
        this.country.isRequired = false;
        this.country.values = HealthNotifierAPI.getInstance().getValues("country");
        this.country.fieldId = R.id.spPatientPhysicianCountry;

        this.firstName = new ModelField();
        this.firstName.attribute = "first_name";
        this.firstName.label = "First Name";
        this.firstName.fieldId = R.id.etPatientPhysicianFirstName;
        
        this.lastName = new ModelField();
        this.lastName.attribute = "last_name";
        this.lastName.label = "Last Name";
        this.lastName.isRequired = true;
        this.lastName.fieldId = R.id.etPatientPhysicianLastName;

        this.privacy.fieldId = R.id.spPatientPhysicianPrivacy;

        this.humanizedName = "Physician";

    }

}
