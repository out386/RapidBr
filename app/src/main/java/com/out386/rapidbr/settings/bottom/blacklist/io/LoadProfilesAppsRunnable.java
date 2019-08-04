package com.out386.rapidbr.settings.bottom.blacklist.io;

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
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import com.out386.rapidbr.settings.bottom.blacklist.AppProfilesAppsItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import static com.out386.rapidbr.settings.bottom.blacklist.BlacklistFragment.FILE_APP_PROFILES_APPS_LIST;
import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.setIcon;

public class LoadProfilesAppsRunnable implements Runnable {
    private PackageManager packageManager;
    private LoadProfilesAppsListener listener;
    private Context context;
    private List<AppProfilesAppsItem> includeItems;

    /**
     * Constructor for LoadProfilesAppsRunnable.
     * If includeItems is null, read previously saved list from disk, set icons,
     * and call the listener.
     * Else set icons to the items in #includeItems, and call the listener.
     *
     * @param context      The Application Context
     * @param includeItems Can be null
     * @param listener     The listener to call after reading the list.
     */
    public LoadProfilesAppsRunnable(Context context, List<AppProfilesAppsItem> includeItems, LoadProfilesAppsListener listener) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        this.listener = listener;
        this.includeItems = includeItems;
    }

    @Override
    public void run() {
        if (includeItems != null) {
            // No need to read from disk, because an updated version of whatever is on disk is in includeItems
            setIcons(includeItems);
            listener.onAppsLoaded(includeItems);
            return;
        }

        ObjectInputStream objectInputStream;
        List<AppProfilesAppsItem> apps;
        File file = null;
        try {
            file = new File(context.getFilesDir().getAbsolutePath(), FILE_APP_PROFILES_APPS_LIST);
            FileInputStream inputStream = new FileInputStream(file);
            objectInputStream = new ObjectInputStream(inputStream);
            //noinspection unchecked
            apps = (List<AppProfilesAppsItem>) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            listener.onAppsLoaded(null);    // Nothing on disk
            return;
        }
        if (apps != null)
            setIcons(apps);

        listener.onAppsLoaded(apps);
    }

    private void setIcons(List<AppProfilesAppsItem> apps) {
        for (AppProfilesAppsItem appsItem : apps) {
            setIcon(context, packageManager, appsItem);
        }
    }

    public interface LoadProfilesAppsListener {
        void onAppsLoaded(@Nullable List<AppProfilesAppsItem> apps);
    }

}
