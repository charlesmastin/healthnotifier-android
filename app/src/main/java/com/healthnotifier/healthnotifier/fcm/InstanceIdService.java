package com.healthnotifier.healthnotifier.fcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.healthnotifier.healthnotifier.utility.Logcat;

/**
 * Created by charles on 3/13/17.
 */

public class InstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        // TODO call our LF api
        Logcat.d(refreshedToken);
    }
}
