package com.healthnotifier.healthnotifier.model;

import android.widget.Spinner;

import java.util.LinkedHashMap;

public class ModelField extends Object {
    public Integer fieldId; // the R.bla on your nuts
    public String attribute; // within the scope of the parent JSON "object"
    public String label = "";
    public String dataType = "string"; // boolean, integer, double, meh
    public String formControl = "text"; // select, checkbox, (custom guys)
    public Boolean isRequired = false;
    // for select, w/e, vs storing the reference somewhere else, waa waa waa waa
    public LinkedHashMap<String, String> values;
    // meh
    public String autocompleteId = null;

    public String getLabel() {
        if(!this.label.equals("")){
            return this.label;
        } else {
            // TODO: use apache commons or something homebrewed
            // http://stackoverflow.com/questions/1086123/string-conversion-to-title-case
            String l = this.attribute.toLowerCase().replace("_", " ");
            if(l.length() > 1) {
                return l.substring(0, 1).toUpperCase() + l.substring(1);
            } else {
                return l;
            }
        }
    }


}