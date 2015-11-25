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
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.weave.apis.data.ModelManifest;
import com.google.android.apps.weave.apis.data.WeaveDevice;


/**
 * Adapter for the list of devices. For each {@link WeaveDevice} added to this
 * adapter, a corresponding card will be created. When clicked, the card will launch a new
 * {@link LedActivity}.
 */
public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private static final String TAG = DeviceListAdapter.class.getSimpleName();

    private final ArrayMap<String, Pair<WeaveDevice, ModelManifest>> mDataSet;

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView name;
        public final TextView description;
        public final TextView deviceType;
        public final ImageView deviceImage;

        public ViewHolder(final View parentView, final TextView name,
                          final TextView description, final TextView deviceType,
                          final ImageView deviceImage) {
            super(parentView);
            this.name = name;
            this.description = description;
            this.deviceType = deviceType;
            this.deviceImage = deviceImage;

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    WeaveDevice device = mDataSet.valueAt(position).first;
                    Log.i(TAG, "Selecting device: " + device.getId());

                    Intent intent = new Intent(v.getContext(), LedActivity.class);
                    intent.putExtra(LedActivity.EXTRA_KEY_WEAVE_DEVICE, device);
                    v.getContext().startActivity(intent);
                }
            };

            parentView.setOnClickListener(clickListener);
        }
    }

    public DeviceListAdapter() {
        mDataSet = new ArrayMap<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_device, parent, false);
        return new ViewHolder(v,
                (TextView) v.findViewById(R.id.device_title),
                (TextView) v.findViewById(R.id.device_description),
                (TextView) v.findViewById(R.id.device_device_type),
                (ImageView) v.findViewById(R.id.device_picture));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Pair<WeaveDevice, ModelManifest> data= mDataSet.valueAt(position);
        holder.name.setText(data.first.getName());
        holder.description.setText(data.first.getDescription());

        if (data.first.getDiscoveryTransport().hasCloud()) {
            holder.deviceImage.setImageResource(R.drawable.ic_developer_board_blue_48dp);
        } else {
            holder.deviceImage.setImageResource(R.drawable.ic_developer_board_grey_48dp);
        }
        if (data.second == null) {
            holder.deviceType.setText(R.string.unknown_device_type);
        } else {
            holder.deviceType.setText(data.second.getModelName());
        }
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void add(WeaveDevice device, ModelManifest manifest) {
        mDataSet.put(device.getId(), new Pair<>(device, manifest));
    }

    public void remove(WeaveDevice device) {
        mDataSet.remove(device.getId());
    }
}
