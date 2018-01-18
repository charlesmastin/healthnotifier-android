package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 1/30/17.
 */

public class PatientAllergy extends CollectionItem {
    public ModelField allergen;
    public ModelField reaction;

    public PatientAllergy(){
        this.collectionName = "patient_allergies";

        this.allergen = new ModelField();
        this.allergen.attribute = "allergen";
        this.allergen.label = "Allergy";
        this.allergen.isRequired = true;
        this.allergen.formControl = "autocomplete";
        this.allergen.autocompleteId = "allergy";
        this.allergen.fieldId = R.id.acProfileAllergyAllergen;

        this.reaction = new ModelField();
        this.reaction.attribute = "reaction";
        this.reaction.label = "Reaction";
        this.reaction.formControl = "select";
        this.reaction.values = HealthNotifierAPI.getInstance().getValues("patient_allergy", "reaction");
        this.reaction.fieldId = R.id.spPatientAllergyReaction;

        this.privacy.fieldId = R.id.spPatientAllergyPrivacy;

        this.humanizedName = "Allergy";
    }
}
