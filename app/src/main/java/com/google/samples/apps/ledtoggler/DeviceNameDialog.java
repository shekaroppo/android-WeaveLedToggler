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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

/**
 * Allows user to modify the "Device name" setting via a simple dialog instead of a settings screen.
 */
public class DeviceNameDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Device Name");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                EditText nameField = (EditText) ((Dialog) dialogInterface)
                        .findViewById(R.id.dialog_devicename_field);
                String newName = nameField.getText().toString();
                setDeviceNamePreference(newName);
            }
        });

        builder.setNegativeButton("Cancel", null);
        View dialogView = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_device_name_chooser, null);


        EditText nameField = (EditText) dialogView.findViewById(R.id.dialog_devicename_field);
        nameField.setText(getCurrentDeviceNamePreference());
        builder.setView(dialogView);

        return builder.create();
    }

    /**
     * Retrieves the current device name to search for from preferences.
     * @return the target device name which should be used by the app to search for a weave device.
     */
    private String getCurrentDeviceNamePreference() {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

        return prefs.getString(getString(R.string.pref_device_name_key),
                getString(R.string.pref_device_name_default));
    }

    /**
     * Sets the name that should be used by the app to search for a weave device.
     * @param newName The new target device name to use while searching for a weave device.
     */
    private void setDeviceNamePreference(String newName) {
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        prefs.edit().putString(getString(R.string.pref_device_name_key), newName).apply();
    }
}
