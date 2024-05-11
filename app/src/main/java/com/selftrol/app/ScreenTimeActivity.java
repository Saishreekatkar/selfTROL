package com.selftrol.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ScreenTimeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time);

        // Get appUsageMap data passed from MainActivity
        HashMap<String, Integer> appUsageMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("appUsageMap");

        // Ensure appNames and timeSpent have the same length
        if (appUsageMap != null) {
            List<AppInfo> appInfoList = new ArrayList<>();
            PackageManager packageManager = getPackageManager();
            // In ScreenTimeActivity class, modify the creation of AppInfo instances to filter out times lower than 1 minute and convert seconds to hours and minutes
            for (String packageName : appUsageMap.keySet()) {
                int timeSpentSeconds = appUsageMap.get(packageName);
                if (timeSpentSeconds >= 60) {
                    int hours = timeSpentSeconds / 3600;
                    int minutes = (timeSpentSeconds % 3600) / 60;

                    String timeSpentFormatted;
                    if (hours > 0) {
                        timeSpentFormatted = String.format(Locale.getDefault(), "%d hours %d minutes", hours, minutes);
                    } else {
                        timeSpentFormatted = String.format(Locale.getDefault(), "%d minutes", minutes);
                    }

                    // Get the app icon using getAppIconFromPackageName
                    Drawable appIcon = getAppIconFromPackageName(ScreenTimeActivity.this, packageName);
                    String appName = getAppNameFromPackageName(packageManager, packageName);

                    appInfoList.add(new AppInfo(appName, timeSpentFormatted, appIcon));


                }

            }

            // Sort appInfoList in descending order based on time spent
            // Sort appInfoList in descending order based on time spent
            Collections.sort(appInfoList, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo appInfo1, AppInfo appInfo2) {
                    // Assuming timeSpent is in the format "X hours Y minutes"
                    // Split and extract hours and minutes
                    String[] parts1 = appInfo1.getTimeSpent().split(" ");
                    int hours1 = 0;
                    int minutes1 = 0;
                    if (parts1.length == 4) {
                        hours1 = Integer.parseInt(parts1[0]);
                        minutes1 = Integer.parseInt(parts1[2]);
                    } else if (parts1.length == 2) {
                        minutes1 = Integer.parseInt(parts1[0]);
                    }

                    String[] parts2 = appInfo2.getTimeSpent().split(" ");
                    int hours2 = 0;
                    int minutes2 = 0;
                    if (parts2.length == 4) {
                        hours2 = Integer.parseInt(parts2[0]);
                        minutes2 = Integer.parseInt(parts2[2]);
                    } else if (parts2.length == 2) {
                        minutes2 = Integer.parseInt(parts2[0]);
                    }

                    // Compare total time spent
                    int totalTime1 = hours1 * 60 + minutes1;
                    int totalTime2 = hours2 * 60 + minutes2;

                    // Compare in descending order
                    return Integer.compare(totalTime2, totalTime1);
                }
            });

            // Populate ListView
            ListView listView = findViewById(R.id.screen_time_listview);
            ScreenTimeAdapter adapter = new ScreenTimeAdapter(this, appInfoList);
            listView.setAdapter(adapter);
        }
    }

    private String getAppNameFromPackageName(PackageManager packageManager, String packageName) {
        try {
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return packageName; // Return package name if app name not found
        }
    }

    private Drawable getAppIconFromPackageName(Context context, String appName) {
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            String label = packageManager.getApplicationLabel(packageInfo).toString();
            if (label.equals(appName)) {
                try {
                    return packageManager.getApplicationIcon(packageInfo.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("ScreenTimeActivity", "Error retrieving app icon for app: " + appName);
                    e.printStackTrace();
                    return null;
                }
            }
        }
        Log.e("ScreenTimeActivity", "App not found: " + appName);
        return null;
    }



    static class AppInfo {
        private String appName;
        private String timeSpent;
        private Drawable appIcon;

        public AppInfo(String appName, String timeSpent, Drawable appIcon) {
            this.appName = appName;
            this.timeSpent = timeSpent;
            this.appIcon = appIcon;
        }

        public String getAppName() {
            return appName;
        }

        public String getTimeSpent() {
            return timeSpent;
        }

        public Drawable getAppIcon() {
            return appIcon;
        }
    }

    static class ScreenTimeAdapter extends BaseAdapter {
        private Context context;
        private List<AppInfo> appInfoList;

        public ScreenTimeAdapter(Context context, List<AppInfo> appInfoList) {
            this.context = context;
            this.appInfoList = appInfoList;
        }

        @Override
        public int getCount() {
            return appInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return appInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder viewHolder;

            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.screen_time_list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.appIcon = view.findViewById(R.id.app_icon);
                viewHolder.appName = view.findViewById(R.id.app_name);
                viewHolder.timeSpent = view.findViewById(R.id.time_spent);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            AppInfo appInfo = appInfoList.get(position);
            viewHolder.appIcon.setImageDrawable(appInfo.getAppIcon());
            viewHolder.appName.setText(appInfo.getAppName());
            viewHolder.timeSpent.setText(appInfo.getTimeSpent());

            return view;
        }

        static class ViewHolder {
            ImageView appIcon;
            TextView appName;
            TextView timeSpent;
        }
    }
}
