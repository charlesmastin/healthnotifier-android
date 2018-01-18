package com.healthnotifier.healthnotifier.model;

import org.json.JSONObject;

/**
 * Created by charles on 2/18/17.
 */

public class Lifesquare extends Object {
    // cheap utility model to store scanned, and to some parsing of the attributes, brizzle
    // this is just the lightweight Scan object / search / nearyby helper brizzle
    // not a legit Profile bro
    private JSONObject mNode;
    // ain't got time for properties bitch, just dynamic getters

    public void setNode(JSONObject node){
        mNode = node;
    };

    public String getLifesquareId(){
        // gonna be null bro if we're from a search, just cause dat's how it worky work
        try {
            if(!mNode.isNull("LifesquareId")){
                return mNode.getString("LifesquareId");
            } else {
                return null;
            }
        } catch(Exception e){
            return null;
        }
    }

    public String getPatientId(){
        // gonna be null bro if we're from a search, just cause dat's how it worky work
        try {
            if(!mNode.isNull("PatientId")){
                return mNode.getString("PatientId");
            } else {
                return null;
            }
        } catch(Exception e){
            return null;
        }
    }

    public String getName(){
        try {
            return mNode.getString("FirstName") + " " + mNode.getString("LastName");
        } catch(Exception e){
            return null;
        }
    }

    public String getProfilePhoto(){
        try {
            return mNode.getString("ProfilePhoto");
        } catch(Exception e){
            return null;
        }
    }

    public String getFormattedAddress() {
        try {
            if(!mNode.isNull("Residence")){
                JSONObject residenceNode = mNode.getJSONObject("Residence");
                String address = "";
                // 100% OTT null checks, because we can never have enough son
                // unsure, too lazy, maybe we have a is null or empty native lookup, w/e
                if (!residenceNode.isNull("Address1")) {
                    String t = residenceNode.getString("Address1");
                    if (!t.equals("")) {
                        address += t;
                    }
                }
                if (!residenceNode.isNull("Address2")) {
                    String t = residenceNode.getString("Address2");
                    if (!t.equals("")) {
                        address += ", " + t;
                    }
                }
                if (!residenceNode.isNull("City")) {
                    String t = residenceNode.getString("City");
                    if (!t.equals("")) {
                        address += ", " + t;
                    }
                }
                if (!residenceNode.isNull("State")) {
                    String t = residenceNode.getString("State");
                    if (!t.equals("")) {
                        address += ", " + t;
                    }
                }
                if (!residenceNode.isNull("Postal")) {
                    String t = residenceNode.getString("Postal");
                    if (!t.equals("")) {
                        address += ", " + t;
                    }
                }
                return address;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String getLifesquareLocation(){
        // excesssssssssssssive checks bro
        try {
            if (!mNode.isNull("Residence")) {
                JSONObject residenceNode = mNode.getJSONObject("Residence");
                String lifesquareLocation = "";
                if (!residenceNode.isNull("LifesquareLocation")) {
                    String t = residenceNode.getString("LifesquareLocation");
                    if (!t.equals("")) {
                        lifesquareLocation = t;
                    }
                }
                return lifesquareLocation;
            } else {
                return null;
            }
        } catch(Exception e){
            return null;
        }
    }
}
