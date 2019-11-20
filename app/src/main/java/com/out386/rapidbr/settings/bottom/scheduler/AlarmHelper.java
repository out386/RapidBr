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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.out386.rapidbr.BuildConfig;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.out386.rapidbr.settings.bottom.scheduler.SchedulerFragment.KEY_SCHED_ENABLE;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_START_HOUR;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_START_MINUTE;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_STOP_HOUR;
import static com.out386.rapidbr.settings.bottom.scheduler.TimePickerFragment.KEY_SCHEDULER_STOP_MINUTE;


public class AlarmHelper {
    private Context context;
    private AlarmManager alarmMgr;
    private static final int REQUEST_CODE_START = 5;
    private static final int REQUEST_CODE_STOP = 6;
    static final String ACTION_SCHEDULER_START = "actionSchedulerStart";
    static final String ACTION_SCHEDULER_STOP = "actionSchedulerStop";

    public AlarmHelper(Context context) {
        this.context = context;
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Sets or unsets scheduler alarms
     *
     * @param isStart Specifies if this is a start scheduler or a stop scheduler
     * @param millis  The time in milliseconds to trigger the alarm. -1 to cancel the alarm
     */
    private void setAlarmInternal(boolean isStart, long millis) {
        Intent intent = new Intent(context, BootAlarmReceiver.class);
        PendingIntent alarmIntent;

        if (isStart) {
            intent.setAction(ACTION_SCHEDULER_START);
            alarmIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_START,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            intent.setAction(ACTION_SCHEDULER_STOP);
            alarmIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_STOP,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        if (millis > -1) {
            alarmMgr.setRepeating(AlarmManager.RTC, millis,
                    AlarmManager.INTERVAL_DAY, alarmIntent);
            if (BuildConfig.DEBUG)
                Log.i("Alarms", (isStart ? "Start " : "Stop ")
                        + "alarm set for " + getTimeAsString(millis));
        } else {
            alarmMgr.cancel(alarmIntent);
            if (BuildConfig.DEBUG)
                Log.i("Alarms", (isStart ? "Start " : "Stop ")
                        + "alarm cancelled ");
        }
    }

    private void setAlarm(boolean isStart, long timeInMillis) {
        if (timeInMillis < System.currentTimeMillis()) {
            // Time has passed, so schedule the alarm for tomorrow
            setAlarm(isStart, 86400000L + timeInMillis);
            return;
        }

        setAlarmInternal(isStart, timeInMillis);
    }

    /**
     * Set system alarms when the scheduler is enabled
     *
     * @param isStart Specifies if this is a start scheduler or a stop scheduler
     * @param hour    Alarm hour in 24 hour format
     * @param minute  Alarm minute
     */
    private void setAlarm(boolean isStart, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        setAlarm(isStart, calendar.getTimeInMillis());
    }

    private void unsetAlarms() {
        setAlarmInternal(true, -1);
        setAlarmInternal(false, -1);
    }

    /**
     * Set or unset Alarms for the scheduler, depending on whether it is enabled or not.
     *
     * @param prefs A SharedPreferences from which Scheduler settings will be read
     */
    public void setUnsetAlarms(SharedPreferences prefs) {
        boolean isEnabled = prefs.getBoolean(KEY_SCHED_ENABLE, false);
        if (isEnabled) {
            int startHour = prefs.getInt(KEY_SCHEDULER_START_HOUR, 0);
            int startMin = prefs.getInt(KEY_SCHEDULER_START_MINUTE, 0);
            int stopHour = prefs.getInt(KEY_SCHEDULER_STOP_HOUR, 0);
            int stopMin = prefs.getInt(KEY_SCHEDULER_STOP_MINUTE, 0);
            setAlarm(true, startHour, startMin);
            setAlarm(false, stopHour, stopMin);
        } else {
            unsetAlarms();
        }
    }

    private String getTimeAsString(long millis) {
        DateFormat df = DateFormat.getDateTimeInstance();
        return df.format(new Date(millis));
    }

}
