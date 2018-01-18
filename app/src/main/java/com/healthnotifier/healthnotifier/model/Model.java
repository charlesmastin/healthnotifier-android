package com.healthnotifier.healthnotifier.model;

import org.json.JSONObject;

/**
 * Created by charles on 1/30/17.
 */

public class Model extends Object {

    public String humanizedName;

    public JSONObject getDefaultJSON(){
        JSONObject json = new JSONObject();
        // iterate all da fields my brosef
        // this is so we can do defaults, YEA SON
        return json;
    }

}
