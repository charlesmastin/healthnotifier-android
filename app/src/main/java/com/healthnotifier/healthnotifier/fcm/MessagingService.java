package com.healthnotifier.healthnotifier.fcm;

import android.content.Intent;
import android.os.IBinder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.healthnotifier.healthnotifier.utility.Logcat;

public class MessagingService extends FirebaseMessagingService {
    public MessagingService() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            PushController controller = new PushController();
            controller.data = remoteMessage.getData();
            if (remoteMessage.getNotification() != null) {
                controller.notification = remoteMessage.getNotification();
            }
            // jump threads bro
            controller.handleForeground(getApplicationContext());
        }
    }
}
