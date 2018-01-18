package com.healthnotifier.healthnotifier.utility;

import java.util.Map;

// TODO: refactor to package Analytics, class Event, lul
public class AnalyticsEvent {

    public String eventName;
    public Map attributes;

    public AnalyticsEvent(String e, Map a) {
        this.eventName = e;
        this.attributes = a;
    }

    // love dat java optionals syntax
    public AnalyticsEvent(String e) {
        this.eventName = e;
        this.attributes = null;
    }

}
