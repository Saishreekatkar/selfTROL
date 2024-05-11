package com.selftrol.app;

import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;

public class TimerManager {

    private static final String TAG = "TimerManager";
    private CountDownTimer countDownTimer;
    private long remainingTime;
    private long startTime;
    private String packageName;
    private final AppTimerDbHelper dbHelper;

    public TimerManager(AppTimerDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    long elapsedTime;
    long endtime1;

    public void startTimer(MyAppAccessibilityService context, long startTime, long endTime, String packageName) {
        this.startTime = startTime;
        this.packageName = packageName;
        this.endtime1 = endTime * 1000;
        elapsedTime = (endTime * 1000) - (startTime * 1000);
        int timerDuration = dbHelper.getTimerDuration(packageName);

        // Check if timer duration is valid
        if (timerDuration <= 0) {
            Log.e(TAG, "Invalid timer duration for package: " + packageName);
        }

        countDownTimer = new CountDownTimer(elapsedTime, 1000) {
            public void onTick(long millisUntilFinished) {
                remainingTime = millisUntilFinished;
                // You can perform actions here on tick
            }

            public void onFinish() {
                dbHelper.updateTimerUsage(packageName, endTime);
                Log.d(TAG, "Timer finished!");

                // Launch LockScreenActivity to lock the app
                Intent lockIntent = new Intent(context, LockScreenActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lockIntent);
            }
        }.start();
    }


    public void pauseTimer(String packageName) {
        if(countDownTimer != null){
            countDownTimer.cancel();

            // Calculate time used before pausing
            long timeUsed = (endtime1-remainingTime) / 1000;
            // Update timer usage in the database
            dbHelper.updateTimerUsage(packageName, timeUsed);
        }
    }
}