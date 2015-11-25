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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.apps.weave.apis.data.Command;
import com.google.android.apps.weave.apis.data.CommandResult;
import com.google.android.apps.weave.apis.data.DeviceState;
import com.google.android.apps.weave.apis.data.WeaveApiClient;
import com.google.android.apps.weave.apis.data.WeaveDevice;
import com.google.android.apps.weave.apis.data.responses.Response;
import com.google.android.apps.weave.framework.apis.Weave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the RecyclerView displaying a list of lights, handles interactions with Weave API.
 */
public class LedSwitchesFragment extends Fragment implements OnLightToggledListener {
    private static final String TAG = "LedSwitchesFragment";

    private LedSwitchesAdapter mAdapter;

    // Instance of the WeaveApi.
    private WeaveApiClient mApiClient;
    private WeaveDevice mDevice;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_leds, container, false);

        RecyclerView recyclerView = (RecyclerView) layout.findViewById(android.R.id.list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LedSwitchesAdapter(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);

        initializeApiClient();

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDevice = ((WeaveDeviceProvider) getActivity()).getDevice();
        if (mDevice == null) {
            throw new IllegalArgumentException("Required WeaveDevice argument is null.");
        }
    }

    /**
     * Generates an Api client instance, sets up a listener to react to devices being either
     * discovered or lost.  All code related to initializing the Weave API client should go here.
     */
    private void initializeApiClient() {
        mApiClient = new WeaveApiClient(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLightStates();
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
                Command command = getSetLightStateCommand(normalizedLedIndex, lightState);

                return Weave.COMMAND_API.execute(
                        mApiClient,
                        device.getId(),
                        command);
            }

            @Override
            protected void onPostExecute(Response<CommandResult> result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (!result.isSuccess() || result.getError() != null) {
                        Log.e(TAG, "Failure setting light state: " + result.getError());
                        Snackbar.make(LedSwitchesFragment.this.getView(),
                                R.string.error_setting_state, Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        Log.i(TAG, "Success setting light state!");
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Queries the device for its current state, and extracts the data related to the current state
     * of all LEDs on the device.  The "post execute" step populates the UI with the correct number
     * of Led switches, each initialized to the correct state. E.g if the board has 3 LEDs in
     * positions "on, off, on", the UI will have 3 switches set to "on, off, on".
     */
    public void updateLightStates() {
        // Network call, punt off the main thread.
        new AsyncTask<Void, Void, Response<DeviceState>>() {

            @Override
            protected Response<DeviceState> doInBackground(Void... params) {
                return Weave.COMMAND_API.getState(mApiClient, mDevice.getId());
            }

            @Override
            protected void onPostExecute(Response<DeviceState> result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (!result.isSuccess() || result.getError() != null) {
                        Log.e(TAG, "Failure querying for state. " + result.getError());
                        Snackbar.make(LedSwitchesFragment.this.getView(),
                                R.string.error_querying_state, Snackbar.LENGTH_LONG)
                                .show();
                    }
                    else {

                        Map<String, Object> state = (Map<String, Object>)
                                result.getSuccess().getStateValue("_ledflasher");
                        if (state == null) {
                            Log.i(TAG, "Command definition Doesn't contain led flasher. " +
                                    "States are " + result.getSuccess().getStateNames().toString());
                            Snackbar.make(LedSwitchesFragment.this.getView(),
                                    R.string.error_unexpected_states, Snackbar.LENGTH_LONG)
                                    .show();
                        } else {
                            Log.i(TAG, "Success querying device for LEDs! Populating now.");

                            // Convert list of boolean states to Led Objects, use them
                            // to populate a collection of UI switches for the user.
                            ArrayList<Boolean> ledStates =  (ArrayList<Boolean>) state.get("_leds");
                            mAdapter.clear();
                            for (Boolean ledState : ledStates) {
                                mAdapter.add(new Led(ledState));
                            }

                        }
                    }
                }
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onLightToggled(int position, boolean newLightState) {
        setDeviceLightState(mDevice, position, newLightState);
    }
}
