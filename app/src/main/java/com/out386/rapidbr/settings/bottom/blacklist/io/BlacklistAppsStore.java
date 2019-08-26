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

import androidx.annotation.Nullable;

import com.out386.rapidbr.settings.bottom.blacklist.BlacklistAppsItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlacklistAppsStore {
    static final String FILE_APP_PROFILES_APPS_LIST = "blacklistAppsList";
    private static BlacklistAppsStore blacklistAppsStore;

    private Context context;
    private ExecutorService appsStoreExecutor;

    public static BlacklistAppsStore getInstance(Context context) {
        if (blacklistAppsStore == null)
            blacklistAppsStore = new BlacklistAppsStore(context);
        return blacklistAppsStore;
    }

    private BlacklistAppsStore(Context context) {
        this.context = context.getApplicationContext();
        appsStoreExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Manages loading a list app items on a background thread. Can read from saved data on disk.
     * Sets icons to the items, and calls {@code listener)}
     *
     * @param list     If null, attempts to read the list from disk. Else sets icons to this list.
     * @param listener Called with a list of {@link BlacklistAppsItem}
     */
    public void read(List<BlacklistAppsItem> list, OnBlacklistReadListener listener) {
        ReadBlacklistRunnable readBlacklistRunnable =
                new ReadBlacklistRunnable(context, list, listener);
        appsStoreExecutor.submit(readBlacklistRunnable);
    }

    public void write(List<BlacklistAppsItem> list) {
        WriteBlacklistRunnable writeProfilesAppsRunnable =
                new WriteBlacklistRunnable(context, list);
        appsStoreExecutor.submit(writeProfilesAppsRunnable);
    }

    public interface OnBlacklistReadListener {
        void onBlacklistRead(@Nullable List<BlacklistAppsItem> apps);
    }
}
