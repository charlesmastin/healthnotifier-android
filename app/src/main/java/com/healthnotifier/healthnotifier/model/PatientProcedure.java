package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.R;

import org.json.JSONObject;

/**
 * Created by charles on 1/30/17.
 */

public class PatientProcedure extends CollectionItem {
    public ModelField healthEvent;
    public ModelField startDate;

    public PatientProcedure(){
        this.collectionName = "patient_health_events";

        this.healthEvent = new ModelField();
        this.healthEvent.isRequired = true;
        this.healthEvent.attribute = "health_event";
        this.healthEvent.label = "Procedure or Device";
        this.healthEvent.formControl = "autocomplete";
        this.healthEvent.fieldId = R.id.acPatientProcedureHealthEvent;
        this.healthEvent.autocompleteId = "procedure";

        this.startDate = new ModelField();
        this.startDate.attribute = "start_date";
        this.startDate.label = "Date Administered";
        this.startDate.formControl = "datepicker";
        this.startDate.fieldId = R.id.etPatientProcedureStartDate;
        // TODO: datepicker son

        this.privacy.fieldId = R.id.spPatientProcedurePrivacy;

        this.humanizedName = "Procedure";
    }

    @Override
    public JSONObject getDefaultJSON(){
        JSONObject json = super.getDefaultJSON();
        try {
            json.put("health_event_type", "PROCEDURE");
        } catch(Exception e){
            // donkey stains
        }
        return json;
    }
}
