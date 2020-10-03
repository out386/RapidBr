package com.out386.rapidbr.settings.bottom.scheduler;

/*
 * Copyright (C) 2019 Ritayan Chakraborty <ritayanout@gmail.com>
 *
 * This file is part of RapidBr
 *
 * RapidBr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RapidBr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RapidBr.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.out386.rapidbr.R;
import com.out386.rapidbr.ads.AdManager;
import com.out386.rapidbr.ads.OnAdLoadedListener;
import com.out386.rapidbr.settings.bottom.views.ButtonHideNestedScrollView;
import com.out386.rapidbr.settings.bottom.views.SwitchItem;

import java.util.Calendar;

import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_START_HOUR;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_START_MINUTE;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_STOP_HOUR;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_STOP_MINUTE;

public class SchedulerFragment extends Fragment implements OnAdLoadedListener {
    static final String KEY_SCHED_ENABLE = "KEY_SCHED_ENABLE";
    private static final int AD_REQUESTER_ID = 1;

    private LinearLayout schedStart;
    private LinearLayout schedStop;
    private TextView schedStartDesc;
    private TextView schedStopDesc;
    private SwitchItem schedEnable;
    private SharedPreferences prefs;
    private PrefsListener prefsListener;
    private AlarmHelper alarmHelper;
    private ButtonHideNestedScrollView scrollView;
    private View rootView;
    private boolean isFragmentStopped = true;
    //private AdManager adManager;
    private UnifiedNativeAdView currentAd;

    public SchedulerFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_scheduler, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        schedStartDesc = rootView.findViewById(R.id.sched_start_desc);
        schedStopDesc = rootView.findViewById(R.id.sched_stop_desc);
        schedStart = rootView.findViewById(R.id.sched_start);
        schedStop = rootView.findViewById(R.id.sched_stop);
        schedEnable = rootView.findViewById(R.id.sched_enable_switch);
        alarmHelper = new AlarmHelper(requireContext());

        scrollView = rootView.findViewById(R.id.scroll_view);
        scrollView.setupButtonHideListener(requireActivity());
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean isEnabled = prefs.getBoolean(KEY_SCHED_ENABLE, false);
        int startHour = prefs.getInt(KEY_SCHEDULER_START_HOUR, 0);
        int startMin = prefs.getInt(KEY_SCHEDULER_START_MINUTE, 0);
        int stopHour = prefs.getInt(KEY_SCHEDULER_STOP_HOUR, 0);
        int stopMin = prefs.getInt(KEY_SCHEDULER_STOP_MINUTE, 0);

        schedEnable.setChecked(isEnabled);
        setTimeText(true, startHour, startMin);
        setTimeText(false, stopHour, stopMin);
        setupListeners();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //adManager = AdManager.getInstance(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        isFragmentStopped = false;
        //adManager.getAd(this, AD_REQUESTER_ID);
    }

    @Override
    public void onStop() {
        super.onStop();
        isFragmentStopped = true;
        if (currentAd != null)
            currentAd.destroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        scrollView.forceButtonShow();
        setPrefsListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //adManager = null;
    }

    private void setPrefsListener() {
        if (prefsListener != null)
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);

        prefsListener = new PrefsListener();
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);
    }

    private void setTimeText(boolean startChanged, int hour, int min) {
        String messageTemplate;
        String message;
        java.text.DateFormat format = DateFormat.getTimeFormat(getActivity());
        TextView target;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);

        if (startChanged) {
            messageTemplate = getResources().getString(R.string.sett_sched_start_desc);
            target = schedStartDesc;
        } else {
            messageTemplate = getResources().getString(R.string.sett_sched_stop_desc);
            target = schedStopDesc;
        }

        message = String.format(messageTemplate, format.format(cal.getTime()));
        target.setText(message);
    }

    private void setupListeners() {
        schedEnable.setOnCheckedChangeListener(isChecked -> {
            prefs.edit()
                    .putBoolean(KEY_SCHED_ENABLE, isChecked)
                    .apply();
            alarmHelper.setUnsetAlarms(prefs);
        });

        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null)
            return; // Eh.

        schedStart.setOnClickListener(view ->
                new TimePickerFragment(true, getActivity())
                        .show(fragmentManager, null));

        schedStop.setOnClickListener(view ->
                new TimePickerFragment(false, getActivity())
                        .show(fragmentManager, null));
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
    }

    @Override
    public void onAdLoaded(@Nullable UnifiedNativeAd ad) {
        if (isFragmentStopped)
            return;

        LinearLayout adViewRoot = rootView.findViewById(R.id.ad_view);
        currentAd = AdManager.inflateAd(adViewRoot, ad);
    }

    private class PrefsListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

            if (KEY_SCHEDULER_START_HOUR.equals(key) || KEY_SCHEDULER_START_MINUTE.equals(key)) {
                int hour = prefs.getInt(KEY_SCHEDULER_START_HOUR, 0);
                int min = prefs.getInt(KEY_SCHEDULER_START_MINUTE, 0);
                setTimeText(true, hour, min);
                alarmHelper.setUnsetAlarms(prefs);

            } else if (KEY_SCHEDULER_STOP_HOUR.equals(key) || KEY_SCHEDULER_STOP_MINUTE.equals(key)) {
                int hour = prefs.getInt(KEY_SCHEDULER_STOP_HOUR, 0);
                int min = prefs.getInt(KEY_SCHEDULER_STOP_MINUTE, 0);
                setTimeText(false, hour, min);
                alarmHelper.setUnsetAlarms(prefs);
            }
        }
    }

}
