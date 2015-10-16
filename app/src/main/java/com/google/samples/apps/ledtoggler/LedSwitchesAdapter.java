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


public class LedSwitchesAdapter extends RecyclerView.Adapter<LedSwitchesAdapter.ViewHolder> {
    private OnLightToggledListener lightToggledListener;
    private ArrayList<Led> mDataSet;

    private static final String TAG = "LedSwitchesAdapter";

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {

        //
        public Switch toggler;
        public View parentView;

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

    // Provide a suitable constructor (depends on the kind of dataset)
    public LedSwitchesAdapter(OnLightToggledListener lightToggledListener, ArrayList<Led> myDataset) {
        this.lightToggledListener = lightToggledListener;
        mDataSet = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(v,
                (Switch) v.findViewById(R.id.toggler));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        int normalizedPosition = position + 1;
        // holder.toggler.setContentDescription("L E D " + normalizedPosition);
        holder.toggler.setText("L E D  " + normalizedPosition);

        // Nobody pronounces LED as "lead".
        holder.toggler.setChecked(mDataSet.get(position).isLightOn());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


}
