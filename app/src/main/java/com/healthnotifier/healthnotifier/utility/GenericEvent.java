package com.healthnotifier.healthnotifier.utility;

import java.util.Map;

public class GenericEvent {

    public String eventName;
    public Map attributes;

    public GenericEvent(String e, Map a) {
        this.eventName = e;
        this.attributes = a;
    }

    // love dat java optionals syntax
    public GenericEvent(String e) {
        this.eventName = e;
        this.attributes = null;
    }

}
