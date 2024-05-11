package com.selftrol.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Arrays;

public class MyAppAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAppAccessibility";

    private boolean[] appStatus;
    private String[] items; // Declare the items array

    private AppTimerDbHelper dbHelper;
    private TimerManager timerManager;
    private MyScheduler scheduler;

    private String lastAppOpened = null; // For Android 12 logic
    private final Handler handler = new Handler();
    private Runnable closeRunnable;

    private BroadcastReceiver databaseUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.selftrol.ACTION_DATABASE_UPDATED".equals(intent.getAction())) {
                // Refresh items from the database
                refreshItemsFromDatabase();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize dbHelper in onCreate
        dbHelper = new AppTimerDbHelper(this);
        timerManager = new TimerManager(dbHelper);
        scheduler = new MyScheduler();
        // Register BroadcastReceiver to listen for database update intents
        IntentFilter intentFilter = new IntentFilter("com.example.selftrol.ACTION_DATABASE_UPDATED");
        registerReceiver(databaseUpdateReceiver, intentFilter);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Service connected");
        // Initialize items array from database
        refreshItemsFromDatabase();
        // Set up AccessibilityServiceInfo
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
    }

    public void refreshItemsFromDatabase() {
        AppTimerDbHelper dbHelp = new AppTimerDbHelper(this);
        // Retrieve package names from database and assign them to items array
        String[] packageNames = dbHelp.getPackageNames();
        if (packageNames != null) {
            items = packageNames;
            // Update appStatus array size accordingly
            appStatus = new boolean[packageNames.length];
            Arrays.fill(appStatus, false); // Reset app status array
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            handleEventAndroid12(event);
        } else {
            handleEventAndroid9(event);
        }
    }

    private void handleEventAndroid12(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                String pkgName = packageName.toString();
                if (isWatchedApp(pkgName)) {
                    if (!pkgName.equals(lastAppOpened)) {
                        long startTime = dbHelper.getTimerUsage(pkgName);
                        long timerDuration = dbHelper.getTimerDuration(pkgName);
                        if(startTime == timerDuration){
                            lastAppOpened = null;
                            Log.d(TAG, pkgName + "Time is Up");
                            // Launch LockScreenActivity to lock the app
                            Intent lockIntent = new Intent(getApplicationContext(), LockScreenActivity.class);
                            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(lockIntent);
                        }else{
                            lastAppOpened = pkgName;
                            timerManager.startTimer(this,startTime, timerDuration, pkgName);
                            Log.d(TAG, pkgName + " is opened");
                            if (closeRunnable != null) {
                                handler.removeCallbacks(closeRunnable);
                            }
                        }
                    }
                } else {
                    if (lastAppOpened != null && !isLauncherApp(pkgName) && !packageName.toString().equals("com.android.systemui")) {
                        closeRunnable = () -> {
                            long startTime = dbHelper.getTimerUsage(lastAppOpened);
                            long timerDuration = dbHelper.getTimerDuration(lastAppOpened);
                            if(startTime!=timerDuration){
                                Log.d(TAG, lastAppOpened + " is closed");
                                timerManager.pauseTimer(lastAppOpened);
                            }
                            Log.d(TAG, lastAppOpened + " is closed");
                            lastAppOpened = null;
                        };
                        handler.postDelayed(closeRunnable, 1000);
                    }
                }
            }
        }
    }

    private void handleEventAndroid9(AccessibilityEvent event) {
        if (items != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null) {
                for (int i = 0; i < items.length; i++) {
                    if (items[i].equals(packageName.toString())) {
                        if (!appStatus[i]) {
                            long startTime = dbHelper.getTimerUsage(items[i]);
                            long timerDuration = dbHelper.getTimerDuration(items[i]);
                            if(startTime == timerDuration){
                                appStatus[i] = false;
                                Log.d(TAG, items[i] + "Time is Up");
                                // Launch LockScreenActivity to lock the app
                                Intent lockIntent = new Intent(getApplicationContext(), LockScreenActivity.class);
                                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(lockIntent);
                                break;
                            }else{

                                appStatus[i] = true;
                                timerManager.startTimer(this,startTime, timerDuration, items[i]);
                                Log.d(TAG, items[i] + " is opened");
                            }
                        }
                        break; // Exit the loop once a match is found
                    } else if (appStatus[i] && !packageName.toString().equals("com.android.systemui" )) {
                        appStatus[i] = false;
                        long startTime = dbHelper.getTimerUsage(items[i]);
                        long timerDuration = dbHelper.getTimerDuration(items[i]);
                        if(startTime!=timerDuration){
                            Log.d(TAG, items[i] + " is closed");
                            timerManager.pauseTimer(items[i]);
                        }

                    }
                }
            }
        }
    }

    private boolean isWatchedApp(String packageName) {
        for (String app : items) {
            if (app.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLauncherApp(String packageName) {
        return packageName.contains("launcher") || packageName.contains("home");
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister BroadcastReceiver when the service is destroyed
        unregisterReceiver(databaseUpdateReceiver);
    }
}
