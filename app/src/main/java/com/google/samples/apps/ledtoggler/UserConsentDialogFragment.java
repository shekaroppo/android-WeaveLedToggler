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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Presents a dialog to the user that handles consent to Google TOS.
 */
public class UserConsentDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Spanned message = android.text.Html.fromHtml(getString(R.string.tos_confirmation));

        TextView tv = new TextView(getActivity());
        tv.setText(message);

        tv.setMovementMethod(LinkMovementMethod.getInstance());

        tv.setTextSize(20);
        tv.setGravity(Gravity.CENTER);

        int spacingInPixels = dpsToPixels(16);

        builder.setView(tv, spacingInPixels, spacingInPixels, spacingInPixels, spacingInPixels)
                .setPositiveButton(R.string.tos_accept, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i("ConsentDialog", "Before onTosAccepted called.");
                        ((TosListener) getActivity()).onTosAccepted();
                        Log.i("ConsentDialog", "After onTosAccepted called.");
                    }
                })
                .setNegativeButton(R.string.tos_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((TosListener) getActivity()).onTosRejected();
                    }
                });
        return builder.create();

    }

    /**
     * Converts dp's (device independant pixels) to pixel values at runtime.
     * @param dp Device independant pixels to convert
     * @return input value in pixels.
     */
    private int dpsToPixels(int dp) {
        Resources r = getResources();
        return (int)
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public interface TosListener {
        void onTosAccepted();

        void onTosRejected();
    }
}
