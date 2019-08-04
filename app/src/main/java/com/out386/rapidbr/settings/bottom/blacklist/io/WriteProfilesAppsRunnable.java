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

import com.out386.rapidbr.settings.bottom.blacklist.AppProfilesAppsItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import static com.out386.rapidbr.settings.bottom.blacklist.BlacklistFragment.FILE_APP_PROFILES_APPS_LIST;

public class WriteProfilesAppsRunnable implements Runnable {
    private Context context;
    private List<AppProfilesAppsItem> items;

    /**
     * Writes the given List to disk
     *
     * @param context The Application Context
     * @param items   The List to write to disk
     */
    public WriteProfilesAppsRunnable(Context context, List<AppProfilesAppsItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public void run() {
        ObjectOutputStream objectOutputStream;
        File file;
        try {
            file = new File(context.getFilesDir().getAbsolutePath(), FILE_APP_PROFILES_APPS_LIST);
            FileOutputStream outputStream = new FileOutputStream(file);
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(items);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}