package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.R;

import org.json.JSONObject;

/**
 * Created by charles on 1/30/17.
 */

public class PatientCondition extends CollectionItem {
    public ModelField healthEvent;
    public ModelField startDate;

    public PatientCondition(){
        this.collectionName = "patient_health_events";

        this.healthEvent = new ModelField();
        this.healthEvent.isRequired = true;
        this.healthEvent.attribute = "health_event";
        this.healthEvent.label = "Condition";
        this.healthEvent.formControl = "autocomplete";
        this.healthEvent.autocompleteId = "condition";
        this.healthEvent.fieldId = R.id.acPatientConditionHealthEvent;

        this.startDate = new ModelField();
        this.startDate.attribute = "start_date";
        this.startDate.label = "Date Administered";
        this.startDate.formControl = "datepicker";
        this.startDate.fieldId = R.id.etPatientConditionStartDate;
        // TODO: datepicker son

        this.privacy.fieldId = R.id.spPatientConditionPrivacy;

        this.humanizedName = "Condition";

    }

    @Override
    public JSONObject getDefaultJSON(){
        JSONObject json = super.getDefaultJSON();
        try {
            json.put("health_event_type", "CONDITION");
        } catch(Exception e){
            // donkey stains
        }
        return json;
    }
}
