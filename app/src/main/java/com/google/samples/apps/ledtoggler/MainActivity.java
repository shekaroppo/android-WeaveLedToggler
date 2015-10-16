/*
 * Copyright (C) 2015 Google
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.ledtoggler;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

/**
 * Bare Activity, handles application's lifecycle.
 * Nothing "Weavy" happens in this class.  For something juicier, check out the
 * LedSwitchesFragment class.
 */
public class MainActivity extends AppCompatActivity
        implements UserConsentDialogFragment.TosListener {

    private final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the user has already accepted TOS, go ahead and load UI components, start up the
        // device scan.  If not, kick off the user consent flow, which can load content when it
        // returns a positive result.
        if(hasUserAcceptedTos()) {
            setContentView(R.layout.activity_main);
        } else {
            confirmTos();
        }
    }

    public void confirmTos() {
        new UserConsentDialogFragment().show(getSupportFragmentManager(), "Accept TOS");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * When the user accepts TOS, store as a preference.
     * @return Whether the user has signed TOS or not.
     */
    private boolean hasUserAcceptedTos() {
        String key = getString(R.string.pref_tos_key);
        return getPreferences(Context.MODE_PRIVATE).getBoolean(key, false);
    }

    @Override
    public void onTosAccepted() {
        // When TOS is accepted, save an indicator of acceptance using shared preferences.
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        prefs.edit().putBoolean(getString(R.string.pref_tos_key), true).apply();

        // Then, continue the app flow including device scanning, OAUTH consent screens, etc.
        setContentView(R.layout.activity_main);

    }

    @Override
    public void onTosRejected() {
        // If the user rejected TOS, exit the app, as there's absolutely nothing available for them
        // to do.
        finish();
    }
}
