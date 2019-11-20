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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.out386.rapidbr.services.overlay.ServiceLauncher;

public class BootAlarmReceiver extends BroadcastReceiver {
    public static final String KEY_START_ON_BOOT = "startOnBoot";

    @Override
    public void onReceive(Context context, Intent i) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            if (prefs.getBoolean(KEY_START_ON_BOOT, false))
                ServiceLauncher.startBrightnessService(context, prefs);

            AlarmHelper alarmHelper = new AlarmHelper(context);
            alarmHelper.setUnsetAlarms(prefs);
        } else if (AlarmHelper.ACTION_SCHEDULER_START.equals(i.getAction())) {
            ServiceLauncher.startBrightnessService(context, prefs);
        } else if (AlarmHelper.ACTION_SCHEDULER_STOP.equals(i.getAction())) {
            ServiceLauncher.stopBrightnessService(context);
        }
    }
}
