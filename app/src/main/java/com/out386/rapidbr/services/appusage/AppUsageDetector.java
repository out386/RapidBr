package com.out386.rapidbr.services.appusage;

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

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

interface AppUsageListener {
    void onForegroundAppChanged(@NonNull String packageName);
}

/**
 * Polls UsageStatsManager for app usage history. The last opened app is detected that way.
 */
class AppUsageDetector {

    private final long SCHEDULE_DELAY = 500;
    private final int STATS_START_TIME = 1000;
    private ScheduledExecutorService executor;
    private Context context;
    private AppUsageListener listener;
    private String lastPackage;
    private boolean isRunning;

    AppUsageDetector(Context context, @NonNull AppUsageListener listener) {
        this.context = context;
        this.listener = listener;
    }

    boolean isRunning() {
        return isRunning;
    }

    /**
     * Starts polling UsageStatsManager for app usage events. The registered listener will be called
     * if the app in the foreground changes.
     */
    void startPolling() {
        isRunning = true;
        executor = Executors.newSingleThreadScheduledExecutor();

        CheckUsageRunnable runnable = new CheckUsageRunnable(context);
        executor.scheduleWithFixedDelay(runnable, 0, SCHEDULE_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops polling UsageStatsManager.
     */
    void stop() {
        isRunning = false;
        executor.shutdownNow();
    }

    private class CheckUsageRunnable implements Runnable {

        private UsageStatsManager usageStatsManager;

        CheckUsageRunnable(Context context) {
            usageStatsManager = (UsageStatsManager) context
                    .getSystemService(Context.USAGE_STATS_SERVICE);
        }

        @Override
        public void run() {
            String packageName = getForegroundPackage(usageStatsManager);
            if (lastPackage == null) {
                if (packageName != null) {
                    lastPackage = packageName;
                    listener.onForegroundAppChanged(packageName);
                }
            } else if (packageName != null && !lastPackage.equals(packageName)) {
                lastPackage = packageName;
                listener.onForegroundAppChanged(packageName);
            }
        }

        /**
         * @param usageStatsManager Self explanatory
         * @return The package name of the last package that was used (activity opened /
         * notification interaction)
         */
        @Nullable
        private String getForegroundPackage(UsageStatsManager usageStatsManager) {
            long endTime = System.currentTimeMillis();
            long beginTime = endTime - STATS_START_TIME;
            String lastPackage = getLastUsedPackage(usageStatsManager, beginTime, endTime);

            if (lastPackage == null)
                return null;
            // getLastUsedPackage triggers on notifications, but wasPackageLastUsed
            // doesn't, but is not realtime on some devices
            if (wasPackageLastUsed(
                    usageStatsManager, lastPackage, beginTime, endTime))
                return lastPackage;
            else
                return null;
        }

        /**
         * Finds the last package that the user interacted with. This could be the foreground app,
         * or an app's notification. This method has realtime data, but needs extra processing (like
         * from {@link #wasPackageLastUsed}), because it'll return false positives in case there are
         * notifications. A very short {@link #STATS_START_TIME} will return null, unless a package
         * was interacted with in that time. On some devices, a very short {@link #STATS_START_TIME}
         * might return null all the time.
         *
         * @param usageStatsManager An UsageStatsManager
         * @param begin             The earliest time to track events from
         * @param end               The last time to track events from
         * @return The name of the package that was last interacted with
         */
        private String getLastUsedPackage(UsageStatsManager usageStatsManager,
                                          long begin, long end) {

            List<UsageStats> statsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY, begin, end);
            if (statsList == null || statsList.size() == 0)
                return null;

            sortUsageList(statsList);
            return statsList.get(0).getPackageName();
        }

        /**
         * Sorts the given List by descending order of item.getLastTimeUsed
         *
         * @param statsList The List to sort
         */
        private void sortUsageList(List<UsageStats> statsList) {
            Collections.sort(statsList, (left, right) ->
                    Long.compare(right.getLastTimeUsed(), left.getLastTimeUsed()));
        }

        /**
         * Checks if the given package is in the foreground. Does not care about notifications.
         * However, this check is realtime on some devices, but very slow on others. {@link
         * #getLastUsedPackage} is realtime, but also triggers on notifications from packageName.
         * Using both methods mean ignoring notifications from packageName, and immediately saying
         * that packageName is not in foreground as soon as it isn't. However, if packageName just
         * came to foreground, {@link #getLastUsedPackage} will know it, but this method might not,
         * so it will add a delay before returning positive. On some devices, this delay might be
         * too long.
         * <p>
         * A very short {@link #STATS_START_TIME} will return null, unless a package was interacted
         * with in that time. On some devices, a very short {@link #STATS_START_TIME} might return
         * null all the time.
         *
         * @param usageStatsManager An UsageStatsManager
         * @param packageName       The name of the package that is suspected to be in the
         *                          foreground
         * @param begin             The earliest time to track events from
         * @param end               The last time to track events from
         * @return True if the given package is in foreground, false if it <b>might</b> not be
         */
        private boolean wasPackageLastUsed(UsageStatsManager usageStatsManager,
                                           @NonNull String packageName, long begin, long end) {

            UsageEvents usageEvent = usageStatsManager.queryEvents(begin, end);
            UsageEvents.Event event = new UsageEvents.Event();

            // get last event
            //noinspection StatementWithEmptyBody
            while (usageEvent.getNextEvent(event)) ;

            return
                    packageName.equals(event.getPackageName()) && event.getEventType()
                            == UsageEvents.Event.MOVE_TO_FOREGROUND;
        }
    }
}