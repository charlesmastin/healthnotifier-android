package com.healthnotifier.healthnotifier.form;

import com.healthnotifier.healthnotifier.model.ModelField;

/**
 * Created by charles on 1/29/17.
 */

public class FieldValidation extends Object {
    // this is just really honestly so we can have a typed hash
    // implementation upstream
    public String attribute;
    public String fieldId;
    public String message;
    // optional son
    public ModelField field = null;
}
