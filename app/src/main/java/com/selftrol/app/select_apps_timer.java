package com.selftrol.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class select_apps_timer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listing_apps);

        // Get a list of all installed apps using PackageManager
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 0);

        // Get app names, icons, and package names
        List<String> appNames = new ArrayList<>();
        List<Drawable> appIcons = new ArrayList<>();
        List<String> packageNames = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            appNames.add(resolveInfo.loadLabel(packageManager).toString());
            appIcons.add(resolveInfo.loadIcon(packageManager));
            packageNames.add(resolveInfo.activityInfo.packageName);
        }

        // Create a custom adapter using AppListUtils
        AppListUtils.CustomAdapter adapter = AppListUtils.createCustomAdapter(this, resolveInfos, appNames, appIcons, packageNames);

        // Display the list of apps in the ListView
        ListView listViewApps = findViewById(R.id.listView);
        listViewApps.setAdapter(adapter);

        // Set a listener for checkbox clicks
        adapter.setOnCheckboxClickListener((packageName, isChecked) -> {
            if (isChecked) {
                // Start the SelectTimeActivity when the checkbox is checked
                Intent selectTimeIntent = new Intent(select_apps_timer.this, SelectTimeActivity.class);
                selectTimeIntent.putExtra("packageName", packageName);
                Log.d("MainActivity", packageName);
                // Pass the package name to SelectTimeActivity
                startActivity(selectTimeIntent);
            }
        });
    }
}
