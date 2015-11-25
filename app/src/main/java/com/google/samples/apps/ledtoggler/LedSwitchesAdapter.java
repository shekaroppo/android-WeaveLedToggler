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

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import java.util.ArrayList;

/**
 * Adapter for the list of switches representing device LEDs. For each {@link Led} added to this
 * adapter, a corresponding {@link Switch} will be created that, when clicked, will trigger a
 * callback on the given {@link OnLightToggledListener}.
 */
public class LedSwitchesAdapter extends RecyclerView.Adapter<LedSwitchesAdapter.ViewHolder> {
    private static final String TAG = LedSwitchesAdapter.class.getSimpleName();

    private final OnLightToggledListener lightToggledListener;

    private final ArrayList<Led> mDataSet;
    private String mLedLabel;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final Switch toggler;
        public final View parentView;

        public ViewHolder(final View parentView, final Switch toggler) {
            super(parentView);
            this.toggler = toggler;
            this.parentView = parentView;

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    Led led = mDataSet.get(position);

                    // Update the internal model
                    led.toggleLight();

                    boolean lightOn = led.isLightOn();

                    // Update the UI to reflect new state.
                    toggler.setChecked(lightOn);

                    // Update the light
                    lightToggledListener.onLightToggled(position, lightOn);
                    Log.i(TAG, "Toggler changed to state: " + ViewHolder.this.toggler.isChecked());
                }
            };

            // parentView.setOnClickListener(clickListener);
            toggler.setOnClickListener(clickListener);
        }
    }

    public LedSwitchesAdapter(OnLightToggledListener lightToggledListener) {
        this.lightToggledListener = lightToggledListener;
        mDataSet = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_led, parent, false);
        return new ViewHolder(v,
                (Switch) v.findViewById(R.id.toggler));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (mLedLabel == null) {
            // The LED label is the same for every view on this adapter, so we cache it for
            // performance reasons.
            mLedLabel = holder.parentView.getResources().getString(R.string.led_text);
        }
        int normalizedPosition = position + 1;
        holder.toggler.setText(String.format(mLedLabel, normalizedPosition));

        holder.toggler.setChecked(mDataSet.get(position).isLightOn());
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void add(Led led) {
        mDataSet.add(led);
        notifyItemInserted(mDataSet.size() - 1);
    }

    public void clear() {
        mDataSet.clear();
        notifyDataSetChanged();
    }
}
