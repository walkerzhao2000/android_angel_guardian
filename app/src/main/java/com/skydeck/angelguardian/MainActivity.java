/*
  Copyright:
  Walker Zhao (the owner of this git repo) takes full privilege and ownership of the code.
  No distribution by anyone else.
  Will change to public license when the code is ready at owner's announcement.
 */

package com.skydeck.angelguardian;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    static final String PREFS_NAME = "preferences";
    static final String PREF_USERNAME = "Username";
    static final String PREF_PASSWORD = "Password";
    static final String DEFAULT_USERNAME = "";
    static final String DEFAULT_PASSWORD = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Make sure this is before calling super.onCreate
        setTheme(R.style.splashScreenTheme);
        super.onCreate(savedInstanceState);

        if (isSignIn()) {
            // username and password are available, so go to location activity.
            Intent intentLocation = new Intent(this, LocationActivity.class);
            startActivity(intentLocation);
        } else {
            // username and password are not available, so go to account activity.
            Intent intentAccount = new Intent(this, AccountActivity.class);
            startActivity(intentAccount);
        }
    }

    private boolean isSignIn() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);

        // Get value
        String username = settings.getString(PREF_USERNAME, DEFAULT_USERNAME);
        String password = settings.getString(PREF_PASSWORD, DEFAULT_PASSWORD);

        return !(DEFAULT_USERNAME.equals(username) || DEFAULT_PASSWORD.equals(password));
    }
}
