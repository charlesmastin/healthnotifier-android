package com.healthnotifier.healthnotifier;
import android.os.Build;

import com.healthnotifier.healthnotifier.BuildConfig;
public class Config {

	public static final boolean LOGS = false;
    public static final String RELEASE_API_ROOT = "https://api.domain.com/api/v1/";
    public static final String API_ROOT = "http://10.0.1.9:3000/api/v1/";

    public static final int JPEG_COMPRESSION = 90;

    public static final String DEBUG_STRIPE_PUBLISHABLE_KEY = BuildConfig.LSQ_DEBUG_STRIPE_PUBLISHABLE_KEY;
    public static final String RELEASE_STRIPE_PUBLISHABLE_KEY = BuildConfig.LSQ_RELEASE_STRIPE_PUBLISHABLE_KEY;

    //public static final String DEBUG_API_HMAC = BuildConfig.LSQ_DEBUG_API_HMAC;
    //public static final String RELEASE_API_HMAC = BuildConfig.LSQ_RELEASE_API_HMAC;

}
