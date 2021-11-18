package com.nandafr.netztimer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.nandafr.netztimer.databinding.ActivityMainBinding;
import com.nandafr.netztimer.services.NetzTimerServices;
import com.nandafr.netztimer.utils.Constants;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,PropertyChangeListener {

    private ActivityMainBinding bind;


    private boolean isRunning = false;
    private boolean wasRunning = false;
    private long millis;


    private long seconds;
    private long minutes;
    private long hours;
    Intent serviceIntent;
    private SharedPreferences sPref;
    private Timer netzTimer;
    private Handler netzHandler;
    private String TAG = MainActivity.class.getSimpleName();


    private final Runnable updateTextRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimeText();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bind = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(bind.getRoot());

        initView();

        serviceIntent = new Intent(this, NetzTimerServices.class);

        //ini kondisi untuk cek state timer ~ tetapi tidak digunakan :)
        //selayaknya aplikasi musik, eh timer, akan lebih baik jika tetap berjalan, bukan berhenti
//        if(savedInstanceState != null){
//            hours    =  savedInstanceState.getLong(Constants.keyHours);
//            minutes  =  savedInstanceState.getLong(Constants.keyMinutes);
//            seconds  =  savedInstanceState.getLong(Constants.keySeconds);
//        }

    }

    private void initView(){
        bind.btnStartStopTimer.setOnClickListener(this);
        bind.btnResetTimer.setOnClickListener(this);
        netzHandler = new Handler();
    }

    private void checkServiceRunning() {
        Log.d(TAG, "checkServiceRunning + " + NetzTimerServices.TimeContainer.getInstance().isServiceRunning.get());
        if(!NetzTimerServices.TimeContainer.getInstance().isServiceRunning.get()) {
            startService(new Intent(this, NetzTimerServices.class));
        }
    }

    //ini kondisi untuk get state timer ~ tetapi tidak digunakan :)
//    @Override
//    protected void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
//        hours    =  outState.getLong(Constants.keyHours);
//        minutes  =  outState.getLong(Constants.keyMinutes);
//        seconds  =  outState.getLong(Constants.keySeconds);
//    }


    //ini kondisi untuk menyimpan state timer ~ tetapi tidak digunakan :)
//    @Override
//    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        savedInstanceState.putLong(Constants.keyHours, hours);
//        savedInstanceState.putLong(Constants.keyMinutes, minutes);
//        savedInstanceState.putLong(Constants.keySeconds, seconds);
//    }

    @Override
    protected void onPause() {
        super.onPause();
        if(netzTimer != null) {
            netzTimer.cancel();
            netzTimer = null;
        }
        NetzTimerServices.TimeContainer.getInstance().removeObserver(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServiceRunning();
        NetzTimerServices.TimeContainer tc = NetzTimerServices.TimeContainer.getInstance();
        if(tc.getCurrentState() == NetzTimerServices.TimeContainer.STATE_RUNNING){
            bind.btnStartStopTimer.setText(R.string.btn_pause);
            startUpdateTimer();
        }else {
            bind.btnStartStopTimer.setText(R.string.btn_start);
            updateTimeText();
        }
        NetzTimerServices.TimeContainer.getInstance().addObserver(this);
    }

    @Override
    public void onClick(View v) {
        NetzTimerServices.TimeContainer timeContainer = NetzTimerServices.TimeContainer.getInstance();
        switch (v.getId()){
            case R.id.btnStartStopTimer:
                    Log.d(TAG, "StartStop clicked");
                    if (timeContainer.getCurrentState() == NetzTimerServices.TimeContainer.STATE_RUNNING) {
                        bind.waveAnimation.pauseAnimation();
                        NetzTimerServices.TimeContainer.getInstance().pause();
                        bind.btnStartStopTimer.setText(R.string.btn_resume);
                        Log.d(TAG, "STATE RUNNING");

                    }else{
                        NetzTimerServices.TimeContainer.getInstance().start();
                        startUpdateTimer();
                        bind.waveAnimation.playAnimation();
                        bind.btnStartStopTimer.setText(R.string.btn_pause);
                        Log.d(TAG, "ELSE STATE");
                    }
                break;

            case R.id.btnResetTimer:
                Log.d(TAG, "Reset clicked");
                //ini untuk reset timer tanpa menghentikan timer
//                if(timeContainer.getCurrentState() == NetzTimerServices.TimeContainer.STATE_RUNNING){
//                    NetzTimerServices.TimeContainer.getInstance().reset();
//                    updateTimeText();
//                }

                //ini untuk reset timer dan timer berhenti
                NetzTimerServices.TimeContainer.getInstance().stopAndReset();
                updateTimeText();
                bind.btnStartStopTimer.setText(R.string.btn_start);
                break;
        }

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(NetzTimerServices.TimeContainer.STATE_CHANGED)){
            NetzTimerServices.TimeContainer t = NetzTimerServices.TimeContainer.getInstance();
            if(t.getCurrentState() == NetzTimerServices.TimeContainer.STATE_RUNNING){
                bind.btnStartStopTimer.setText(R.string.btn_pause);
                bind.waveAnimation.playAnimation();
                startUpdateTimer();
            }else{
                bind.btnStartStopTimer.setText(R.string.btn_resume);
                bind.waveAnimation.pauseAnimation();
                updateTimeText();
            }
            checkServiceRunning();
        }
    }

    private void startUpdateTimer() {
        if(netzTimer != null) {
            netzTimer.cancel();
            netzTimer = null;
        }
        netzTimer = new Timer();
        netzTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                netzHandler.post(updateTextRunnable);
            }
        }, 0, 16);
    }

    private void updateTimeText() {
        bind.timer.setText(getTimeString(NetzTimerServices.TimeContainer.getInstance().getElapsedTime()));
    }

    private String getTimeString(long ms) {
        if(ms == 0) {
            return "00:00:00";
        } else {
             millis = (ms % 1000) / 10;
             seconds = (ms / 1000) % 60;
             minutes = (ms / 1000) / 60;
             hours = minutes / 60;

            StringBuilder sb = new StringBuilder();
            if(hours > 0) {
                sb.append(hours);
            }else{
                sb.append('0');
                sb.append('0');
            }
            sb.append(':');
            if(minutes > 0) {
                minutes = minutes % 60;
                if(minutes >= 10) {
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
            if(seconds > 0) {
                if(seconds >= 10) {
                    sb.append(seconds);
                } else {
                    sb.append(0);
                    sb.append(seconds);
                }
            } else {
                sb.append('0');
                sb.append('0');
            }
            //ini untuk menampilkan milidetik, jika dibutuhkan :)
//            sb.append(':');
//            if(millis > 0) {
//                if(millis >= 10) {
//                    sb.append(millis);
//                } else {
//                    sb.append(0);
//                    sb.append(millis);
//                }
//            } else {
//                sb.append('0');
//                sb.append('0');
//            }
            return sb.toString();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
