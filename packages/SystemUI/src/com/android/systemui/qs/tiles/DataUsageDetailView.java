/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.internal.util.xdroid.DeviceUtils;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.DataUsageGraph;
import com.android.systemui.statusbar.policy.NetworkController;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Layout for the data usage detail in quick settings.
 */
public class DataUsageDetailView extends LinearLayout {

    private static final double KB = 1024;
    private static final double MB = 1024 * KB;
    private static final double GB = 1024 * MB;
    private static final int NETWORK_2G = 0;
    private static final int NETWORK_3G = 1;
    private static final int NETWORK_2G3G = 2;
    private static final int NETWORK_LTE = 3;

    private final DecimalFormat FORMAT = new DecimalFormat("#.##");

    public DataUsageDetailView(Context mContext, AttributeSet attrs) {
        super(mContext, attrs);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_text, R.dimen.qs_data_usage_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_carrier_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_info_top_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_period_text, R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_info_bottom_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.mobile_network_text,
                R.dimen.qs_data_usage_text_size);
    }

    public void bind(NetworkController.MobileDataController.DataUsageInfo info) {
        final Resources res = mContext.getResources();
        final int titleId;
        final long bytes;
        int usageColor = R.color.system_accent_color;
        final String top;
        String bottom = null;
        if (info.usageLevel < info.warningLevel || info.limitLevel <= 0) {
            // under warning, or no limit
            titleId = R.string.quick_settings_cellular_detail_data_usage;
            bytes = info.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_warning,
                    formatBytes(info.warningLevel));
        } else if (info.usageLevel <= info.limitLevel) {
            // over warning, under limit
            titleId = R.string.quick_settings_cellular_detail_remaining_data;
            bytes = info.limitLevel - info.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used,
                    formatBytes(info.usageLevel));
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit,
                    formatBytes(info.limitLevel));
        } else {
            // over limit
            titleId = R.string.quick_settings_cellular_detail_over_limit;
            bytes = info.usageLevel - info.limitLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used,
                    formatBytes(info.usageLevel));
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit,
                    formatBytes(info.limitLevel));
            usageColor = R.color.system_warning_color;
        }

        final TextView title = (TextView) findViewById(android.R.id.title);
        title.setText(titleId);
        final TextView usage = (TextView) findViewById(R.id.usage_text);
        usage.setText(formatBytes(bytes));
        usage.setTextColor(res.getColor(usageColor));
        final DataUsageGraph graph = (DataUsageGraph) findViewById(R.id.usage_graph);
        graph.setLevels(info.limitLevel, info.warningLevel, info.usageLevel);
        final TextView carrier = (TextView) findViewById(R.id.usage_carrier_text);
        carrier.setText(info.carrier);
        final TextView period = (TextView) findViewById(R.id.usage_period_text);
        period.setText(info.period);
        final TextView infoTop = (TextView) findViewById(R.id.usage_info_top_text);
        infoTop.setVisibility(top != null ? View.VISIBLE : View.GONE);
        infoTop.setText(top);
        final TextView infoBottom = (TextView) findViewById(R.id.usage_info_bottom_text);
        infoBottom.setVisibility(bottom != null ? View.VISIBLE : View.GONE);
        infoBottom.setText(bottom);
        final TextView networkMode = (TextView) findViewById(R.id.mobile_network_text);
        networkMode.setText(R.string.qs_network_mode);

        Spinner mNetTypeList = (Spinner) findViewById(R.id.mobile_network_type);
        ArrayList<String> mNetTypeArray = 
              new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.mobile_network_type)));
        if(!deviceSupportsLTE())
            mNetTypeArray.remove("LTE");
        ArrayAdapter<String> mNetTypeAdapter = new ArrayAdapter<String>(mContext,
                                     android.R.layout.simple_list_item_1, mNetTypeArray);
        mNetTypeList.setAdapter(mNetTypeAdapter);
        mNetTypeList.setSelection(getSelectedNetwork());
        mNetTypeList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                                                         int position, long id) {
                setSelectedNetwork(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {                
            }
        });
        
    }

    private String formatBytes(long bytes) {
        final long b = Math.abs(bytes);
        double val;
        String suffix;
        if (b > 100 * MB) {
            val = b / GB;
            suffix = "GB";
        } else if (b > 100 * KB) {
            val = b / MB;
            suffix = "MB";
        } else {
            val = b / KB;
            suffix = "KB";
        }
        return FORMAT.format(val * (bytes < 0 ? -1 : 1)) + " " + suffix;
    }

    boolean deviceSupportsLTE() {
        return DeviceUtils.deviceSupportsLte(mContext);
    }

    public int getCurrentPreferredNetworkMode(Context mContext) {
        int network = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE, -1);
        return network;
    }

    public void setSelectedNetwork(int pos) {
        TelephonyManager tm = (TelephonyManager)
            mContext.getSystemService(Context.TELEPHONY_SERVICE);
        boolean usesQcLte = SystemProperties.getBoolean(
                        "ro.config.qc_lte_network_modes", false);
        int curNetwork = getCurrentPreferredNetworkMode(mContext);
        switch(curNetwork) {
            //GSM Modes            
            case Phone.NT_MODE_LTE_WCDMA:
            case Phone.NT_MODE_LTE_GSM_WCDMA:
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_GSM_ONLY:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_GSM_UMTS:
                switch(pos) {
                    case NETWORK_2G : tm.toggleMobileNetwork(Phone.NT_MODE_GSM_ONLY); break;
                    case NETWORK_3G : tm.toggleMobileNetwork(Phone.NT_MODE_WCDMA_ONLY); break;
                    case NETWORK_2G3G : tm.toggleMobileNetwork(Phone.NT_MODE_WCDMA_PREF); break;
                    case NETWORK_LTE : if (deviceSupportsLTE()) {
                                 if (usesQcLte)
                                     tm.toggleMobileNetwork(Phone.NT_MODE_LTE_CDMA_AND_EVDO);
                                 else
                                     tm.toggleMobileNetwork(Phone.NT_MODE_LTE_GSM_WCDMA);
                             }
                             break;
                }
                break;
            //CDMA Modes
            case Phone.NT_MODE_GLOBAL:
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_CDMA_NO_EVDO:
            case Phone.NT_MODE_CDMA:
                switch(pos) {
                    case NETWORK_2G : tm.toggleMobileNetwork(Phone.NT_MODE_CDMA_NO_EVDO); break;
                    case NETWORK_3G : tm.toggleMobileNetwork(Phone.NT_MODE_EVDO_NO_CDMA); break;
                    case NETWORK_2G3G : tm.toggleMobileNetwork(Phone.NT_MODE_CDMA); break;
                    case NETWORK_LTE : tm.toggleMobileNetwork(Phone.NT_MODE_LTE_CDMA_AND_EVDO); break;
                }
                break;
        }
    }

    public int getSelectedNetwork() {
        int curNetwork = getCurrentPreferredNetworkMode(mContext);
        switch(curNetwork) {
            case Phone.NT_MODE_GSM_ONLY:
            case Phone.NT_MODE_CDMA_NO_EVDO:
                return NETWORK_2G;
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_EVDO_NO_CDMA:
                return NETWORK_3G;
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_CDMA:
                return NETWORK_2G3G;
            case Phone.NT_MODE_LTE_WCDMA:
            case Phone.NT_MODE_LTE_GSM_WCDMA:
            case Phone.NT_MODE_LTE_ONLY:
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                return NETWORK_LTE;

            default: setSelectedNetwork(NETWORK_2G3G);
                return NETWORK_2G3G;
        }
    }
}
