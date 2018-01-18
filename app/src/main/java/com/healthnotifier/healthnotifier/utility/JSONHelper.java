package com.healthnotifier.healthnotifier.utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 2/2/17.
 */

public class JSONHelper {
    public static ArrayList<JSONObject> jsonArrayToArrayList(JSONArray jArray){
        // should this Throw? probably
        ArrayList<JSONObject> jArrayList = new ArrayList<JSONObject>();
        try {
            for(int i=0;i<jArray.length();i++){
                jArrayList.add(jArray.getJSONObject(i));
            }
        } catch(Exception e){

        }
        return jArrayList;
    }
}
