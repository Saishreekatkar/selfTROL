package com.selftrol.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Variables for app usage tracking
    private long lastResetTime = 0; // Time when the counters were last reset
    private int lastResetDay = 0; // Day of the month when the counters were last reset

    // HashMap to store app usage counters for each app individually
    private HashMap<String, AppUsageData> appUsageDataMap = new HashMap<>();
    private DatabaseHelper dbHelper;
    private AppTimerDbHelper appTimerDbHelper;

    private EditText editTextPackageName, editTextDuration;
    private Button buttonAddTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DatabaseHelper instance
        dbHelper = new DatabaseHelper(this);

        // Initialize AppTimerDbHelper instance
        appTimerDbHelper = new AppTimerDbHelper(this);

        // Check if usage access permission is granted, if not, request it
        if (!isUsageAccessPermissionGranted()) {
            requestUsageAccessPermission();
        }

        // Check if the Accessibility Service is enabled right away
        if (!isAccessibilityServiceEnabled(this, MyAppAccessibilityService.class)) {
            // If not enabled, redirect to Accessibility Settings
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            finish(); // Finish the activity as its work is done
        }

        // Reset app usage counters at midnight
        resetAppUsageCountersAtMidnight();

        // Calculate midnight of the current day
        long midnight = getMidnight();

        // Example usage
        long endTime = System.currentTimeMillis();
        final HashMap<String, Integer> appUsageMap = getTimeSpent(this, midnight, endTime);

        // Replace package names with app names
        HashMap<String, Integer> appUsageMapWithAppNames = new HashMap<>();
        for (String packageName : appUsageMap.keySet()) {
            String appName = getAppNameFromPackageName(packageName);
            appUsageMapWithAppNames.put(appName, appUsageMap.get(packageName));
        }

        // Calculate total screen time for today
        int totalScreenTime = 0;
        for (int time : appUsageMap.values()) {
            totalScreenTime += time;
        }
        int hours = totalScreenTime / 3600;
        int minutes = (totalScreenTime % 3600) / 60;
        String totalScreenTimeString = String.format("%d hours %d min", hours, minutes);

        ImageButton lockAppSelectButton = findViewById(R.id.lock_app_select_button);

        lockAppSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SelectAppActivity when the button is clicked
                startActivity(new Intent(MainActivity.this, select_apps_timer.class));
            }
        });

        // Button to navigate to screen time activity
        ImageButton showScreenTimeButton = findViewById(R.id.show_screen_time_button);
        TextView showScreenTime = findViewById(R.id.showScreenTime); // Replace yourTextViewId with the actual ID of your TextView
        showScreenTime.setText(totalScreenTimeString);

        // Set the total screen time as the button text
        View.OnClickListener redirectToScreenTimeActivity = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ScreenTimeActivity.class);
                intent.putExtra("appUsageMap", appUsageMap);
                startActivity(intent);
            }
        };

        ImageButton apps = findViewById(R.id.screen_time);
        showScreenTimeButton.setOnClickListener(redirectToScreenTimeActivity);
        apps.setOnClickListener(redirectToScreenTimeActivity);

        // Button to navigate to settings activity
        ImageButton settingsButton = findViewById(R.id.setting);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.activity_settings); // Assuming "settings.xml" is your static XML layout
            }
        });


        // Set the grayscale app icon to the ImageView
        ImageView appIconImageView = findViewById(R.id.app_icon);


    }

    public void redirectToAddTask(View view) {
        Intent intent = new Intent(this, select_apps_timer.class);
        startActivity(intent);

    }


    public void redirectToHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Optionally, you can finish the current activity if you don't want it to remain in the back stack
    }

    @Override
    public void onBackPressed() {
        // Start MainActivity
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish the current activity
    }

    // Method to navigate to the settings activity

    // Helper method to get the app name from the package name
    private String getAppNameFromPackageName(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return packageName; // Return package name if app name not found
        }
    }

    // Method to check if usage access permission is granted
    private boolean isUsageAccessPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        }
        return true; // For devices below Lollipop, usage access permission is always granted
    }

    // Method to request usage access permission
    private void requestUsageAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    // Reset app usage counters at midnight (potentially with data insertion)
    private void resetAppUsageCountersAtMidnight() {
        // Get current time and today's date
        Log.d(TAG, "Resetting app usage counters at midnight.");
        long currentTime = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Get the timestamp for midnight (12:00 AM) of the current day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long midnight = calendar.getTimeInMillis();

        // Check if it's a new day
        if (currentDay != lastResetDay || currentTime >= lastResetTime + (24 * 60 * 60 * 1000)) {
            // Calculate screen time for all apps (assuming getTimeSpent returns app usage data)
            HashMap<String, Integer> appUsageMap = getTimeSpent(this, lastResetTime, currentTime);

            // Insert the usage data into the database before resetting counters
            insertScreenTimeDataIntoDatabase(appUsageMap);

            // Reset counters to zero for each app
            for (String packageName : appUsageDataMap.keySet()) {
                AppUsageData appUsageData = appUsageDataMap.get(packageName);
                appUsageData.resetCounters();
            }

            // Update last reset date and time
            lastResetTime = midnight; // Update last reset time to midnight

            lastResetDay = currentDay; // Update last reset day
            Log.d(TAG, "App usage counters reset.");
            Log.d(TAG, "Last reset time: " + lastResetTime);

            Log.d(TAG, "Last reset day: " + lastResetDay);
        }
    }


    // Helper method to get the timestamp of midnight
    private long getMidnight() {
        // Get current time
        long currentTime = System.currentTimeMillis();

        // Get the timestamp for midnight (12:00 AM) of the current day
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // Helper method to calculate screen time for all apps
    HashMap<String, Integer> getTimeSpent(Context context, long beginTime, long endTime) {
        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEvents = new ArrayList<>();
        HashMap<String, Integer> appUsageMap = new HashMap<>();

        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = usageStatsManager.queryEvents(beginTime, endTime);

        while (usageEvents.hasNextEvent()) {
            currentEvent = new UsageEvents.Event();
            usageEvents.getNextEvent(currentEvent);
            if (currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                    || currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED) {
                allEvents.add(currentEvent);
                String key = currentEvent.getPackageName();
                if (!appUsageDataMap.containsKey(key)) {
                    appUsageDataMap.put(key, new AppUsageData());
                }
            }
        }

        for (int i = 0; i < allEvents.size() - 1; i++) {
            UsageEvents.Event E0 = allEvents.get(i);
            UsageEvents.Event E1 = allEvents.get(i + 1);

            if (E0.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED
                    && E1.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED
                    && E0.getClassName().equals(E1.getClassName())) {
                int diff = (int) (E1.getTimeStamp() - E0.getTimeStamp());
                diff /= 1000;
                AppUsageData appUsageData = appUsageDataMap.get(E0.getPackageName());
                appUsageData.addToTotalTime(diff);
            }
        }

        // Retrieve total usage time and app names for each app
        for (String packageName : appUsageDataMap.keySet()) {
            AppUsageData appUsageData = appUsageDataMap.get(packageName);
            int totalTime = appUsageData.getTotalTime();

            // Get the application name for the package name
            String appName = getAppNameFromPackageName(packageName);

            // Add the app name and total usage time to the map
            appUsageMap.put(appName, totalTime);
        }
        return appUsageMap;
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        ComponentName expectedComponentName = new ComponentName(context, service);
        if (!TextUtils.isEmpty(prefString)) {
            String[] services = prefString.split(":");
            for (String serviceComponent : services) {
                ComponentName enabledComponent = ComponentName.unflattenFromString(serviceComponent);
                if (enabledComponent != null && enabledComponent.equals(expectedComponentName))
                    return true;
            }
        }
        return false;
    }




    private void insertScreenTimeDataIntoDatabase(HashMap<String, Integer> appUsageMap) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            for (Map.Entry<String, Integer> entry : appUsageMap.entrySet()) {
                String appName = entry.getKey();
                int screenTime = entry.getValue();

                // Filter apps with less than 5 minutes of usage time (300 seconds)
                if (screenTime >= 300) {
                    // Create a new ContentValues object for each insertion
                    ContentValues values = new ContentValues();

                    // Prepare values to be inserted
                    values.put(DatabaseHelper.COLUMN_APP_NAME, appName);
                    values.put(DatabaseHelper.COLUMN_SCREEN_TIME, screenTime);

                    // Insert into database
                    db.insert(DatabaseHelper.TABLE_SCREEN_TIME, null, values);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error inserting screen time data into database.", Toast.LENGTH_SHORT).show();
        } finally {
            // Close the database connection
            db.close();
        }
    }


    // Class to store app usage data
    class AppUsageData {
        private int totalTime; // Total time spent in the app in seconds

        public AppUsageData() {
            totalTime = 0;
        }

        public void addToTotalTime(int time) {
            totalTime += time;
        }

        public int getTotalTime() {
            return totalTime;
        }

        public void resetCounters() {
            totalTime = 0;
        }
    }}



