package com.selftrol.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SelectTimeActivity extends AppCompatActivity {

    private NumberPicker numberPickerHours;
    private NumberPicker numberPickerMinutes;
    private Button buttonConfirm;
    private String packageName; // Assuming packageName is set before launching this activity
    private AppTimerDbHelper dbHelper; // Declare dbHelper variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_time);

        // Initialize NumberPickers
        numberPickerHours = findViewById(R.id.numberPickerHours);
        numberPickerMinutes = findViewById(R.id.numberPickerMinutes);

        // Set the minimum and maximum values for hours (0-23)
        numberPickerHours.setMinValue(0);
        numberPickerHours.setMaxValue(23);

        // Set the minimum and maximum values for minutes (0-59)
        numberPickerMinutes.setMinValue(0);
        numberPickerMinutes.setMaxValue(59);

        // Initialize Button
        buttonConfirm = findViewById(R.id.buttonConfirm);

        // Initialize dbHelper
        dbHelper = new AppTimerDbHelper(this);

        // Retrieve the packageName from the intent extras
        Intent intent = getIntent();
        if (intent != null) {
            packageName = intent.getStringExtra("packageName");
        }

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hours = numberPickerHours.getValue();
                int minutes = numberPickerMinutes.getValue();

                int duration = ((hours * 60) + minutes) * 60;

                // Check if flag in table is true

                String flagValue = dbHelper.getFlagValue(packageName);
                if (flagValue != null && flagValue.equals("true")) {
                    // Flag is true, show alert dialog and return
                    new AlertDialog.Builder(SelectTimeActivity.this)
                            .setTitle("Cannot Change Time")
                            .setMessage("You cannot change the time now. Please make the changes Tommorrow.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                Log.d("Package name", packageName);
                Log.d("Duration", String.valueOf(duration));

                // Send the selected time (hours and minutes) to the database
                long result = dbHelper.addAppTimer(packageName, duration);
                if (result != -1) {
                    // Timer added successfully
                    Log.d("MainActivity", "Timer added successfully");
                    dbHelper.updateFlagValue(packageName, "true");
                    // Broadcast intent to inform the service about the database update
                    Intent intent = new Intent("com.example.selftrol.ACTION_DATABASE_UPDATED");
                    sendBroadcast(intent);
                } else {
                    // Error adding timer
                    Log.d("MainActivity", "Error adding timer");
                }

                // Display a toast message
                String message = "Usage time set to: " + hours + " hours and " + minutes + " minutes";
                Toast.makeText(SelectTimeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

