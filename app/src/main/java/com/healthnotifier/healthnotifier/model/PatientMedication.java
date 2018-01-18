package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

import java.util.LinkedHashMap;

/**
 * Created by charles on 1/30/17.
 */

public class PatientMedication extends CollectionItem {
    public ModelField medication;
    public ModelField dose;
    public ModelField frequency;
    public ModelField quantity;

    public PatientMedication(){
        this.collectionName = "patient_therapies";

        this.medication = new ModelField();
        this.medication.attribute = "therapy";
        this.medication.label = "Medication";
        this.medication.isRequired = true;
        this.medication.formControl = "autocomplete";
        this.medication.autocompleteId = "medication";
        this.medication.fieldId = R.id.acPatientMedicationTherapy;

        this.dose = new ModelField();
        this.dose.attribute = "therapy_strength_form";
        this.dose.label = "Dose";
        this.dose.formControl = "select";
        this.dose.fieldId = R.id.spPatientMedicationDose;
        this.dose.values = new LinkedHashMap<String, String>();
        this.dose.values.put(null, "-");
        // values will be coming dynamically

        this.frequency = new ModelField();
        this.frequency.attribute = "therapy_frequency";
        this.frequency.label = "Frequency";
        this.frequency.formControl = "select";
        this.frequency.values = HealthNotifierAPI.getInstance().getValues("patient_therapy", "therapy_frequency");
        this.frequency.fieldId = R.id.spPatientMedicationFrequency;

        this.quantity = new ModelField();
        this.quantity.attribute = "therapy_quantity";
        this.quantity.label = "Quantity";
        this.quantity.formControl = "select";
        this.quantity.values = HealthNotifierAPI.getInstance().getValues("patient_therapy", "therapy_quantity");
        this.quantity.fieldId = R.id.spPatientMedicationQuantity;

        this.privacy.fieldId = R.id.spPatientMedicationPrivacy;

        this.humanizedName = "Medication";
    }
}
