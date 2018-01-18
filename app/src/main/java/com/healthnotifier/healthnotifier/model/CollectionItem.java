package com.healthnotifier.healthnotifier.model;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;

/**
 * Created by charles on 1/30/17.
 */

public class CollectionItem extends Model {
    public String collectionName;
    public Integer recordOrder = 0;
    public ModelField privacy;

    // yea son - define privacy in a moment son
    public CollectionItem(){
        this.privacy = new ModelField();
        this.privacy.isRequired = true;
        this.privacy.label = "Privacy";
        this.privacy.attribute = "privacy";
        this.privacy.formControl = "select";
        this.privacy.values = HealthNotifierAPI.getInstance().getValues("privacy");
    }

    // recordOrder in toJSON, ?? mememe
}
