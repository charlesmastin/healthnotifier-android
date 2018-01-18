package com.healthnotifier.healthnotifier.network;

import android.support.annotation.Nullable;

import com.auth0.android.jwt.JWT;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.utility.Logcat;
import com.healthnotifier.healthnotifier.utility.Preferences;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HealthNotifierAPI {
    // singleton stuffs
    private static HealthNotifierAPI instance = null;
    private HealthNotifierAPI() {
        // I guess we could do this in the def
        this.headers = new HashMap<String, Object>();
        // this is the ticket for Rails and understanding incoming request format oddly
        this.headers.put("Accept", "application/json");
    }
    public static HealthNotifierAPI getInstance(){
        if (instance == null){
            instance = new HealthNotifierAPI();
        }
        // clever, but not too clever
        // try catch, because this will be retried the next necessary networking call, and not simply a call to statically configure this class
        try {
            Preferences pref = HealthNotifierApplication.preferences;
            if(pref.getAuthToken() != null) {
                // try to open up
                // and set if valid bro
                try {
                    JWT jwt = new JWT(pref.getAuthToken());
                    if (!jwt.isExpired(60 * 10)) { // 10 minutes drift / leeway
                        instance.setAccessToken(pref.getAuthToken());
                    }
                } catch (Exception e){
                    // legacy token as well

                }
            }
        } catch(Exception e){
        }
        return instance;
    }
    // regular class stuffs
    public static final String API_ROOT = Config.API_ROOT;
    private HashMap<String, Object> headers;
    // we should do this in the umm, constructor, lo
    private final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new AuthInterceptor()).build();
    private JSONArray valuesJson = null;
    //
    private String deviceToken = null;
    public String accessToken = null;

    public static final MediaType MEDIA_TYPE_JSON
            = MediaType.parse("application/json; charset=utf-8");

    public void setAccessToken(String token) {
        this.headers.put("Authorization", "Bearer " + token);
        accessToken = token;
    }

    public void removeAcessToken() {
        this.headers.remove("Authorization");
    }

    public void setClientVersionBuild(String version) {
        this.headers.put("HealthNotifier-Client-Version", version);
    }

    public void setClientVersionName(String version) {
        this.headers.put("HealthNotifier-Client-Version-Name", version);
    }

    public void loadValues() {
        // query that shiz
        // store results internally
        Request request = addHeaders(new Request.Builder())
            .url(API_ROOT + "values")
            .build();
        try {
            client.newCall(request).enqueue(
                new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // we couldn't get a response or we timed out, offline, etc
                        Logcat.d( "onFailure" + e.toString());
                    }

                    @Override
                    public void onResponse(Call call, okhttp3.Response response) throws IOException {
                        if (response.isSuccessful()) { // exactly what status codes determine this?
                            try {
                                valuesJson = new JSONArray(response.body().string());

                            } catch (Exception e) {
                                //
                            }
                        } else {
                            Logcat.d( "onResponseError" + response.toString());
                        }
                    }
                }
            );
        } catch (Exception e) {
            Logcat.d( "onException" + e.toString());
        }
    }

    private LinkedHashMap<String, String> reformatValues(JSONObject node){
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        // nill null your self silly
        values.put(null, "—"); // WTF SON
        try {
            JSONArray vA = node.getJSONArray("values");
            for (int i = 0; i < vA.length(); i++) {
                JSONObject tO = vA.getJSONObject(i);
                values.put(tO.getString("value"), tO.getString("name"));
            }
        } catch(Exception e){

        }
        return values;
    }

    public LinkedHashMap<String, String> getValues(String model) {
        if(valuesJson != null){
            for(int i=0;i<valuesJson.length();i++){
                try {
                    JSONObject node = valuesJson.getJSONObject(i);
                    if(node.getString("model").equals(model)) {
                        return reformatValues(node);
                    }
                } catch(Exception e){

                }
            }
        }
        return null;
    }

    public LinkedHashMap<String, String> getValues(String model, String attribute) {
        if(valuesJson != null){
            for(int i=0;i<valuesJson.length();i++){
                try {
                    JSONObject node = valuesJson.getJSONObject(i);
                    if(node.getString("model").equals(model) && node.getString("attribute").equals(attribute)) {
                        return reformatValues(node);
                    }
                } catch(Exception e){

                }
            }
        }
        return null;
    }

    // I said java standard nullable noooope
    public String getNameForValue(String model, @Nullable String attribute, String value){
        String name = "";
        // FML son all the time sone FML
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        if(attribute == null) {
            values = getValues(model);
        } else {
            values = getValues(model, attribute);
        }
        // check for dat key you little trick
        if(values.containsKey(value)){
            name = values.get(value);
        }
        return name;
    }

    // util to decorate request with headers son
    private Builder addHeaders(Builder request) {
        for (String key : headers.keySet()) {
            request.addHeader(key, headers.get(key).toString());
        }
        return request;
    }

    public void getAccessToken(String email, String password, Callback callback) {
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("grant_type", "password")
                    .add("username", email)
                    .add("password", password)
                    .build();
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "oauth/access_token")
                    .post(formBody)
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                // failure callback
                Logcat.d("onException" + e.toString());
            }
        } catch(Exception e){
            // failure callback
        }
    }

    public void logout(Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("device_token", this.deviceToken);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "auth/logout")
                    .delete(RequestBody.create(MEDIA_TYPE_JSON, body.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d("onException" + e.toString());
            }
        } catch(Exception e){
            // failure callback
        }

    }

    public void beginAccountRecovery(String email, @Nullable String phone, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("Email", email);
            if(!phone.equals("") && phone != null){
                body.put("MobilePhone", phone);
            }

            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "accounts/begin-recovery")
                    .post(RequestBody.create(MEDIA_TYPE_JSON, body.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                // failure callback
                Logcat.d("onException" + e.toString());
            }
        } catch(Exception e){
            // failure callback
        }
    }

    public void unlockAccount(String code, String phone, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("UnlockCode", code);
            body.put("MobilePhone", phone);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "accounts/recover")
                    .post(RequestBody.create(MEDIA_TYPE_JSON, body.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                // failure callback
                Logcat.d("onException" + e.toString());
            }
        } catch(Exception e){
            // failure callback
        }
    }

    public void completeAccountRecovery(String password, String token, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("Token", token);
            body.put("Password", password);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "accounts/complete-recovery")
                    .post(RequestBody.create(MEDIA_TYPE_JSON, body.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                // failure callback
                Logcat.d("onException" + e.toString());
            }
        } catch(Exception e){
            // failure callback
        }
    }

    public void createAccount(JSONObject json, Callback callback) {
        // http://stackoverflow.com/questions/25953819/how-to-set-connection-timeout-with-okhttp
        /*
        client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
         */

        OkHttpClient longJohnSilverClient = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        // ok reference / value thing here, unsure of

        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "accounts")
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            longJohnSilverClient.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void getAccount(String accountId, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "accounts/" + accountId)
                .get()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void updateAccount(String accountId, JSONObject json, Callback callback) {
        // docs say in memory post should be 1mb mememe
        // otherwise use the post streaming apis
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "accounts/" + accountId)
                .put(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void deleteAccount(String accountId, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "accounts/" + accountId)
                .delete()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void addDeviceToken(String token, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("platform", "android");
            json.put("device_token", token);
            this.deviceToken = token;
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "devices")
                    .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    public void registerProvider(JSONObject json, Callback callback) {
        // TODO: NOT WORKING LONG ENOUGH LOVE YOU LONG TIME
        OkHttpClient longJohnSilverClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "provider-credentials")
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            longJohnSilverClient.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void getLifesquare(String lifesquareId, @Nullable Double latitude, @Nullable Double longitude, Callback callback) {
        Map<Object, Object> params = new HashMap<Object, Object>();
        if(latitude != null && longitude != null){
            params.put("latitude", latitude);
            params.put("longitude", longitude);
        }
        String baseUrl = API_ROOT + "lifesquares/" + lifesquareId;
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){
            // YOU SCREWED SON
        }
    }

    public void getPatients(Callback callback) {
        Request request = addHeaders(new Request.Builder()).url(API_ROOT + "profiles").build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void getPatient(String patientId, Callback callback) {
        Request request = addHeaders(new Request.Builder()).url(API_ROOT + "profiles/" + patientId).build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void parseLicense(JSONObject json, Callback callback) {
        // docs say in memory post should be 1mb mememe
        // otherwise use the post streaming apis
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "parser/drivers-license")
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void createProfile(Callback callback) {
        // docs say in memory post should be 1mb mememe
        // otherwise use the post streaming apis
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles")
                .post(RequestBody.create(MEDIA_TYPE_JSON, new JSONObject().toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void updateProfile(String patientId, JSONObject json, Callback callback) {
        // docs say in memory post should be 1mb mememe
        // otherwise use the post streaming apis
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId)
                .put(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void deleteProfile(String patientId, Callback callback) {
        // docs say in memory post should be 1mb mememe
        // otherwise use the post streaming apis
        // not sure about the request response header on an empty body, whatever son
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId)
                .delete()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void confirmProfile(String patientId, Callback callback){
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/confirm")
                .put(RequestBody.create(MEDIA_TYPE_JSON, new JSONObject().toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void updateProfilePhoto(String patientId, JSONObject json, Callback callback) {
        OkHttpClient longJohnSilverClient = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/profile-photo")
                .put(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            longJohnSilverClient.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void getCollection(String patientId, String collectionName, Callback callback) {
        Request request = addHeaders(new Request.Builder()).url(API_ROOT + "profiles/" + patientId + "/" + collectionName).build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    // blame the OG crew, RS, this is create, update, and delete all in one son!
    // that said, I could have fixed it
    public void updateCollection(String patientId, String collectionName, JSONObject collectionNode, Callback callback) {
        // docs say in memory post should be 1mb mememe
        // otherwise use the post streaming apis
        JSONObject json = new JSONObject();
        try {
            JSONArray collectionArray = new JSONArray();
            collectionArray.put(collectionNode);
            json.put(collectionName, collectionArray);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/" + collectionName)
                    .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d("onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    public void getDocument(String uuid, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "documents/" + uuid)
                .get()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void createDocument(JSONObject json, Callback callback) {
        // http://stackoverflow.com/questions/25953819/how-to-set-connection-timeout-with-okhttp
        OkHttpClient longJohnSilverClient = new OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        // ok reference / value thing here, unsure of

        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "documents")
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            longJohnSilverClient.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void updateDocument(String uuid, JSONObject json, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "documents/" + uuid)
                .patch(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    public void deleteDocument(String uuid, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "documents/" + uuid)
                .delete()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    // assign, renew, replace could be aliased, whatever
    public void processLifesquares(String action, JSONObject json, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "lifesquares/" + action)
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    // this could just be a different action argument for processLifesquare, but this is hardcoded so it's non-descructive
    public void validateLifesquares(JSONObject json, Callback callback) {
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "lifesquares/validate")
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    // -------------------
    // PATIENT NETWORK SON
    // -------------------
    public void patientNetworkConnections(String patientId, Callback callback){
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/network")
                .get()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d("onException" + e.toString());
        }
    }

    // TODO: signature for null group yo, for now, pass in ""
    public void patientNetworkSearch(String patientId, String keywords, String group, Callback callback){
        // build up dat query string son, because of course Square devs didn't think to implement a query string on the request object
        // or I can't read docs, but it seems it's not supported
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("keywords", keywords);
        if(group.length() > 0){
            params.put("group", group);
        }
        String baseUrl = API_ROOT + "profiles/" + patientId + "/network/search";
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){
            // YOU SCREWED SON
        }

    }

    // PATIENT is the OWNER (confusing yes)
    // AUDITOR is the person ABLE TO VIEW
    // GRANTER is the patient BEING VIEWED
    // TODO: rework entire API to only accept a single param for the "granter or auditor"
    // it's always contextual based on operation

    public void patientNetworkAdd(String patientId, String granterId, String auditorId, String privacy, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            json.put("Privacy", privacy);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/add")
                    .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    // TODO: support "request reason custom message"
    public void patientNetworkRequestAccess(String patientId, String granterId, String auditorId, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/request-access")
                    .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    public void patientNetworkAccept(String patientId, String granterId, String auditorId, String privacy, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            json.put("Privacy", privacy);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/accept")
                    .put(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    // TODO: support "reason for decline message"
    public void patientNetworkDecline(String patientId, String granterId, String auditorId, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/decline")
                    .put(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    public void patientNetworkUpdate(String patientId, String granterId, String auditorId, String privacy, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            json.put("Privacy", privacy);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/update")
                    .put(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    // TODO: support "reason for revoke"
    public void patientNetworkRevoke(String patientId, String granterId, String auditorId, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/revoke")
                    .delete(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    public void patientNetworkLeave(String patientId, String granterId, String auditorId, Callback callback){
        JSONObject json = new JSONObject();
        try {
            json.put("GranterId", granterId);
            json.put("AuditorId", auditorId);
            Request request = addHeaders(new Request.Builder())
                    .url(API_ROOT + "profiles/" + patientId + "/network/leave")
                    .delete(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            try {
                client.newCall(request).enqueue(callback);
            } catch (Exception e) {
                Logcat.d( "onException" + e.toString());
            }
        } catch(Exception e){
            // well Fudge donkey nuts
        }
    }

    // TODO: signature for null group yo, for now, pass in ""
    public void termsSearch(String autocompleteId, String keywords, Callback callback){
        // build up dat query string son, because of course Square devs didn't think to implement a query string on the request object
        // or I can't read docs, but it seems it's not supported
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("category", autocompleteId);
        params.put("term", keywords);

        String baseUrl = API_ROOT + "term-lookup/search";
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){
            // YOU SCREWED SON
        }

    }

    public Response termsSearchSync(String autocompleteId, String keywords){
        // this is ONLY for calling in async context, such as the Filterable adapters
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("category", autocompleteId);
        params.put("term", keywords);
        String baseUrl = API_ROOT + "term-lookup/search";
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            return response;
        } catch(Exception e){
            // YOU SCREWED SON
        }
        // so screwed son
        return null;
    }

    public void medicationDose(String medicationName, Callback callback){
        // build up dat query string son, because of course Square devs didn't think to implement a query string on the request object
        // or I can't read docs, but it seems it's not supported
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("med_name", medicationName);

        String baseUrl = API_ROOT + "term-lookup/medication";
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){
            // YOU SCREWED SON
        }
    }

    public void notifyEmergencyContacts(String patientId, String message, @Nullable Double latitude, @Nullable Double longitude, Callback callback){
        // build up dat query string son, because of course Square devs didn't think to implement a query string on the request object
        // or I can't read docs, but it seems it's not supported
        String baseUrl = API_ROOT + "profiles/" + patientId + "/emergency-contacts/message";
        String url = "";
        if(latitude != null && longitude != null){
            Map<Object, Object> params = new HashMap<Object, Object>();
            params.put("latitude", latitude);
            params.put("longitude", longitude);
            try {
                url = addQueryStringToUrlString(baseUrl, params);
            } catch(Exception e){
                // meh
                Logcat.d("in here though, no geo for you son, really nothing");
            }
        } else {
            url = baseUrl;
        }
        // payload son
        JSONObject json = new JSONObject();
        try {
            json.put("message", message);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){

        }
    }

    // CAREPLANS, namespacing inconsistent bro, oh well
    public void getCareplans(String patientId, Callback callback){
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/advise-me")
                .get()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d( "onException" + e.toString());
        }
    }

    public void getCareplanQuestionGroup(String patientId, String questionGroupId, Callback callback){
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/advise-me/question-group/" + questionGroupId)
                .get()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d( "onException" + e.toString());
        }
    }

    public void createCareplanResponse(String patientId, JSONObject json, Callback callback){
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/advise-me/response")
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d( "onException" + e.toString());
        }
    }

    public void getCareplanRecommendation(String patientId, String recommendationId, Callback callback){
        Request request = addHeaders(new Request.Builder())
                .url(API_ROOT + "profiles/" + patientId + "/advise-me/advice/" + recommendationId)
                .get()
                .build();
        try {
            client.newCall(request).enqueue(callback);
        } catch (Exception e) {
            Logcat.d( "onException" + e.toString());
        }
    }

    // Providers Only search/location API
    public void searchLifesquares(String keywords, @Nullable Double latitude, @Nullable Double longitude, Callback callback){
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("keywords", keywords);
        if(latitude != null && longitude != null){
            params.put("latitude", latitude);
            params.put("longitude", longitude);
        }
        String baseUrl = API_ROOT + "lifesquares/search";
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){
            // YOU SCREWED SON
        }
    }

    public void nearbyLifesquares(Double latitude, Double longitude, Callback callback){
        Map<Object, Object> params = new HashMap<Object, Object>();
        params.put("latitude", latitude);
        params.put("longitude", longitude);
        String baseUrl = API_ROOT + "lifesquares/nearby";
        try {
            String url = addQueryStringToUrlString(baseUrl, params);
            Request request = addHeaders(new Request.Builder())
                    .url(url)
                    .get()
                    .build();
            client.newCall(request).enqueue(callback);
        } catch(Exception e){
            // YOU SCREWED SON
        }
    }


    // http://stackoverflow.com/questions/10786042/java-url-encoding-of-query-string-parameters
    // because Fing Java and Square LAZY TOWN
    public static String addQueryStringToUrlString(String url, final Map<Object, Object> parameters) throws UnsupportedEncodingException {
        if (parameters == null) {
            return url;
        }
        for (Map.Entry<Object, Object> parameter : parameters.entrySet()) {
            final String encodedKey = URLEncoder.encode(parameter.getKey().toString(), "UTF-8");
            final String encodedValue = URLEncoder.encode(parameter.getValue().toString(), "UTF-8");
            if (!url.contains("?")) {
                url += "?" + encodedKey + "=" + encodedValue;
            } else {
                url += "&" + encodedKey + "=" + encodedValue;
            }
        }
        return url;
    }

}
