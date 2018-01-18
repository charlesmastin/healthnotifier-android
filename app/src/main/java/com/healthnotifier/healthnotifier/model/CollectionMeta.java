package com.healthnotifier.healthnotifier.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by charles on 1/30/17.
 */

public class CollectionMeta extends Object {
    public String collectionId;
    public int listId = 0;
    public String label;
    public String rawJson;
    public ArrayList<JSONObject> collectionJson;
    public String adaptor = "default"; // bla vs a Generic type where we pass in the class of the adpator (if it had an interface) LOLO YOLO™
}
