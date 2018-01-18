package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientResidence extends CollectionItem {

    public ModelField address1;
    public ModelField address2;
    public ModelField city;
    public ModelField state; // the raw string input
    public ModelField stateSelect;
    public ModelField zip;
    public ModelField country;
    public ModelField residenceType;
    public ModelField lifesquareLocation;
    public ModelField lifesquareLocationOther;
    public ModelField mailingAddress;

    public PatientResidence() {
        // LOLOLOLOLOLOLOLOLOLOL MODEL FIELD KNOWING THE ID OF THE VIEW ELEMENT
        // super();
        // PRICELESS
        this.address1 = new ModelField();
        this.address1.attribute = "address_line1";
        this.address1.label = "Street Address";
        this.address1.isRequired = true;
        this.address1.fieldId = R.id.etPatientResidenceAddress1;

        this.address2 = new ModelField();
        this.address2.attribute = "address_line2";
        this.address2.label = "Address 2";
        this.address2.fieldId = R.id.etPatientResidenceAddress2;

        this.city = new ModelField();
        this.city.attribute = "city";
        this.city.label = "City";
        this.city.isRequired = true;
        this.city.fieldId = R.id.etPatientResidenceCity;

        this.stateSelect = new ModelField();
        this.stateSelect.attribute = "state_province";
        this.stateSelect.label = "State / Province";
        this.stateSelect.formControl = "select";
        this.stateSelect.isRequired = true;
        this.stateSelect.values = HealthNotifierAPI.getInstance().getValues("state"); // currently just US son;
        this.stateSelect.fieldId = R.id.spPatientResidenceState;

        this.state = new ModelField();
        this.state.attribute = "state_province";
        this.state.label = "State / Province";
        this.state.isRequired = true;
        this.state.fieldId = R.id.etPatientResidenceState;

        this.country = new ModelField();
        this.country.attribute = "country";
        this.country.label = "Country";
        this.country.formControl = "select";
        this.country.isRequired = true;
        this.country.values = HealthNotifierAPI.getInstance().getValues("country");
        this.country.fieldId = R.id.spPatientResidenceCountry;

        this.zip = new ModelField();
        this.zip.attribute = "postal_code";
        this.zip.label = "Postal Code";
        this.zip.isRequired = true;
        this.zip.fieldId = R.id.etPatientResidenceZip;

        this.residenceType = new ModelField();
        this.residenceType.attribute = "residence_type";
        this.residenceType.label = "Type";
        this.residenceType.formControl = "select";
        this.residenceType.isRequired = true;
        this.residenceType.values = HealthNotifierAPI.getInstance().getValues("patient_residence", "residence_type");
        this.residenceType.fieldId = R.id.spPatientResidenceResidenceType;

        this.lifesquareLocation = new ModelField();
        this.lifesquareLocation.attribute = "lifesquare_location_type";
        this.lifesquareLocation.label = "LifeSticker Location";
        this.lifesquareLocation.formControl = "select";
        this.lifesquareLocation.isRequired = true;
        this.lifesquareLocation.values = HealthNotifierAPI.getInstance().getValues("patient_residence", "lifesquare_location_type");
        this.lifesquareLocation.fieldId = R.id.spPatientResidenceLifesquareLocation;

        this.lifesquareLocationOther = new ModelField();
        this.lifesquareLocationOther.attribute = "lifesquare_location_other";
        this.lifesquareLocationOther.label = "Location";
        this.lifesquareLocationOther.fieldId = R.id.etPatientResidenceLifesquareLocationOther;

        this.mailingAddress = new ModelField();
        this.mailingAddress.attribute = "mailing_address";
        this.mailingAddress.label = "Mailing Address";
        this.mailingAddress.formControl = "checkbox";
        this.mailingAddress.dataType = "boolean";
        this.mailingAddress.fieldId = R.id.cbPatientResidenceMailingAddress;

        // we'll see how this works out
        this.privacy.fieldId = R.id.spPatientResidencePrivacy;

        this.collectionName = "patient_residences";

        this.humanizedName = "Address";
    }

}
