/*
 * Copyright (C) 2014 The X-Droid Project
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
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.provider.Settings;
import android.os.UserManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;

/**
 * Layout for the location mode detail in quick settings.
 */
public class LocationDetailView extends LinearLayout {
    private static final String TAG = "LocationDetailView"; 

    private RadioGroup mLocationModes;
    private RadioButton mHighAccuracy;
    private RadioButton mBatterySaving;
    private RadioButton mSensorsOnly;
    private TextView tHighAccuracy = (TextView) findViewById(R.id.high_accuracy_summary);
    private TextView tBatterySaving = (TextView) findViewById(R.id.battery_saving_summary);
    private TextView tSensorsOnly = (TextView) findViewById(R.id.sensors_only_summary);

    public LocationDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_data_usage_text_size);
    }

    public void bind() {

        final TextView title = (TextView) findViewById(android.R.id.title);
        title.setText(R.string.quick_settings_location_mode_title);

        mLocationModes = (RadioGroup) findViewById(R.id.location_modes);
        mLocationModes.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                onRadioButtonClicked(checkedId);
            }
        });

        mHighAccuracy = (RadioButton) findViewById(R.id.high_accuracy);
        mBatterySaving = (RadioButton) findViewById(R.id.battery_saving);
        mSensorsOnly = (RadioButton) findViewById(R.id.sensors_only);
        tHighAccuracy = (TextView) findViewById(R.id.high_accuracy_summary);
        tBatterySaving = (TextView) findViewById(R.id.battery_saving_summary);
        tSensorsOnly = (TextView) findViewById(R.id.sensors_only_summary);
        tHighAccuracy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onVirtualClick(v);
            }
        });
        tBatterySaving.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onVirtualClick(v);
            }
        });
        tSensorsOnly.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onVirtualClick(v);
            }
        });
        refreshView();
    }

    public void onVirtualClick(View view) {
        switch(view.getId()) {
            case R.id.high_accuracy_summary:
                mHighAccuracy.performClick(); break;
            case R.id.battery_saving_summary:
                mBatterySaving.performClick(); break;
            case R.id.sensors_only_summary:
                mSensorsOnly.performClick(); break;
        }
    }

    public void onRadioButtonClicked(int checkedId) {
        int mode = Settings.Secure.LOCATION_MODE_OFF;
        switch(checkedId) {
            case R.id.high_accuracy:
                mode = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY; break;
            case R.id.battery_saving:
                mode = Settings.Secure.LOCATION_MODE_BATTERY_SAVING; break;
            case R.id.sensors_only:
                mode = Settings.Secure.LOCATION_MODE_SENSORS_ONLY; break;
        }
        setLocationMode(mode);
    }      

    public void setLocationMode(int mode) {
        if (isRestricted()) {
            // Location toggling disabled by user restriction. Read the current location mode to
            // update the location master switch.
            Log.i(TAG, "restricted user, not setting location mode");
            return;
        }
        Settings.Secure.putIntForUser(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE,
                  mode, ActivityManager.getCurrentUser());
        Log.i(TAG, "location mode has been changed to " + 
                  Settings.Secure.getIntForUser(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE,
                  Settings.Secure.LOCATION_MODE_OFF, ActivityManager.getCurrentUser()));
        refreshView();
    }

    public void enableRadioButtons(int mode) {
        boolean restricted = isRestricted();
        boolean enabled = (mode != Settings.Secure.LOCATION_MODE_OFF) && !restricted;
        mHighAccuracy.setEnabled(enabled);
        mBatterySaving.setEnabled(enabled);
        mSensorsOnly.setEnabled(enabled);
        tHighAccuracy.setClickable(enabled);
        tBatterySaving.setClickable(enabled);
        tSensorsOnly.setClickable(enabled);
        checkRadioButtons(mode);
    }

    public void checkRadioButtons(int mode) {
        mLocationModes.setOnCheckedChangeListener(null); // unregister listener so that code
                                                         // below does not trigger respective liseners
        switch(mode) {
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                mHighAccuracy.setChecked(true); break;
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                mBatterySaving.setChecked(true); break;
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                mSensorsOnly.setChecked(true); break;
            default:
                mLocationModes.clearCheck(); break;
        }
        mLocationModes.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                onRadioButtonClicked(checkedId);
            }
        });
    }    

    private boolean isRestricted() {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }

    public void refreshView() {
        int mode = Settings.Secure.getIntForUser(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE,
                  Settings.Secure.LOCATION_MODE_OFF, ActivityManager.getCurrentUser());
        enableRadioButtons(mode);
    }
}
