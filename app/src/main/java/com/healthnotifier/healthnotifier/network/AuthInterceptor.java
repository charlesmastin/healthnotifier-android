package com.healthnotifier.healthnotifier.network;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.squareup.otto.Bus;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by charles on 2/19/17.
 */

public class AuthInterceptor implements Interceptor {
    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        // did we get a 401 anywhere except login though
        if(!request.url().toString().contains("oauth/access_token")) {
            if (response.code() == 401) {
                // we gotta jump threads bro
                // return null; // probably the wrong thing to do
                Bus bus = HealthNotifierApplication.bus;
                bus.post(new GenericEvent("onDeauthorized"));
            }
        }
        if(response.code() == 403){
            Bus bus = HealthNotifierApplication.bus;
            bus.post(new GenericEvent("onPermissionDenied"));
        }
        return response;
    }

}
