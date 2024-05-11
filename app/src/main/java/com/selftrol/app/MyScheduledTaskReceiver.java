package com.selftrol.app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyScheduledTaskReceiver extends BroadcastReceiver {

    private AppTimerDbHelper dbHelper;

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        dbHelper = new AppTimerDbHelper(context);
        // Perform the task (e.g., update the value in the database)
        updateDatabase(context);
    }

    private void updateDatabase(Context context) {
        // Perform database update here (e.g., set COLUMN_TIME_USED to 0)
        dbHelper.updateTimeUsedToZero();
        dbHelper.updateAllFlagsToFalse();
    }
}