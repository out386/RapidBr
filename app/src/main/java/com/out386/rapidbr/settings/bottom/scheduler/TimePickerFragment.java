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

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class TimePickerFragment extends DialogFragment implements OnTimeSetListener {
    static final String KEY_SCHEDULER_START_HOUR = "KEY_SCHEDULER_START_HOUR";
    static final String KEY_SCHEDULER_START_MINUTE = "KEY_SCHEDULER_START_MINUTE";
    static final String KEY_SCHEDULER_STOP_HOUR = "KEY_SCHEDULER_STOP_HOUR";
    static final String KEY_SCHEDULER_STOP_MINUTE = "KEY_SCHEDULER_STOP_MINUTE";

    private TimePickerDialog timePicker;
    private SharedPreferences prefs;
    private boolean isStart;

    TimePickerFragment(boolean isStart, int title, Context context) {
        this.isStart = isStart;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int hour;
        int minute;

        if (isStart) {
            hour = prefs.getInt(KEY_SCHEDULER_START_HOUR, 0);
            minute = prefs.getInt(KEY_SCHEDULER_START_MINUTE, 0);
        } else {
            hour = prefs.getInt(KEY_SCHEDULER_STOP_HOUR, 0);
            minute = prefs.getInt(KEY_SCHEDULER_STOP_MINUTE, 0);
        }

        timePicker = new TimePickerDialog(context, TimePickerFragment.this,
                hour, minute, DateFormat.is24HourFormat(context));
        timePicker.setTitle(context.getResources().getString(title));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return timePicker;
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        SharedPreferences.Editor editor = prefs.edit();
        if (isStart) {
            editor.putInt(KEY_SCHEDULER_START_HOUR, hourOfDay);
            editor.putInt(KEY_SCHEDULER_START_MINUTE, minute);
        } else {
            editor.putInt(KEY_SCHEDULER_STOP_HOUR, hourOfDay);
            editor.putInt(KEY_SCHEDULER_STOP_MINUTE, minute);
        }
        editor.apply();
    }

}
