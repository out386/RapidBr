package com.out386.rapidbr.settings.bottom.blacklist.picker;

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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.getAppName;
import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.getIcon;

public class AllAppsStore {
    private Context context;
    private static AllAppsStore allAppsStore;
    private PackageManager packageManager;
    private List<BlacklistPickerItem> appsList;
    private ExecutorService appsStoreExecutor;

    private AllAppsStore(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
        appsStoreExecutor = Executors.newSingleThreadExecutor();
    }

    public static AllAppsStore getInstance(Context context) {
        if (allAppsStore == null)
            allAppsStore = new AllAppsStore(context);
        return allAppsStore;
    }

    public void fetchApps(@Nullable LoadProfilesPickerAppsListener listener, boolean allowCache) {
        if (allowCache && appsList != null && appsList.size() > 0) {
            if (listener != null)
                listener.onAppsLoaded(appsList);
        } else {
            appsStoreExecutor.submit(new GetAppsRunnable(listener));
        }
    }

    private class GetAppsRunnable implements Runnable {
        LoadProfilesPickerAppsListener listener;

        GetAppsRunnable(LoadProfilesPickerAppsListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            List<ResolveInfo> resolveAppsList;
            Intent appsIntent = new Intent(Intent.ACTION_MAIN, null);
            appsIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveAppsList = packageManager.queryIntentActivities(appsIntent, 0);
            if (resolveAppsList == null || resolveAppsList.size() == 0) {
                if (listener != null)
                    listener.onAppsLoaded(null);
                return;
            }

            // Using the package name as the key. This will handle apps with multiple launcher activities
            TreeMap<String, BlacklistPickerItem> apps = new TreeMap<>();
            for (ResolveInfo resolveInfo : resolveAppsList) {
                String name = getAppName(packageManager,
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.applicationInfo
                );

                boolean isAppBlacklistable =
                        !context.getPackageName().equals(resolveInfo.activityInfo.packageName);

                if (isAppBlacklistable) {
                    apps.put(resolveInfo.activityInfo.packageName,
                            new BlacklistPickerItem.Builder()
                                    .withName(name)
                                    .withPackage(resolveInfo.activityInfo.packageName)
                                    .withIcon(getIcon(
                                            context, packageManager,
                                            resolveInfo.activityInfo.packageName
                                    ))
                                    .build()
                    );
                }
            }
            // Sorting by app name
            List<BlacklistPickerItem> list = new ArrayList<>(apps.values());
            Collections.sort(list);
            appsList = list;
            if (listener != null)
                listener.onAppsLoaded(list);
        }
    }

    public interface LoadProfilesPickerAppsListener {
        void onAppsLoaded(List<BlacklistPickerItem> apps);
    }
}