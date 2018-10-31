package com.controllerapp.jdfree.jjandroidedustudy.controllerapp.controller;

import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.controllerapp.jdfree.jjandroidedustudy.controllerapp.R;
import com.controllerapp.jdfree.jjandroidedustudy.controllerapp.activity.MainActivity;
import com.controllerapp.jdfree.jjandroidedustudy.controllerapp.model.AppListModel;

import java.util.List;

public class CheckTimeAppControllerService extends Service implements Runnable {

    private boolean isCheck;
    private List<AppListModel> mList;
    private MediaPlayer mPlayer;
    private Thread mThread;
    public static final String TAG = "abcasdf";
    private TimeInfoNotification mNotification;

    //    private int RETURN_VALUE = START_REDELIVER_INTENT;
    private int RETURN_VALUE = START_STICKY;

    int time = 0;

    private IBinder mBinder = new CheckTimeAppControlBinder();

    public class CheckTimeAppControlBinder extends Binder {
        public CheckTimeAppControllerService getService() {
            return CheckTimeAppControllerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if ("startForeground".equals(intent.getAction())) {
        mNotification = new TimeInfoNotification(this);

        startForeground(1, mNotification.getBuilder().build());

//        } else if (mThread == null) {
        isCheck = true;

        mThread = new Thread(this);
        mThread.start();
        mPlayer = MediaPlayer.create(this, R.raw.song);
        mPlayer.setLooping(true);
        mPlayer.start();
        if (intent != null) {
            mList = intent.getParcelableArrayListExtra(MainActivity.CHECK_CONTROLLER);
        }
//        }

        return RETURN_VALUE;
    }

    @Override
    public void onDestroy() {
        if (mNotification != null) {
            RETURN_VALUE = START_NOT_STICKY;
            mNotification.hide();

            isCheck = false;
            mPlayer.stop();
            if (mThread != null) {
//                mThread.interrupt();
                mThread = null;
            }
            this.stopSelf();
            super.onDestroy();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        try {
//            Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            UsageEvents usageEvents;
            TimeCalculationThread timeCalculation = new TimeCalculationThread("");
            timeCalculation.setDaemon(true);

            while (isCheck) {
                Thread.sleep(1000);

                if (mList != null) {

                    String packageName;

                    long endTime = System.currentTimeMillis();
                    long beginTime = endTime - 1000;

                    UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                    usageEvents = usageStatsManager.queryEvents(beginTime, endTime);

                    while (usageEvents.hasNextEvent()) {
                        UsageEvents.Event event = new UsageEvents.Event();
                        usageEvents.getNextEvent(event);

                        if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            packageName = event.getPackageName();

                            timeCalculation.setPackageName(packageName);

                            if (!timeCalculation.isTime) {
                                timeCalculation.setTime(true);
                                timeCalculation.start();
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            isCheck = false;
//            mThread.stop();
//            mThread.destroy();
//            this.stopSelf();
        }
    }


    private class TimeCalculationThread extends Thread {

        private boolean isTime = false;
        private String packageName;

        public boolean isTime() {
            return isTime;
        }

        public void setTime(boolean time) {
            isTime = time;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        private TimeCalculationThread() {

        }

        private TimeCalculationThread(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public void run() {
            try {
                while (isTime) {
                    for (int i = 0; i < mList.size(); i++) {
                        if (packageName.equals(mList.get(i).getPackageName())) {
                            time = mList.get(i).getAllDayTime();

                            int timeSecond = mList.get(i).getStartDayTime() + 1;
                            int minuteSecond = mList.get(i).getOverDayTime();

                            if (timeSecond % 60 == 0) {
                                minuteSecond--;
                            }

                            mList.get(i).setStartDayTime(timeSecond);
                            mList.get(i).setOverDayTime(minuteSecond);

                            Log.e(TAG, "run: start" + mList.get(i).getStartDayTime() +
                                    "overTime" + mList.get(i).getOverDayTime() + "  " + mList.get(i).getPackageName());

//                                    startActivity(mainIntent);
                        }

                    }

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
            } finally {
                isTime = false;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public List<AppListModel> getList() {
        return mList;
    }
}
