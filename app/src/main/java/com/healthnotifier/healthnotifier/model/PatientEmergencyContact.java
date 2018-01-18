package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientEmergencyContact extends CollectionItem {
    public ModelField firstName;
    public ModelField lastName;
    public ModelField relationship;
    public ModelField phone;
    public ModelField email;
    public ModelField powerOfAttorney;
    public ModelField nextOfKin;

    public PatientEmergencyContact(){
        this.collectionName = "patient_contacts";
        // default value for privacy

        this.firstName = new ModelField();
        this.firstName.attribute = "first_name";
        this.firstName.label = "First Name";
        this.firstName.isRequired = true;
        this.firstName.fieldId = R.id.etPatientEmergencyFirstName;

        this.lastName = new ModelField();
        this.lastName.attribute = "last_name";
        this.lastName.label = "Last Name";
        this.lastName.isRequired = true;
        this.lastName.fieldId = R.id.etPatientEmergencyLastName;

        this.relationship = new ModelField();
        this.relationship.attribute = "contact_relationship";
        this.relationship.label = "Relationship";
        this.relationship.isRequired = true;
        this.relationship.formControl = "select";
        this.relationship.values = HealthNotifierAPI.getInstance().getValues("patient_contact", "relationship");
        this.relationship.fieldId = R.id.spPatientEmergencyRelationship;

        this.phone = new ModelField();
        this.phone.attribute = "home_phone";
        this.phone.label = "Phone Number";
        this.phone.isRequired = true;
        this.phone.fieldId = R.id.etPatientEmergencyPhone;
        // this.phone.keyboard = UIKeyboardType.PhonePad; this is in the View XML anyhow son

        this.email = new ModelField();
        this.email.attribute = "email";
        this.email.label = "Email";
        this.email.fieldId = R.id.etPatientEmergencyEmail;

        this.powerOfAttorney = new ModelField();
        this.powerOfAttorney.attribute = "power_of_attorney";
        this.powerOfAttorney.label = "Durable health care Power of Attorney";
        this.powerOfAttorney.formControl = "checkbox";
        this.powerOfAttorney.dataType = "boolean";
        this.powerOfAttorney.fieldId = R.id.cbPatientEmergencyPowerOfAttorney;

        this.nextOfKin = new ModelField();
        this.nextOfKin.attribute = "next_of_kin";
        this.nextOfKin.label = "Next of Kin";
        this.nextOfKin.formControl = "checkbox";
        this.nextOfKin.dataType = "boolean";
        this.nextOfKin.fieldId = R.id.cbPatientEmergencyNextOfKin;

        this.privacy.fieldId = R.id.spPatientEmergencyPrivacy;

        this.humanizedName = "Contact";

    }

}
