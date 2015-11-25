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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.apps.weave.apis.data.WeaveDevice;

/**
 * Show controls to enable or disable LEDs on a given {@link WeaveDevice}.
 */
public class LedActivity extends AppCompatActivity implements WeaveDeviceProvider {

    public static final String EXTRA_KEY_WEAVE_DEVICE = BuildConfig.APPLICATION_ID + ".weave_device";

    private WeaveDevice mDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_leds);

        mDevice = getDeviceFromIntent(getIntent());
        if (mDevice == null) {
            throw new IllegalArgumentException("No WeaveDevice set in intent extra " +
                    EXTRA_KEY_WEAVE_DEVICE);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_leds);
        toolbar.setTitle(R.string.title_text);
        toolbar.setSubtitle(mDevice.getName());
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private WeaveDevice getDeviceFromIntent(Intent intent) {
        return intent.getParcelableExtra(EXTRA_KEY_WEAVE_DEVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_leds, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_licenses:
                new LicenseDialog().show(getSupportFragmentManager(), "Show licenses");
                return true;
            case R.id.action_refresh:
                LedSwitchesFragment fragment = (LedSwitchesFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.led_fragment);
                fragment.updateLightStates();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public WeaveDevice getDevice() {
        return mDevice;
    }

}
