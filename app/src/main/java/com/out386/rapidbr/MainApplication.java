package com.out386.rapidbr;

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

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class MainApplication extends Application {
    static final String KEY_THEME_TYPE = "themeSetting";

    @Override
    public void onCreate() {
        super.onCreate();

        int theme;
        if (Build.VERSION.SDK_INT > 28)
            theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        else
            theme = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        theme = prefs.getInt(KEY_THEME_TYPE, theme);

        AppCompatDelegate.setDefaultNightMode(theme);
    }

}
