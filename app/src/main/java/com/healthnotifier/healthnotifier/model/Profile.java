package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class Profile extends Model {

    public ModelField firstName;
    public ModelField middleName;
    public ModelField lastName;
    public ModelField suffix;
    public ModelField birthdate;
    public ModelField organDonor;
    public ModelField discoverable;
    public ModelField demographicsPrivacy;
    public ModelField gender;
    public ModelField ethnicity;
    public ModelField biometricsPrivacy;
    public ModelField height;
    public ModelField weight;
    public ModelField bloodType;
    public ModelField bpSystolic;
    public ModelField bpDiastolic;
    public ModelField pulse;
    public ModelField hairColor;
    public ModelField eyeColor;

    //
    // lol pass in json hahahahaha hahaha hah aha
    public Profile() {

        this.firstName = new ModelField();
        this.firstName.fieldId = R.id.etProfileFirstName;
        this.firstName.attribute = "first_name";
        this.firstName.isRequired = true;

        this.middleName = new ModelField();
        this.middleName.fieldId = R.id.etProfileMiddleName;
        this.middleName.attribute = "middle_name";

        this.lastName = new ModelField();
        this.lastName.fieldId = R.id.etProfileLastName;
        this.lastName.attribute = "last_name";
        this.lastName.isRequired = true;

        this.suffix = new ModelField();
        this.suffix.fieldId = R.id.etProfileSuffix;
        this.suffix.attribute = "suffix";

        // hack town
        this.birthdate = new ModelField();
        this.birthdate.fieldId = R.id.etProfileBirthdate;
        this.birthdate.attribute = "birthdate";
        this.birthdate.formControl = "datepicker";
        this.birthdate.isRequired = true; // damn right it is

        this.organDonor = new ModelField();
        this.organDonor.fieldId = R.id.cbOrganDonor;
        this.organDonor.formControl = "checkbox";
        this.organDonor.dataType = "boolean";
        this.organDonor.attribute = "organ_donor";

        this.discoverable = new ModelField();
        this.discoverable.fieldId = R.id.cbSearchable;
        this.discoverable.formControl = "checkbox";
        this.discoverable.attribute = "searchable";
        this.discoverable.dataType = "boolean";

        this.demographicsPrivacy = new ModelField();
        this.demographicsPrivacy.fieldId = R.id.spProfileDemographicsPrivacy;
        this.demographicsPrivacy.attribute = "demographics_privacy";
        this.demographicsPrivacy.values = HealthNotifierAPI.getInstance().getValues("privacy");
        this.demographicsPrivacy.formControl = "select";

        this.gender = new ModelField();
        this.gender.fieldId = R.id.spProfileGender;
        this.gender.attribute = "gender";
        this.gender.values = HealthNotifierAPI.getInstance().getValues("patient", "gender");
        this.gender.formControl = "select";

        this.biometricsPrivacy = new ModelField();
        this.biometricsPrivacy.fieldId = R.id.spProfileBiometricsPrivacy;
        this.biometricsPrivacy.attribute = "biometrics_privacy";
        this.biometricsPrivacy.values = HealthNotifierAPI.getInstance().getValues("privacy");
        this.biometricsPrivacy.formControl = "select";

        // hacks on input type - compisite inputs son
        this.height = new ModelField();
        // this.height.fieldId = R.id.etProfileHeight;
        this.height.formControl = "height";
        this.height.attribute = "height";
        this.height.dataType = "float";

        this.weight = new ModelField();
        this.weight.fieldId = R.id.etProfileWeight;
        this.weight.formControl = "text";
        this.weight.attribute = "weight";
        this.weight.dataType = "float";

        this.pulse = new ModelField();
        this.pulse.fieldId = R.id.etProfilePulse;
        this.pulse.formControl = "text";
        this.pulse.attribute = "pulse";
        this.pulse.dataType = "integer";

        this.bpSystolic = new ModelField();
        this.bpSystolic.fieldId = R.id.etProfileBPSystolic;
        this.bpSystolic.formControl = "text";
        this.bpSystolic.attribute = "bp_systolic";
        this.bpSystolic.dataType = "integer";

        this.bpDiastolic = new ModelField();
        this.bpDiastolic.fieldId = R.id.etProfileBPDiastolic;
        this.bpDiastolic.formControl = "text";
        this.bpDiastolic.attribute = "bp_diastolic";
        this.bpDiastolic.dataType = "integer";

        this.ethnicity = new ModelField();
        this.ethnicity.fieldId = R.id.spProfileEthnicity;
        this.ethnicity.attribute = "ethnicity";
        this.ethnicity.values = HealthNotifierAPI.getInstance().getValues("patient", "ethnicity");
        this.ethnicity.formControl = "select";

        this.hairColor = new ModelField();
        this.hairColor.fieldId = R.id.spProfileHairColor;
        this.hairColor.attribute = "hair_color";
        this.hairColor.values = HealthNotifierAPI.getInstance().getValues("patient", "hair_color");
        this.hairColor.formControl = "select";

        this.eyeColor = new ModelField();
        this.eyeColor.fieldId = R.id.spProfileEyeColor;
        this.eyeColor.attribute = "eye_color_both";
        this.eyeColor.values = HealthNotifierAPI.getInstance().getValues("patient", "eye_color");
        this.eyeColor.formControl = "select";

        this.bloodType = new ModelField();
        this.bloodType.fieldId = R.id.spProfileBloodType;
        this.bloodType.attribute = "blood_type";
        this.bloodType.values = HealthNotifierAPI.getInstance().getValues("patient", "blood_type");
        this.bloodType.formControl = "select";

        this.humanizedName = "Profile";

    }
}
