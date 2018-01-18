package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.R;

import org.json.JSONObject;

/**
 * Created by charles on 1/30/17.
 */

public class PatientImmunization extends CollectionItem {

    public ModelField healthEvent;
    public ModelField startDate;

    public PatientImmunization(){
        this.collectionName = "patient_health_events";

        this.healthEvent = new ModelField();
        this.healthEvent.isRequired = true;
        this.healthEvent.attribute = "health_event";
        this.healthEvent.label = "Immunization";
        this.healthEvent.formControl = "autocomplete";
        this.healthEvent.autocompleteId = "immunization";
        this.healthEvent.fieldId = R.id.acPatientImmunizationHealthEvent;

        this.startDate = new ModelField();
        this.startDate.attribute = "start_date";
        this.startDate.label = "Date Administered";
        this.startDate.formControl = "datepicker";
        this.startDate.fieldId = R.id.etPatientImmunizationStartDate;
        // TODO: datepicker son

        this.privacy.fieldId = R.id.spPatientImmunizationPrivacy;

        this.humanizedName = "Immunization";
    }

    @Override
    public JSONObject getDefaultJSON(){
        JSONObject json = super.getDefaultJSON();
        try {
            json.put("health_event_type", "IMMUNIZATION");
        } catch(Exception e){
            // donkey stains
        }
        return json;
    }
}
