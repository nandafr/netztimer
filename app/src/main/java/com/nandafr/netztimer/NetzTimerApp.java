package com.nandafr.netztimer;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

/**
 * Created by Nanda Fathurrizki
 * Author GitHub: /nandafr
 * Author IG: @nanda.code
 * LinkedIn: /in/nandafr
 * ----
 * NetzTimer crafted at Monday, 15/11/2021
 */
public class NetzTimerApp extends Application {

    public static final String CHANNEL_ID = "netztimer_ch_1";
    RemoteViews bigViews, smallViews;


    @Override
    public void onCreate() {
        super.onCreate();

        createNotification();
    }

    private void createNotification() {
        bigViews = new RemoteViews(getPackageName(), R.layout.netztimer_notification);
        smallViews = new RemoteViews(getPackageName(), R.layout.netztimer_notification_small);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationChannel netzTimerServiceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(netzTimerServiceChannel);


        }
    }
}

