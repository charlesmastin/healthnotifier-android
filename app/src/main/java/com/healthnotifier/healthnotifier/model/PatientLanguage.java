package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientLanguage extends Model {

    public ModelField code;
    public ModelField proficiency;

    public String collectionName = "patient_languages";
    public Integer recordOrder = 0;
    // don't care about java OO now, and this model need to "delete" the privacy attribute whatever, F it
    // recordOrder
    // collectionName

    public PatientLanguage() {
        this.code = new ModelField();
        this.code.attribute = "language_code";
        this.code.label = "Language";
        this.code.isRequired = true;
        this.code.formControl = "select";
        this.code.values = HealthNotifierAPI.getInstance().getValues("language_code");
        this.code.fieldId = R.id.spPatientLanguageLanguage;

        this.proficiency = new ModelField();
        this.proficiency.attribute = "language_proficiency";
        this.proficiency.label = "Proficiency";
        this.proficiency.isRequired = true;
        this.proficiency.formControl = "select";
        this.proficiency.values = HealthNotifierAPI.getInstance().getValues("patient_language", "proficiency");
        this.proficiency.fieldId = R.id.spPatientLanguageProficiency;

        this.humanizedName = "Language";
    }

}
