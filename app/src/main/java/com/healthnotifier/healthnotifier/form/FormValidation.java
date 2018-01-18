package com.healthnotifier.healthnotifier.form;

import com.healthnotifier.healthnotifier.form.FieldValidation;

import java.util.ArrayList;

/**
 * Created by charles on 1/29/17.
 */

public class FormValidation extends Object {
    // the list of errors, but what container
    // this is just really honestly so we can have a typed hash
    public ArrayList<FieldValidation> errors = new ArrayList<FieldValidation>();

    public Boolean isValid() {
        if (this.errors.size() > 0 ){
            return false;
        } else {
            return true;
        }
    }
}
