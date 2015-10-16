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

public class Led {

    private boolean mLightOn = false;
    private int mType = 2;

    public Led() {
        mLightOn = false;
    }

    public Led(boolean lightOn) {
        mLightOn = lightOn;
    }

    public boolean isLightOn() {
        return mLightOn;
    }

    public boolean toggleLight() {
        mLightOn = !mLightOn;
        return mLightOn;
    }

    public String ledStateAsString() {
        return mLightOn ? "on" : "off";
    }
}
