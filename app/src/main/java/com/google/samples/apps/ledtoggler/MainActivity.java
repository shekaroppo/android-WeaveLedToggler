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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.apps.weave.apis.appaccess.AppAccessRequest;
import com.google.android.apps.weave.apis.data.ModelManifest;
import com.google.android.apps.weave.apis.data.WeaveApiClient;
import com.google.android.apps.weave.apis.data.WeaveDevice;
import com.google.android.apps.weave.apis.data.responses.Response;
import com.google.android.apps.weave.apis.data.responses.ResultCode;
import com.google.android.apps.weave.apis.device.DeviceLoaderCallbacks;
import com.google.android.apps.weave.framework.apis.Weave;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows a list of devices with the specified uiDeviceKind. On the first run, it starts the
 * Weave device authorization flow that requests the user to grant your app access to specific
 * devices.
 * After the first run, the Weave device authorization flow can be triggered again using the
 * "Authorize new devices" toolbar action.
 *
 * ATTENTION: Before using this class, make sure you set the constants
 * {@link #CLOUD_PROJECT_NUMBER} and {@link #DEVICE_TYPE} appropriately.
 */
public class MainActivity extends AppCompatActivity
        implements UserConsentDialogFragment.TosListener {

    private final static String TAG = "MainActivity";

    private static final int REQUEST_CODE_DEVICE_ACCESS = 111;
    private static final int REQUEST_CODE_RESOLUTION_REQUIRED = 112;

    // ATTENTION: CONFIGURE THE FOLLOWING STRINGS BASED ON YOUR
    // CLOUD AND WEAVE PROJECT:
    /**
     * Project number of a Google Cloud project with Weave API enabled and with proper
     * Android OAuth Client credentials associated with this app. Note that this is not
     * the project ID, but the project number. It should be a long integer number.
     */
    private static final String CLOUD_PROJECT_NUMBER = "REPLACE_WITH_YOUR_PROJECT_NUMBER";

    /**
     * Type of the devices that this app will request access for. Common types are "vendor"
     * and "toy".
     * {@see <a href="https://developers.google.com/weave/v1/dev-guides/device-behavior/schema-library#uidevicekind">Weave docs</a>}
     */
    private static final String DEVICE_TYPE = "vendor";
    // END OF REQUIRED CONFIGURATION.

    private WeaveApiClient mApiClient;

    private DeviceListAdapter mDeviceListAdapter;
    private ConcurrentHashMap<String, ModelManifest> manifestCache;

    private final DeviceLoaderCallbacks mDiscoveryListener = new DeviceLoaderCallbacks() {
        @Override
        public void onDevicesFound(WeaveDevice[] weaveDevices) {
            Log.i(TAG, "Found Device(s)!");
            for (final WeaveDevice device : weaveDevices) {
                Log.i(TAG, "Found device: " + device.getName()
                        + "\n\t" + device.getDescription() + "\n\t" + device.getAccountName());
                addDevice(device);
            }
        }

        @Override
        public void onDevicesLost(WeaveDevice[] weaveDevices) {
            Log.i(TAG, "Lost Device(s)!");
            for (WeaveDevice device : weaveDevices) {
                Log.i(TAG, "Lost device: " + device.getName());
                mDeviceListAdapter.remove(device);
            }
            mDeviceListAdapter.notifyDataSetChanged();
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_devices);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.devices_list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        manifestCache = new ConcurrentHashMap<>();

        // specify an adapter
        mDeviceListAdapter = new DeviceListAdapter();
        recyclerView.setAdapter(mDeviceListAdapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestDeviceAccess();
            }
        });

        // If the user has already accepted TOS, go ahead and start up the device flow.
        // If not, kick off the user consent flow, which can load content when it
        // returns a positive result.
        if (!UserConsentDialogFragment.showIfNecessary(this)) {
            doDeviceAuthFlow();
        }
    }

    @Override
    public void onPause() {
        stopDiscovery();
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_licenses:
                new LicenseDialog().show(getSupportFragmentManager(), "Show licenses");
                return true;
            case R.id.action_authorize_more_devices:
                requestDeviceAccess();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTosRejected() {
        // If the user rejected TOS, exit the app, as there's absolutely nothing available
        // for them to do.
        finish();
    }

    @Override
    public void onTosAccepted() {
        doDeviceAuthFlow();
    }

    public void doDeviceAuthFlow() {
        if (!CLOUD_PROJECT_NUMBER.matches("[0-9]+")) {
            Log.e(TAG, "Invalid CLOUD_PROJECT_NUMBER: " + CLOUD_PROJECT_NUMBER);
            Toast.makeText(this, R.string.error_invalid_cloud_project_number, Toast.LENGTH_LONG)
                    .show();
            finish();
        }
        if (!UserConsentDialogFragment.isTosAccepted(this)) {
            finish();
        }
        mApiClient = new WeaveApiClient(this);

        boolean firstRun = getPreferences(Context.MODE_PRIVATE).getBoolean("first_run", true);
        if (firstRun) {
            getPreferences(Context.MODE_PRIVATE).edit().putBoolean("first_run", false).apply();
            requestDeviceAccess();
        } else {
            startDiscovery();
        }
    }

    private void requestDeviceAccess() {
        if (mApiClient == null) {
            return;
        }
        AppAccessRequest request = new AppAccessRequest.Builder(
                    AppAccessRequest.APP_ACCESS_ROLE_USER, DEVICE_TYPE,
                    CLOUD_PROJECT_NUMBER)
                .build();

        Response<Intent> accessResponse = Weave
                .APP_ACCESS_API.getRequestAccessIntent(mApiClient,
                        request);
        if (accessResponse.isSuccess()) {
            Log.d(TAG, "Successfully created RequestAccessIntent: " + accessResponse.getSuccess());
            startActivityForResult(accessResponse.getSuccess(), REQUEST_CODE_DEVICE_ACCESS);
        } else if (accessResponse.getError().getErrorCode() == ResultCode.RESOLUTION_REQUIRED) {
            Log.w(TAG, "Could not create RequestAccessIntent, trying resolution intent: " +
                    accessResponse.getError());
            startActivityForResult(accessResponse.getError().getResolutionIntent(),
                    REQUEST_CODE_RESOLUTION_REQUIRED);
        } else {
            Log.e(TAG, "Could not create RequestAccessIntent. No resolution intent provided. " +
                    "Error: " + accessResponse.getError());
            Snackbar.make(findViewById(R.id.devices_list),
                    R.string.error_request_access, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(TAG, "Result onActivityResult: " + data + "  resultCode=" + resultCode +
                " requestCode=" + requestCode);
        if (requestCode == REQUEST_CODE_DEVICE_ACCESS ||
                requestCode == REQUEST_CODE_RESOLUTION_REQUIRED) {
            startDiscovery();
        }
    }



    /** Begins a scan for weave-accessible devices.  Searches for both cloud devices associated with
     * the user's account, and provisioned weave devices sitting on the same network.
     */
    public void startDiscovery() {
        if (mApiClient != null) {
            Weave.DEVICE_API.startLoading(mApiClient, mDiscoveryListener);
        }
    }

    /**
     * Stops device discovery. Device discovery is battery intensive, so this should be called
     * as soon as further discovery is no longer needed.
     */
    public void stopDiscovery() {
        if (mApiClient != null) {
            Weave.DEVICE_API.stopLoading(mApiClient, mDiscoveryListener);
        }
    }

    private void addDevice(final WeaveDevice device) {
        // We don't really need the ModelManifest except to show more information to the user,
        // like the device image and device type. But since we use it, we need to fetch it off
        // the main thread, because it will potentially trigger a network call.
        new AsyncTask<Void, Void, ModelManifest>() {

            @Override
            protected ModelManifest doInBackground(Void... params) {
                String manifestId = device.getModelManifestId();
                ModelManifest manifest = manifestCache.get(manifestId);
                if (manifest == null) {
                    manifest = Weave.DEVICE_API.getModelManifest(mApiClient, manifestId)
                            .getSuccess();
                    if (manifest != null) {
                        manifestCache.put(manifestId, manifest);
                    }
                }
                return manifest;
            }

            @Override
            protected void onPostExecute(ModelManifest manifest) {
                mDeviceListAdapter.add(device, manifest);
                mDeviceListAdapter.notifyDataSetChanged();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
