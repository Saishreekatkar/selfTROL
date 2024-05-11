package com.selftrol.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import com.selftrol.app.AppTimerDbHelper;

public class SelectAppActivity extends AppCompatActivity {
    private EditText editTextPackageName, editTextDuration;

    private AppTimerDbHelper dbHelper;
    private AppTimerDbHelper appTimerDbHelper;
    private Button buttonAddTimer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selectapp);

        // Initialize dbHelper and appTimerDbHelper
        dbHelper = new AppTimerDbHelper(this);
        appTimerDbHelper = new AppTimerDbHelper(this);

        // Initialize views
        editTextPackageName = findViewById(R.id.editTextPackageName);
        editTextDuration = findViewById(R.id.editTextDuration);
        buttonAddTimer = findViewById(R.id.buttonAddTimer);


        // Set click listener for the Add Timer button
        buttonAddTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTimer();
            }
        });

        Cursor cursor = dbHelper.getAllData();

        // Iterate over the cursor to access the data
        if (cursor != null) {
            while (cursor.moveToNext()) {
                // Retrieve data from the cursor
                @SuppressLint("Range") String packageName = cursor.getString(cursor.getColumnIndex(AppTimerContract.COLUMN_PACKAGE_NAME));
                @SuppressLint("Range") long timerDuration = cursor.getLong(cursor.getColumnIndex(AppTimerContract.COLUMN_TIMER_DURATION));
                @SuppressLint("Range") long timeUsed = cursor.getLong(cursor.getColumnIndex(AppTimerContract.COLUMN_TIME_USED));

                // Now you can use packageName, timerDuration, and timeUsed as needed
                Log.d("MainActivity", "Package Name: " + packageName + ", Timer Duration: " + timerDuration + ", Time Used: " + timeUsed);
            }
            cursor.close(); // Don't forget to close the cursor when done
        }

    }
    private void addTimer() {
        // Get user input
        String packageName = editTextPackageName.getText().toString().trim();
        int duration = Integer.parseInt(editTextDuration.getText().toString().trim());

        // Use the instance of AppTimerDbHelper to add timer
        long result = appTimerDbHelper.addAppTimer(packageName, duration);
        if (result != -1) {
            // Timer added successfully
            Log.d("MainActivity", "Timer added successfully");
            // Broadcast intent to inform the service about the database update
            Intent intent = new Intent("com.example.selftrol.ACTION_DATABASE_UPDATED");
            sendBroadcast(intent);
        } else {
            // Error adding timer
            Log.d("MainActivity", "Error adding timer");
        }
    }
    }
