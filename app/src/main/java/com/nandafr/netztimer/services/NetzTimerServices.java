package com.nandafr.netztimer.services;

import android.app.AlarmManager;
import android.app.Application;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.nandafr.netztimer.MainActivity;
import com.nandafr.netztimer.R;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Nanda Fathurrizki
 * Author GitHub: /nandafr
 * Author IG: @nanda.code
 * LinkedIn: /in/nandafr
 * ----
 * NetzTimer crafted at Monday, 15/11/2021
 */
public class NetzTimerServices extends Service implements PropertyChangeListener {

    private static final String ACTION_PLAYPAUSE = "com.nandafr.netztimer.services.action_playpause";
    private static final String ACTION_RESET = "com.nandafr.netztimer.services.action_reset";
    private static final String ACTION_EXIT = "com.nandafr.netztimer.services.action_exit";


    private static final String CHANNEL_ID = "netztimer_ch_1";
    private RemoteViews bigViews, smallViews;
    private BroadcastReceiver recieverplayPause;
    private BroadcastReceiver recieverReset;
    private BroadcastReceiver recieverExit;
    private Notification mNotification;
    private boolean isNotificationShowing;
    private NotificationManager mNotificationManager;
    private Timer netzTimer;
    private String TAG = "NetzTimerServices";

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "updateNotification Called");
        Log.d(TAG, "onStartCommand Called");
        isNotificationShowing = false;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //untuk tombol play/pause timer di notifikasi
        IntentFilter filterPlayPause = new IntentFilter(ACTION_PLAYPAUSE);
        recieverplayPause = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive Called");
                if (TimeContainer.getInstance().getCurrentState() == TimeContainer.STATE_RUNNING) {
                    TimeContainer.getInstance().pause();
                } else {
                    TimeContainer.getInstance().start();
                }
                updateNotification();
            }
        };
        registerReceiver(recieverplayPause, filterPlayPause);
        //end tombol play/pause timer di notifikasi


        //untuk tombol reset timer di notifikasi
        IntentFilter filterReset = new IntentFilter(ACTION_RESET);
        recieverReset = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //ini untuk reset timer tanpa stop timer
//                TimeContainer.getInstance().reset();
                //ini untuk reset timer dan stop timer
                TimeContainer.getInstance().stopAndReset();
                updateNotification();
            }
        };
        registerReceiver(recieverReset, filterReset);
        //akhir tombol reset timer di notifikasi

        //untuk tombol exit di notifikasi
        IntentFilter filterExit = new IntentFilter(ACTION_EXIT);
        recieverExit = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //ini untuk reset timer tanpa stop timer
//                TimeContainer.getInstance().reset();
                //ini untuk reset timer dan stop timer
                TimeContainer.getInstance().stopAndReset();
                stopForeground(true);
                isNotificationShowing = false;
                stopSelf();
            }
        };
        registerReceiver(recieverExit, filterExit);
        //akhir tombol exit di notifikasi


        startUpdateTimer();
        TimeContainer.getInstance().isServiceRunning.set(true);

        return START_STICKY;
    }

    public void startUpdateTimer() {
        Log.d(TAG, "startUpdateTimer Called");

        if (netzTimer != null) {
            netzTimer.cancel();
            netzTimer = null;
        }
        netzTimer = new Timer();
        netzTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateNotification();
            }
        }, 0, 1000);
    }

    private synchronized void updateNotification() {
        Log.d(TAG, "updateNotification Called");

        bigViews = new RemoteViews(getPackageName(), R.layout.netztimer_notification);
        smallViews = new RemoteViews(getPackageName(), R.layout.netztimer_notification_small);

        if (TimeContainer.getInstance().getCurrentState() == TimeContainer.STATE_RUNNING) {
            bigViews.setImageViewResource(R.id.btn_playpause_notif, R.drawable.ic_pause);
            smallViews.setImageViewResource(R.id.btn_playpause_notif, R.drawable.ic_pause);

        } else {
            bigViews.setImageViewResource(R.id.btn_playpause_notif, R.drawable.ic_play);
            smallViews.setImageViewResource(R.id.btn_playpause_notif, R.drawable.ic_play);
        }

        Intent playPauseIntent = new Intent(ACTION_PLAYPAUSE, null);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent resetIntent = new Intent(ACTION_RESET, null);
        PendingIntent resetPendingIntent = PendingIntent.getBroadcast(this, 0, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent exitIntent = new Intent(ACTION_EXIT, null);
        PendingIntent exitPendingIntent = PendingIntent.getBroadcast(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //intent biasa untuk buka main activity ketika pengguna tap notifikasi (selain menu yang tersedia)
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getBroadcast(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setSilent(true)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(msToHourMinSec(TimeContainer.getInstance().getElapsedTime()))
                .setContentIntent(mainPendingIntent)
                .setSmallIcon(R.drawable.netzme)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCustomBigContentView(bigViews)
                .setCustomContentView(smallViews)
                .build();


        bigViews.setTextViewText(R.id.notif_timer, msToHourMinSec(TimeContainer.getInstance().getElapsedTime()));
        smallViews.setTextViewText(R.id.notif_timer, msToHourMinSec(TimeContainer.getInstance().getElapsedTime()));

        //Panggil Action Untuk Tombol Play/Pause
        bigViews.setOnClickPendingIntent(R.id.btn_playpause_notif, playPausePendingIntent);
        smallViews.setOnClickPendingIntent(R.id.btn_playpause_notif, playPausePendingIntent);

        //Panggil Action Untuk Tombol Reset Timer
        bigViews.setOnClickPendingIntent(R.id.btn_reset_notif, resetPendingIntent);
        smallViews.setOnClickPendingIntent(R.id.btn_reset_notif, resetPendingIntent);

        //Panggil Action Untuk Tombol Close Timer/Stop Forground Service
        bigViews.setOnClickPendingIntent(R.id.btn_close_notif, exitPendingIntent);
        smallViews.setOnClickPendingIntent(R.id.btn_close_notif, exitPendingIntent);

        if (isNotificationShowing) {
            Log.d(TAG, "Notification Showwing " + isNotificationShowing);
            mNotificationManager.notify(1, mNotification);
        } else {
            Log.d(TAG, "Notification NOT Showwing " + isNotificationShowing);
            isNotificationShowing = true;
            startForeground(1, mNotification);
        }
    }

    @Override
    public void onDestroy() {
//        AlarmManager alarmMgr = (AlarmManager)this.getSystemService(this.ALARM_SERVICE);
//        Intent playPauseIntent = new Intent(ACTION_PLAYPAUSE, null);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10, pendingIntent);

        Log.d(TAG,"onDestroy: Called");

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, RestarterNetzServices.class);
        this.sendBroadcast(broadcastIntent);

        if (netzTimer != null) {
            netzTimer.cancel();
            netzTimer = null;
        }
        unregisterReceiver(recieverplayPause);
        unregisterReceiver(recieverReset);
        unregisterReceiver(recieverExit);
        TimeContainer.getInstance().isServiceRunning.set(false);




        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public static class TimeContainer {
        public static final int STATE_STOPPED = 0;
        public static final int STATE_PAUSED = 1;
        public static final int STATE_RUNNING = 2;

        private static TimeContainer instance;
        public AtomicBoolean isServiceRunning;
        private PropertyChangeSupport observers;

        public static final String STATE_CHANGED = "state_changed";

        private int currentState;
        private long startTime;
        private long elapsedTime;

        private final Object mSynchronizedObject = new Object();

        private TimeContainer() {
            isServiceRunning = new AtomicBoolean(false);
            observers = new PropertyChangeSupport(this);
        }

        public void addObserver(PropertyChangeListener listener) {
            observers.addPropertyChangeListener(listener);
        }

        public void removeObserver(PropertyChangeListener listener) {
            observers.removePropertyChangeListener(listener);
        }

        public static TimeContainer getInstance() {
            if (instance == null) {
                instance = new TimeContainer();
            }
            return instance;
        }

        public void notifyStateChanged() {
            observers.firePropertyChange(STATE_CHANGED, null, currentState);
        }

        public int getCurrentState() {
            return currentState;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getElapsedTime() {
            if (startTime == 0) {
                return elapsedTime;
            } else {
                return elapsedTime + (System.currentTimeMillis() - startTime);
            }
        }

        public void start() {
            synchronized (mSynchronizedObject) {
                startTime = System.currentTimeMillis();
                currentState = STATE_RUNNING;
                notifyStateChanged();
            }
        }

        public void reset() {
            synchronized (mSynchronizedObject) {
                if (currentState == STATE_RUNNING) {
                    startTime = System.currentTimeMillis();
                    elapsedTime = 0;
                    currentState = STATE_RUNNING;
                    notifyStateChanged();
                } else {
                    startTime = 0;
                    elapsedTime = 0;
                    currentState = STATE_STOPPED;
                    notifyStateChanged();
                }
            }
        }

        public void stopAndReset() {
            synchronized (mSynchronizedObject) {
                startTime = 0;
                elapsedTime = 0;
                currentState = STATE_STOPPED;
                notifyStateChanged();
            }
        }

        public void pause() {
            synchronized (mSynchronizedObject) {
                elapsedTime = elapsedTime + (System.currentTimeMillis() - startTime);
                startTime = 0;
                currentState = STATE_PAUSED;
                notifyStateChanged();
            }
        }

    }

    private String msToHourMinSec(long ms) {
        if (ms == 0) {
            return "00:00:00";
        } else {
            long seconds = (ms / 1000) % 60;
            long minutes = (ms / 1000) / 60;
            long hours = minutes / 60;

            StringBuilder sb = new StringBuilder();
            if (hours > 0) {
                sb.append(hours);
            } else {
                sb.append('0');
                sb.append('0');
            }
            sb.append(':');
            if (minutes > 0) {
                minutes = minutes % 60;
                if (minutes >= 10) {
                    sb.append(minutes);
                } else {
                    sb.append(0);
                    sb.append(minutes);
                }
            } else {
                sb.append('0');
                sb.append('0');
            }
            sb.append(':');
            if (seconds > 0) {
                if (seconds >= 10) {
                    sb.append(seconds);
                } else {
                    sb.append(0);
                    sb.append(seconds);
                }
            } else {
                sb.append('0');
                sb.append('0');
            }
            return sb.toString();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName() == TimeContainer.STATE_CHANGED) {
            startUpdateTimer();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 0, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT );
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);


    }
}
