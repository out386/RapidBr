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


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.out386.rapidbr.R;

import java.util.Calendar;

import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_START_HOUR;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_START_MINUTE;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_STOP_HOUR;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_STOP_MINUTE;

public class SchedulerFragment extends Fragment {
    private static final String KEY_SCHED_ENABLE = "KEY_SCHED_ENABLE";

    private LinearLayout schedStart;
    private LinearLayout schedStop;
    private TextView schedStartDesc;
    private TextView schedStopDesc;
    private Switch schedEnable;
    private SharedPreferences prefs;
    private PrefsListener prefsListener;

    public SchedulerFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_scheduler, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        schedStartDesc = v.findViewById(R.id.sched_start_desc);
        schedStopDesc = v.findViewById(R.id.sched_stop_desc);
        schedStart = v.findViewById(R.id.sched_start);
        schedStop = v.findViewById(R.id.sched_stop);
        schedEnable = v.findViewById(R.id.sched_enable_switch);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupViews();
    }

    private void setupViews() {
        boolean isEnabled = prefs.getBoolean(KEY_SCHED_ENABLE, false);
        int startHour = prefs.getInt(KEY_SCHEDULER_START_HOUR, 0);
        int startMin = prefs.getInt(KEY_SCHEDULER_START_MINUTE, 0);
        int stopHour = prefs.getInt(KEY_SCHEDULER_STOP_HOUR, 0);
        int stopMin = prefs.getInt(KEY_SCHEDULER_STOP_MINUTE, 0);

        schedEnable.setChecked(isEnabled);

        setTimeText(startHour, startMin, stopHour, stopMin);
        setTimeTextChanged();
        setupListeners();
    }

    private void setTimeTextChanged() {
        if (prefsListener != null)
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);

        prefsListener = new PrefsListener();
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);
    }

    private void setTimeText(int startHour, int startMin, int stopHour, int stopMin) {
        String startText = getResources().getString(R.string.sett_sched_start_desc);
        String stopText = getResources().getString(R.string.sett_sched_stop_desc);
        String startStr;
        String stopStr;
        java.text.DateFormat format = DateFormat.getTimeFormat(getActivity());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, startHour);
        cal.set(Calendar.MINUTE, startMin);

        startStr = String.format(startText, format.format(cal.getTime()));

        cal.set(Calendar.HOUR_OF_DAY, stopHour);
        cal.set(Calendar.MINUTE, stopMin);

        stopStr = String.format(stopText, format.format(cal.getTime()));
        schedStartDesc.setText(startStr);
        schedStopDesc.setText(stopStr);
    }

    private void setupListeners() {
        schedEnable.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit()
                        .putBoolean(KEY_SCHED_ENABLE, isChecked)
                        .apply());

        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null)
            return; // Eh.

        schedStart.setOnClickListener(view ->
                new TimePickerFragment(true, R.string.sett_sched_start_title, getActivity())
                        .show(fragmentManager, null));

        schedStop.setOnClickListener(view ->
                new TimePickerFragment(false, R.string.sett_sched_stop_title, getActivity())
                        .show(fragmentManager, null));
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener);
    }

    private class PrefsListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            if (KEY_SCHEDULER_START_HOUR.equals(key) ||
                    KEY_SCHEDULER_START_MINUTE.equals(key) ||
                    KEY_SCHEDULER_STOP_HOUR.equals(key) ||
                    KEY_SCHEDULER_STOP_MINUTE.equals(key)) {

                int startHour = prefs.getInt(KEY_SCHEDULER_START_HOUR, 0);
                int startMin = prefs.getInt(KEY_SCHEDULER_START_MINUTE, 0);
                int stopHour = prefs.getInt(KEY_SCHEDULER_STOP_HOUR, 0);
                int stopMin = prefs.getInt(KEY_SCHEDULER_STOP_MINUTE, 0);
                setTimeText(startHour, startMin, stopHour, stopMin);
            }
        }
    }

}
