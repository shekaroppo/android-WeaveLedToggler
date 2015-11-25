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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Presents a dialog to the user that handles consent to Google TOS.
 */
public class UserConsentDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Spanned message = android.text.Html.fromHtml(getString(R.string.tos_confirmation));

        TextView tv = new TextView(getActivity());
        tv.setText(message);

        tv.setMovementMethod(LinkMovementMethod.getInstance());

        tv.setTextSize(20);
        tv.setGravity(Gravity.CENTER);

        int spacingInPixels = dpsToPixels(16);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(tv, spacingInPixels, spacingInPixels, spacingInPixels, spacingInPixels)
                .setPositiveButton(R.string.tos_accept, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onTosAccepted();
                    }
                })
                .setNegativeButton(R.string.tos_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onTosRejected();
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        setCancelable(false);
        return dialog;

    }

    /**
     * Converts dp's (device independent pixels) to pixel values at runtime.
     * @param dp Device independent pixels to convert
     * @return input value in pixels.
     */
    private int dpsToPixels(int dp) {
        Resources r = getResources();
        return (int)
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    /**
     * When the user accepts TOS, store as a preference.
     * @return Whether the user has signed TOS or not.
     */
    public static boolean showIfNecessary(FragmentActivity activity) {
        if (!isTosAccepted(activity)) {
            String fragmentTag = "Accept_TOS";
            if (activity.getSupportFragmentManager().findFragmentByTag(fragmentTag) == null) {
                new UserConsentDialogFragment().show(
                        activity.getSupportFragmentManager(), fragmentTag);
            }
            return true;
        }
        return false;
    }

    public static boolean isTosAccepted(FragmentActivity activity) {
        String key = activity.getString(R.string.pref_tos_key);
        return activity.getPreferences(Context.MODE_PRIVATE)
                .getBoolean(key, false);
    }

    public void onTosRejected() {
        ((TosListener) getActivity()).onTosRejected();
    }

    public void onTosAccepted() {
        // When TOS is accepted, save an indicator of acceptance using shared preferences.
        SharedPreferences prefs = getActivity().getPreferences(Context.MODE_PRIVATE);
        prefs.edit().putBoolean(getString(R.string.pref_tos_key), true).apply();
        ((TosListener) getActivity()).onTosAccepted();
    }

    public interface TosListener {
        void onTosAccepted();

        void onTosRejected();
    }
}
