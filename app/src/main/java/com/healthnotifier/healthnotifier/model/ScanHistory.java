package com.healthnotifier.healthnotifier.model;

import java.util.Date;

import io.realm.RealmObject;

/**
 * Created by charles on 2/18/17.
 */

public class ScanHistory extends RealmObject {
    // 0FG
    public String name;
    public String lifesquareId; // why not, this let's us "recreate" our webview URL, or do a get query son
    public String patientId;
    public String profilePhoto; // something to do with the photo bro the raw URL, we will apply sizes as we request brizzle
    public String address;
    public String lifesquareLocation;
    public Date createdAt; // of course we might get this for free, w/e son
}
