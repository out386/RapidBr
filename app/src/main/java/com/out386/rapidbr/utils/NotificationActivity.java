package com.out386.rapidbr.utils;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.out386.rapidbr.MainActivity;

/**
 * By David Wasser and Raginmari
 * https://stackoverflow.com/a/9532756
 * https://stackoverflow.com/a/7286683
 */

public class NotificationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isTaskRoot()) {
            // Start the app before finishing
            Intent startAppIntent = new Intent(getApplicationContext(), MainActivity.class);
            startAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startAppIntent);
        }
        // Now finish, which will drop the user in to the activity that was at the top
        //  of the task stack
        finish();
    }
}
