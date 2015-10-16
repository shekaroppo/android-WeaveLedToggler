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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.apps.weave.apis.data.Command;
import com.google.android.apps.weave.apis.data.CommandResult;
import com.google.android.apps.weave.apis.data.DeviceState;
import com.google.android.apps.weave.apis.data.WeaveApiClient;
import com.google.android.apps.weave.apis.data.WeaveDevice;
import com.google.android.apps.weave.apis.data.responses.Response;
import com.google.android.apps.weave.apis.device.DeviceLoaderCallbacks;
import com.google.android.apps.weave.apis.device.DeviceLoaderStatusListener;
import com.google.android.apps.weave.framework.apis.Weave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the RecyclerView displaying a list of lights, handles interactions with Weave API.
 */
public class LedSwitchesFragment extends Fragment
        implements DeviceLoaderStatusListener, OnLightToggledListener {

    private RecyclerView.Adapter mAdapter;

    private ArrayList<Led> mDataSet;

    // The callback for reacting to device discovery.
    private DeviceLoaderCallbacks mDiscoveryListener;

    // Instance of the WeaveApi.
    private WeaveApiClient mApiClient;

    // Reference to the device this application will be communicating with
    private WeaveDevice mDevice;

    // This application uses the device's name property as an indication of which device
    // to send commands to.
    private String mDeviceName;

    private static final String TAG = "LedSwitchesFragment";

    // PreferenceManager only holds weak references to preference change listeners.
    // Unless the application code keeps a reference, there's a chance the ref will get
    // garbage-collected
    SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener;


    public LedSwitchesFragment() {
    }

    /**
     * Refresh device / LED state by clearing current data and re-discovering the desired device.
     * Normally one should just query the saved device for device state if a refresh needs to occur,
     * but this app will be used for testing LED Flasher devices as they're being set up, so method
     * is intentionally heavy-handed and employs a "start from scratch, verify everything works"
     * method.
     */
    private void refreshDevices() {
        clearDataSet();
        if (Weave.DEVICE_API.isLoading(mApiClient)) {
            stopDiscovery();
        }
        startDiscovery();
    }

    /**
     * Wipes out the cached set of Led states.
     */
    private void clearDataSet() {
        if (mDataSet != null) {
            int size = mDataSet.size();
            mDataSet.clear();
            mAdapter.notifyItemRangeRemoved(0,size);
        } else {
            mDataSet = new ArrayList<>();
        }
    }

    /**
     * Given a list of Leds states, populates the internal state and view.
     * @param leds led states to display to the user.
     */
    public void initializeDataSet(ArrayList<Led> leds) {
        clearDataSet();
        if(leds != null) {
            mDataSet.addAll(leds);
        }
        if (mAdapter != null) {
            mAdapter.notifyItemRangeInserted(0, mDataSet.size());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        initializeApiClient();
        initializePreferenceListener();

        View layout = inflater.inflate(R.layout.fragment_main, container, false);

        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        activity.setTitle(getString(R.string.title_text));

        toolbar.inflateMenu(R.menu.menu_main);

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == R.id.action_refresh) {
                    refreshDevices();
                    return true;
                } else if (id == R.id.action_devicename) {
                    DeviceNameDialog dialog = new DeviceNameDialog();
                    dialog.show(getFragmentManager(), "Device Name");
                } else if (id == R.id.action_licenses) {
                    new LicenseDialog().show(getFragmentManager(), "Open Source Licenses");
                }
                return false;
            }
        });

        RecyclerView recyclerView = (RecyclerView) layout.findViewById(android.R.id.list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);


        // specify an adapter (see also next example)
        initializeDataSet(null);
        mAdapter = new LedSwitchesAdapter(this, mDataSet);
        recyclerView.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        startDiscovery();
    }

    @Override
    public void onPause() {
        stopDiscovery();
        super.onPause();
    }


    /** Begins a scan for weave-accessible devices.  Searches for both cloud devices associated with
     * the user's account, and provisioned weave devices sitting on the same network.
     */
    public void startDiscovery() {
        if (getActivity() != null) {
            mDeviceName = getDeviceNamePreference();
            Weave.DEVICE_API.addLoadStatusListener(mApiClient, this);
            Weave.DEVICE_API.startLoading(mApiClient, mDiscoveryListener);

            // Indicate to the user that the search is on!
            getView().findViewById(R.id.waiting_screen).setVisibility(View.VISIBLE);
        }

        // Clear the dataset BEFORE showing the progress bar so the list doesn't jankily shift down
        // when the progress bar appears.
        clearDataSet();

    }

    /**
     * Stops device discovery.  Device discovery is battery intensive, so  this should be called
     * as soon as further discovery is no longer needed.
     */
    public void stopDiscovery() {
        Weave.DEVICE_API.removeLoadStatusListener(mApiClient, this);
        Weave.DEVICE_API.stopLoading(mApiClient, mDiscoveryListener);
    }

    /**
     * Helper method to pull the "device name" from preferences.  This is the name that will be used
     * during discovery to choose which device to send commands to.
     * @return name of the device to search for.
     */
    private String getDeviceNamePreference() {
        String key = getString(R.string.pref_device_name_key);
        String defaultName = getString(R.string.pref_device_name_default);
        return getActivity().getPreferences(Context.MODE_PRIVATE).getString(key, defaultName);
    }

    /**
     * Generates an Api client instance, sets up a listener to react to devices being either
     * discovered or lost.  All code related to initializing the Weave API client should go here.
     */
    private void initializeApiClient() {
        mDiscoveryListener = new DeviceLoaderCallbacks() {
            @Override
            public void onDevicesFound(WeaveDevice[] weaveDevices) {
                Log.i(TAG, "Found Device(s)!");
                Log.d(TAG, "Scanning for devices called: " + mDeviceName);
                for (WeaveDevice device : weaveDevices) {
                    Log.i(TAG, "\t" + device.getName()
                            + "\n\t" + device.getDescription() + "\n\t" + device.getAccountName());

                    if (device.getName().equals(mDeviceName)

                            && device.getDiscoveryTransport().hasCloud()) {
                        mDevice = device;
                        stopDiscovery();

                        getInitialLightStates(device);
                    }
                }
            }

            @Override
            public void onDevicesLost(WeaveDevice[] weaveDevices) {
                Log.i(TAG, "Lost Device(s)!");
                for (WeaveDevice device : weaveDevices) {
                    Log.i(TAG, "\t" + device.toString());
                    if(mDevice != null && mDevice.getId().equals(device.getId())) {
                        mDevice = null;
                        clearDataSet();
                    }
                }
            }
        };
        Weave.EAP_API.initialize(getContext());
        mApiClient = new WeaveApiClient(getContext());
    }

    /**
     * Initializes a listener to respond to changes in the device name preference.  The listener
     * should trigger a device discovery to react to the new name, and wipe out the current internal
     * state, as that's related to the previous device.
     */
    private void initializePreferenceListener() {
        mPrefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                refreshDevices();
            }
        };

        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
    }

    /**
     * Creates a Weave command for adjusting a single LED.
     * @param ledIndex The index of the LED to adjust.
     * @param lightOn Whether the LED should be on or not.
     * @return an executable weave command to toggle the LED to the desired state.
     */
    public Command getSetLightStateCommand(int ledIndex, boolean lightOn) {
        HashMap<String, Object> commandParams = new HashMap<>();
        commandParams.put("_led", ledIndex);
        commandParams.put("_on", lightOn);
        return new Command()
                .setName("_ledflasher._set")
                .setParameters(commandParams);
    }

    /**
     * Sets the state of a single LED
     * @param device The target weave device
     * @param ledIndex The index of the LED to adjust
     * @param lightState Whether the LED should be "on" or not.
     */
    public void setDeviceLightState(final WeaveDevice device, final int ledIndex,
                                    final boolean lightState) {
        // Listview is 0-based, Led index in the brillo app is 1-based.
        final int normalizedLedIndex = ledIndex + 1;

        // Network call, punt off the main thread.
        new AsyncTask<Void, Void, Response<CommandResult>>() {

            @Override
            protected Response<CommandResult> doInBackground(Void... params) {
                if (device != null && device.getName().equals((mDeviceName))) {
                    Command command = getSetLightStateCommand(normalizedLedIndex, lightState);

                    return Weave.COMMAND_API.execute(
                            mApiClient,
                            device.getId(),
                            command);
                } else {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Response<CommandResult> result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (!result.isSuccess() || result.getError() != null) {
                        Log.e(TAG, "Failure setting light state!",
                                result.getError().getException());
                    } else {
                        Log.i(TAG, "Success setting light state!");
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Queries the device for its current state, and extracts the data related to the curernt state
     * of all LEDs on the device.  The "post execute" step populates the UI with the correct number
     * of Led switches, each initialized to the correct state. E.g if the board has 3 Leds in
     * positions "on, off, on", the UI will have 3 switches set to "on, off, on".
     * @param device the device to query Led state from.
     */
    public void getInitialLightStates(final WeaveDevice device) {
        // Network call, punt off the main thread.
        new AsyncTask<Void, Void, Response<DeviceState>>() {

            @Override
            protected Response<DeviceState> doInBackground(Void... params) {
                if (device != null) {
                    String deviceId = device.getId();
                    return Weave.COMMAND_API.getState(mApiClient, deviceId);
                } else {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Response<DeviceState> result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (!result.isSuccess() || result.getError() != null) {
                        Log.e(TAG, "Failure querying for LEDs", result.getError().getException());
                    }
                    else {

                        if (!result.getSuccess().getStateValues().containsKey("_ledflasher")) {
                            Log.d(TAG, "Command definition Doesn't contain led flasher.");
                            Log.i(TAG, result.getSuccess().getStateValues().toString());
                        } else {
                            Log.i(TAG, "Success querying device for LEDs! Populating now.");
                            Map<String, Object> state = (Map<String, Object>)
                                    result.getSuccess().getStateValues().get("_ledflasher");


                            // Convert list of boolean states to Led Objects, use them
                            // to populate a collection of UI switches for the user.
                            ArrayList<Boolean> ledStates =  (ArrayList<Boolean>) state.get("_leds");
                            ArrayList<Led> leds = new ArrayList<>();
                            for (Boolean ledState : ledStates) {
                                leds.add(new Led(ledState));
                            }

                            initializeDataSet(leds);
                        }

                        // Hide the progress bar, since the device search and query is complete.
                        getActivity().findViewById(R.id.waiting_screen).setVisibility(View.GONE);
                    }
                }
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onLoaderStarted() {
        Log.i(TAG, "Discovery started.");
    }

    @Override
    public void onLoaderStopped() {
        Log.i(TAG, "Discovery stopped.");
    }

    @Override
    public void onLightToggled(int position, boolean newLightState) {
        setDeviceLightState(mDevice, position, newLightState);
    }
}
