package com.healthnotifier.healthnotifier.utility;

import android.content.ComponentCallbacks2;
import android.content.res.Configuration;

import com.squareup.otto.Bus;

import java.util.HashMap;
import java.util.Map;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;

/**
 * Created by charles on 4/20/16.
 * http://stackoverflow.com/questions/4414171/how-to-detect-when-an-android-app-goes-to-the-background-and-come-back-to-the-fo
 */
public class MemoryBoss implements ComponentCallbacks2 {
    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(final int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            Bus bus = HealthNotifierApplication.bus;
            bus.post(new GenericEvent("onAppBackground"));
        }
        // you might as well implement some memory cleanup here and be a nice Android dev.
    }
}