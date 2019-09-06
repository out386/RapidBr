package com.out386.rapidbr.services.blacklist;

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

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.out386.rapidbr.services.overlay.BrightnessOverlayService;
import com.out386.rapidbr.services.overlay.ServiceLauncher;
import com.out386.rapidbr.settings.bottom.blacklist.BlacklistAppsItem;

import java.io.Serializable;
import java.util.List;

import static com.out386.rapidbr.utils.DeviceBrightnessUtils.getBrightness;
import static com.out386.rapidbr.utils.DeviceBrightnessUtils.setBrightness;


public class AppBlacklistService extends Service {
    private static final String KEY_USER_BRIGHTNESS = "userBrightness";
    public static final String KEY_BLACKLIST_LIST = "blacklistList";

    private AppUsageDetector appUsageDetector;
    private List<BlacklistAppsItem> blacklistList;
    @Nullable
    private BlacklistAppsItem lastItem;
    private Intent resumeIntent;
    private Intent pauseIntent;
    private SharedPreferences prefs;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        List<BlacklistAppsItem> list = null;
        Serializable serializable = intent.getSerializableExtra(KEY_BLACKLIST_LIST);
        if (serializable instanceof List) {
            //noinspection unchecked
            list = (List<BlacklistAppsItem>) serializable;
        }

        synchronized (this) {
            blacklistList = list;
            if (list != null && list.size() > 0) {
                setupIntents();
                if (appUsageDetector == null || !appUsageDetector.isRunning()) {
                    appUsageDetector = new AppUsageDetector(getApplicationContext(), this::onAppChanged);
                    appUsageDetector.startPolling();
                }
            } else {
                if (appUsageDetector != null && appUsageDetector.isRunning()) {
                    appUsageDetector.stop();
                }
                stopSelf();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        prefs = getSharedPreferences("brightnessPrefs", MODE_PRIVATE);
        super.onCreate();
    }

    private synchronized void onAppChanged(@NonNull String packageName) {
        for (BlacklistAppsItem item : blacklistList) {
            if (packageName.equals(item.getPackageName())) {
                if (lastItem != null) {
                    /* There was a Blacklist app before this app started. So, this is a
                     * Blacklist Entry event, and there will be no Exit event for lastItem. Set the
                     * last app's brightness to whatever the user had set while in the last app.
                     */
                    if (lastItem.getBrightness() > -1)
                        lastItem.setAppBrightness(getBrightness(getApplicationContext()));

                    // RapidBr will stay paused, but brightness will be changed
                    setBrightness(getApplicationContext(), (int) item.getBrightness());
                } else {
                    // Save the current screen brightness
                    prefs.edit()
                            .putInt(KEY_USER_BRIGHTNESS, getBrightness(getApplicationContext()))
                            .apply();
                    pauseRapidbr(item.getBrightness());
                }
                lastItem = item;
                return;
            }
        }

        /* packageName is not on the blacklist. This is a Blacklist Exit event.
         * Inverse behaviours, because we're undoing the applied blacklist.
         * Set the last app's brightness to whatever the user had set while in the last app
         */
        if (lastItem != null) {
            if (lastItem.getBrightness() > -1)
                lastItem.setAppBrightness(getBrightness(getApplicationContext()));
            startRapidbr();
            // Restore the screen brightness before the blacklist got triggered
            setBrightness(getApplicationContext(), prefs.getInt(KEY_USER_BRIGHTNESS, -1));

            lastItem = null;
        }
    }

    @Override
    public void onDestroy() {
        if (appUsageDetector != null && appUsageDetector.isRunning())
            appUsageDetector.stop();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupIntents() {
        if (resumeIntent == null)
            resumeIntent = ServiceLauncher.getBrightnessServiceStartIntent(this);
        if (pauseIntent == null)
            pauseIntent = new Intent(this, BrightnessOverlayService.class)
                    .setAction(BrightnessOverlayService.ACTION_PAUSE);
    }

    private synchronized void pauseRapidbr(float brightness) {
        setBrightness(getApplicationContext(), (int) brightness);
        startService(pauseIntent);
    }

    private synchronized void startRapidbr() {
        startService(resumeIntent);
    }

}

