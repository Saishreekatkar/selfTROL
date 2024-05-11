package com.selftrol.app;

import static java.security.AccessController.getContext;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppTimerDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "AppTimer.db";

    public AppTimerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(AppTimerContract.SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(AppTimerContract.SQL_DELETE_TABLE);
        onCreate(db);
    }

    // Create operation
    public long addAppTimer(String packageName, long timerDuration) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if the package already exists in the database
        if (isPackageExists(db, packageName)) {
            // Package exists, update the timer duration
            updateTimerDuration(packageName, timerDuration);
        } else {
            // Package doesn't exist, insert new data
            ContentValues values = new ContentValues();
            values.put(AppTimerContract.COLUMN_PACKAGE_NAME, packageName);
            values.put(AppTimerContract.COLUMN_TIMER_DURATION, timerDuration);
            db.insert(AppTimerContract.TABLE_NAME, null, values);
        }

        db.close();
        return timerDuration;
    }

    // Check if package already exists in the database
    private boolean isPackageExists(SQLiteDatabase db, String packageName) {
        Cursor cursor = db.query(
                AppTimerContract.TABLE_NAME,
                null,
                AppTimerContract.COLUMN_PACKAGE_NAME + "=?",
                new String[]{packageName},
                null,
                null,
                null
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // Method to get package names from the database
    public String[] getPackageNames() {
        if (getContext() == null) {
            // Handle null context appropriately
            throw new IllegalStateException("Context is null in AppTimerDbHelper");
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(AppTimerContract.TABLE_NAME,
                new String[]{AppTimerContract.COLUMN_PACKAGE_NAME},
                null,
                null,
                null,
                null,
                null);

        List<String> packageNamesList = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String packageName = cursor.getString(cursor.getColumnIndex(AppTimerContract.COLUMN_PACKAGE_NAME));
                packageNamesList.add(packageName);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();

        // Convert ArrayList to array
        return packageNamesList.toArray(new String[0]);
    }



    // Read operation
    public int getTimerDuration(String packageName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(AppTimerContract.TABLE_NAME,
                new String[]{AppTimerContract.COLUMN_TIMER_DURATION},
                AppTimerContract.COLUMN_PACKAGE_NAME + "=?",
                new String[]{packageName}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        assert cursor != null;
        @SuppressLint("Range") int duration = cursor.getInt(cursor.getColumnIndex(AppTimerContract.COLUMN_TIMER_DURATION));
        cursor.close();
        db.close();
        return duration;
    }

    // Read operation to retrieve time used for a package name
    @SuppressLint("Range")
    public int getTimerUsage(String packageName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                AppTimerContract.TABLE_NAME,
                new String[]{AppTimerContract.COLUMN_TIME_USED},
                AppTimerContract.COLUMN_PACKAGE_NAME + "=?",
                new String[]{packageName},
                null,
                null,
                null
        );
        int timeUsed = 0;
        if (cursor != null && cursor.moveToFirst()) {
            timeUsed = cursor.getInt(cursor.getColumnIndex(AppTimerContract.COLUMN_TIME_USED));
            cursor.close();
        }
        db.close();
        return timeUsed;
    }


    // Update operation
    public int updateTimerDuration(String packageName, long newDuration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppTimerContract.COLUMN_TIMER_DURATION, newDuration);
        int rowsAffected = db.update(AppTimerContract.TABLE_NAME, values,
                AppTimerContract.COLUMN_PACKAGE_NAME + "=?", new String[]{packageName});
        db.close();
        return rowsAffected;
    }

    // Update operation
    public int updateTimerUsage(String packageName, long timeUsed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppTimerContract.COLUMN_TIME_USED, timeUsed);
        int rowsAffected = db.update(AppTimerContract.TABLE_NAME, values,
                AppTimerContract.COLUMN_PACKAGE_NAME + "=?", new String[]{packageName});
        db.close();
        return rowsAffected;
    }


    // Delete operation
    public void deleteAppTimer(String packageName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(AppTimerContract.TABLE_NAME,
                AppTimerContract.COLUMN_PACKAGE_NAME + "=?", new String[]{packageName});
        db.close();
    }

    // Update operation to set time used to zero for all records
    public void updateTimeUsedToZero() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppTimerContract.COLUMN_TIME_USED, 0);
        db.update(AppTimerContract.TABLE_NAME, values, null, null);
        db.close();
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                AppTimerContract.TABLE_NAME, // The table to query
                null, // The array of columns to return (null to return all columns)
                null, // The columns for the WHERE clause
                null, // The values for the WHERE clause
                null, // don't group the rows
                null, // don't filter by row groups
                null // don't order the rows
        );
    }

    public String getFlagValue(String packageName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String flagValue = null;
        String query = "SELECT " + AppTimerContract.COLUMN_FLAG + " FROM " + AppTimerContract.TABLE_NAME +
                " WHERE " + AppTimerContract.COLUMN_PACKAGE_NAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{packageName});
        if (cursor.moveToFirst()) {
            flagValue = cursor.getString(cursor.getColumnIndexOrThrow(AppTimerContract.COLUMN_FLAG));
        }
        cursor.close();
        return flagValue;
    }

    // Update flag value query
    public void updateFlagValue(String packageName, String newFlagValue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppTimerContract.COLUMN_FLAG, newFlagValue);
        String whereClause = AppTimerContract.COLUMN_PACKAGE_NAME + " = ?";
        String[] whereArgs = {packageName};
        db.update(AppTimerContract.TABLE_NAME, values, whereClause, whereArgs);
    }

    // Update all flags to false query
    public void updateAllFlagsToFalse() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(AppTimerContract.COLUMN_FLAG, "false");
        db.update(AppTimerContract.TABLE_NAME, values, null, null);
    }

}
