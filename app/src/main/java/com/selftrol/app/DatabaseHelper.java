package com.selftrol.app;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "screen_time.db";
    private static final int DATABASE_VERSION = 1;

    // Table names and column names
    public static final String TABLE_SCREEN_TIME = "screen_time";
    public static final String TABLE_APPS = "apps"; // New table for app names and IDs
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_APP_ID = "app_id";
    public static final String COLUMN_APP_NAME = "app_name";
    public static final String COLUMN_SCREEN_TIME = "screen_time";

    // SQL statement to create the new table for app names and IDs
    private static final String CREATE_TABLE_APPS = "CREATE TABLE " +
            TABLE_APPS + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_APP_ID + " TEXT UNIQUE," +
            COLUMN_APP_NAME + " TEXT" +
            ")";

    // SQL statement to create the screen_time table with a new column for insertion date
    private static final String CREATE_TABLE_SCREEN_TIME = "CREATE TABLE " +
            TABLE_SCREEN_TIME + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_APP_ID + " TEXT," +
            COLUMN_APP_NAME + " TEXT," +
            COLUMN_SCREEN_TIME + " INTEGER," +
            "insertion_date DATE DEFAULT CURRENT_DATE" + // Modified to use CURRENT_DATE for date only
            ")";



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create both tables when the database is created
        db.execSQL(CREATE_TABLE_APPS);
        db.execSQL(CREATE_TABLE_SCREEN_TIME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop both tables if they exist and recreate them
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCREEN_TIME);
        onCreate(db);
    }
    public void insertScreenTimeDataIntoDatabase(HashMap<String, Integer> appUsageMap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        try {
            for (Map.Entry<String, Integer> entry : appUsageMap.entrySet()) {
                String appName = entry.getKey();
                int screenTime = entry.getValue();
                String appId = getAppIdFromDatabase(db, appName); // Get app ID from database

                // If app ID does not exist, generate and insert new app ID
                if (appId == null) {
                    appId = generateAppId();
                    insertAppIdIntoDatabase(db, appName, appId);
                }

                // Prepare values to be inserted
                values.put(COLUMN_APP_ID, appId);
                values.put(COLUMN_APP_NAME, appName);
                values.put(COLUMN_SCREEN_TIME, screenTime);

                // Insert into database
                db.insert(TABLE_SCREEN_TIME, null, values);
            }
            //Toast.makeText(MainActivity.this, "Screen time data inserted into database.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(MainActivity.this, "Error inserting screen time data into database.", Toast.LENGTH_SHORT).show();
        } finally {
            // Close the database connection
            db.close();
        }
    }
    // Helper method to get app ID from database
    @SuppressLint("Range")
    private String getAppIdFromDatabase(SQLiteDatabase db, String appName) {
        Cursor cursor = db.query(TABLE_SCREEN_TIME,
                new String[]{COLUMN_APP_ID},
                COLUMN_APP_NAME + " = ?",
                new String[]{appName},
                null, null, null);
        String appId = null;
        if (cursor != null && cursor.moveToFirst()) {
            appId = cursor.getString(cursor.getColumnIndex(COLUMN_APP_ID));
            cursor.close();
        }
        return appId;
    }

    // Helper method to insert app ID into database
    private void insertAppIdIntoDatabase(SQLiteDatabase db, String appName, String appId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_APP_ID, appId);
        values.put(COLUMN_APP_NAME, appName);
        db.insert(TABLE_SCREEN_TIME, null, values);
    }

    // Generate a unique app ID (You can use UUID.randomUUID() for example)
    private String generateAppId() {
        // Your code to generate a unique app ID
        return UUID.randomUUID().toString();
    }
}
