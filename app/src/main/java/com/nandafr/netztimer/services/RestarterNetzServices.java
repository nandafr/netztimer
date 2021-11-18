package com.nandafr.netztimer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Created by Nanda Fathurrizki
 * Author GitHub: /nandafr
 * Author IG: @nanda.code
 * LinkedIn: /in/nandafr
 * ----
 * NetzTimer crafted at Thursday, 18/11/2021
 */
public class RestarterNetzServices extends BroadcastReceiver {
    private String TAG = "RestarterNetzServices";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"Broadcast Listened: Service tried to stop");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, NetzTimerServices.class));
        } else {
            context.startService(new Intent(context, NetzTimerServices.class));
        }
    }
}
