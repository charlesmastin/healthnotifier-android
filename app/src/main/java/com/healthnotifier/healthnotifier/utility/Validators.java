package com.healthnotifier.healthnotifier.utility;

import android.text.TextUtils;

import java.util.regex.Pattern;

/**
 * Created by charles on 4/18/16.
 */
public class Validators {
    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public final static boolean isValidPhone(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.PHONE.matcher(target).matches();
    }

    public final static boolean isValidLifesquare(CharSequence target) {
        // TODO: really only need A-Za-Z0-9
        Pattern lifesquarePattern = Pattern.compile(".*[\\d|\\w].*"); // note not non-word
        return !TextUtils.isEmpty(target) && target.length() == 9 && lifesquarePattern.matcher(target).matches();
    }

    public final static boolean isValidPassword(CharSequence target) {
        Pattern passwordPattern = Pattern.compile(".*[\\d|\\W].*");
        return !TextUtils.isEmpty(target) && (target.length() > 7 && target.length() < 128) && passwordPattern.matcher(target).matches();
    }
}
