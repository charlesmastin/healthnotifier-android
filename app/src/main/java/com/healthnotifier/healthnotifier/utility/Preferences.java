package com.healthnotifier.healthnotifier.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
	private SharedPreferences mSharedPreferences;
	private Context mContext;

	public static final int NULL_INT = -1;
	public static final long NULL_LONG = -1l;
	public static final double NULL_DOUBLE = -1.0;
	public static final String NULL_STRING = null;
	public static final Set<String> NULL_STRING_SET = null;

	public Preferences() {
		mContext = HealthNotifierApplication.getContext();
		//mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mSharedPreferences = mContext.getSharedPreferences("com.healthnotifier.healthnotifier", Context.MODE_PRIVATE);
	}

	public void clearPreferences() {
	    Logcat.d("CLEAR PREFERENCES");
		Editor editor = mSharedPreferences.edit();
		editor.clear();
		editor.commit();
	}

	// GETTERS

	public String getAuthToken() {
		String key = mContext.getString(R.string.prefs_key_auth_token);
		String value = mSharedPreferences.getString(key, NULL_STRING);
		return value;
	}
	
	public String getFirstName() {
		String key = mContext.getString(R.string.prefs_key_first_name);
		String value = mSharedPreferences.getString(key, NULL_STRING);
		return value;
	}
	
	public String getLastName() {
		String key = mContext.getString(R.string.prefs_key_last_name);
		String value = mSharedPreferences.getString(key, NULL_STRING);
		return value;
	}
	
	public String getEmail() {
		String key = mContext.getString(R.string.prefs_key_email);
		String value = mSharedPreferences.getString(key, NULL_STRING);
		return value;
	}

	public String getMobilePhone() {
		String key = mContext.getString(R.string.prefs_key_mobile_phone);
		String value = mSharedPreferences.getString(key, NULL_STRING);
		return value;
	}
	
	public boolean getProvider() {
		String key = mContext.getString(R.string.prefs_key_provider);
		boolean value = mSharedPreferences.getBoolean(key, false);
		return value;
	}

    public String getProviderCredentialStatus() {
        String key = mContext.getString(R.string.prefs_key_provider_credential_status);
        String value = mSharedPreferences.getString(key, NULL_STRING);
        return value;
    }

	public String getAccountId() {
		String key = mContext.getString(R.string.prefs_key_account_id);
		String value = mSharedPreferences.getString(key, NULL_STRING);
		return value;
	}

	// other crap to persist, because, why not, technically it should be just set on the Lifesquare.Application object

    // THIS IS A HACKZOR BUT it's easier than passing around the ID and storing local copies
    // revisit this if we need intense automated testing for some reason
    public String getCurrentPatientId(){
        String key = mContext.getString(R.string.prefs_key_current_patient_id);
        String value = mSharedPreferences.getString(key, NULL_STRING);
        return value;
    }

	public ArrayList<String> getPermissions() {
		String key = mContext.getString(R.string.prefs_key_permissions);
		Set<String> result = mSharedPreferences.getStringSet(key, NULL_STRING_SET);
		ArrayList<String> value = new ArrayList<String>();
		value.addAll(result);
		return value;
	}


	// SETTERS

	public void setAuthToken(String authToken) {
		String key = mContext.getString(R.string.prefs_key_auth_token);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		if(authToken != null) {
            editor.putString(key, authToken);
        }else{
		    // so
		    editor.remove(key);
        }
		editor.commit();
	}
	
	public void setFirstName(String firstName) {
		String key = mContext.getString(R.string.prefs_key_first_name);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(key, firstName);
		editor.commit();
	}
	
	public void setLastName(String lastName) {
		String key = mContext.getString(R.string.prefs_key_last_name);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(key, lastName);
		editor.commit();
	}
	
	public void setEmail(String email) {
        String key = mContext.getString(R.string.prefs_key_email);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, email);
        editor.commit();
    }

    public void setMobilePhone(String phone) {
        String key = mContext.getString(R.string.prefs_key_mobile_phone);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, phone);
        editor.commit();
    }
	
	public void setProvider(boolean provider) {
		String key = mContext.getString(R.string.prefs_key_provider);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, provider);
		editor.commit();
	}

    public void setProviderCredentialStatus(String providerCredentialStatus) {
        String key = mContext.getString(R.string.prefs_key_provider_credential_status);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, providerCredentialStatus);
        editor.commit();
    }

	public void setAccountId(String accountId) {
		String key = mContext.getString(R.string.prefs_key_account_id);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(key, accountId);
		editor.commit();
	}

    public void setCurrentPatientId(String patientId) {
        // this will be null when we "back out" and go back to the account screen, or whatever, and initially as well
        // set only on ViewPatient activity
        String key = mContext.getString(R.string.prefs_key_current_patient_id);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, patientId);
        editor.commit();
    }
	// TODO: retire this because we just don't care
	public void setPermissions(ArrayList<String> permissions) {
		String key = mContext.getString(R.string.prefs_key_permissions);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		Set<String> set = new HashSet<String>();
		set.addAll(permissions);
		editor.putStringSet(key, set);
		editor.commit();
	}

}
