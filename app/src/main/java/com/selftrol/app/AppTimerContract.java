package com.selftrol.app;

public class AppTimerContract {
    public static final String TABLE_NAME = "apptimers";
    public static final String COLUMN_PACKAGE_NAME = "packagename";
    public static final String COLUMN_TIMER_DURATION = "timerduration";
    public static final String COLUMN_TIME_USED = "timeused";
    public static final String COLUMN_FLAG = "flag"; // New column name

    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_PACKAGE_NAME + " TEXT PRIMARY KEY," +
                    COLUMN_TIMER_DURATION + " INTEGER," +
                    COLUMN_TIME_USED + " INTEGER DEFAULT 0," +
                    COLUMN_FLAG + " TEXT DEFAULT 'false')";

    public static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
}
