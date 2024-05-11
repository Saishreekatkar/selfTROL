package com.selftrol.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

public class LockScreenActivity extends Activity {

    private ImageView unlockImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        // Prevent the user from interacting with the activity
        setFinishOnTouchOutside(false);
    }
}
