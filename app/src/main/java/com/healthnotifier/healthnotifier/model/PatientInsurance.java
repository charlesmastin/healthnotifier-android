package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientInsurance extends CollectionItem {

    public ModelField orgName;
    public ModelField phone;
    public ModelField policyCode;
    public ModelField groupCode;
    public ModelField firstName;
    public ModelField lastName;

    public PatientInsurance(){
        this.collectionName = "patient_insurances";

        this.orgName = new ModelField();
        this.orgName.isRequired = true;
        this.orgName.attribute = "organization_name";
        this.orgName.label = "Insurance Company";
        this.orgName.fieldId = R.id.etPatientInsuranceOrgName;
        
        this.phone = new ModelField();
        this.phone.attribute = "phone";
        this.phone.label = "Insurance Phone Number";
        this.phone.fieldId = R.id.etPatientInsurancePhone;
        //this.phone.keyboard = UIKeyboardType.PhonePad;
        
        this.policyCode = new ModelField();
        this.policyCode.attribute = "policy_code";
        this.policyCode.label = "Member ID";
        this.policyCode.fieldId = R.id.etPatientInsurancePolicyCode;
        
        this.groupCode = new ModelField();
        this.groupCode.attribute = "group_code";
        this.groupCode.label = "Group #";
        this.groupCode.fieldId = R.id.etPatientInsuranceGroupCode;
       
        this.firstName = new ModelField();
        this.firstName.attribute = "policyholder_first_name";
        this.firstName.label = "First Name"; // Policyholder's;
        this.firstName.fieldId = R.id.etPatientInsuranceFirstName;
        
        this.lastName = new ModelField();
        this.lastName.attribute = "policyholder_last_name";
        this.lastName.label = "Last Name";// Policyholder's;
        this.lastName.fieldId = R.id.etPatientInsuranceLastName;

        this.privacy.fieldId = R.id.spPatientInsurancePrivacy;

        this.humanizedName = "Insurance Policy";
    }
}
