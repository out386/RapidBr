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

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.afollestad.materialdialogs.MaterialDialog;

public class ThemeActivity extends AppCompatActivity {

    private static final String KEY_THEME_TYPE = "themeSetting";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int theme;
        if (Build.VERSION.SDK_INT > 28)
            theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        else
            theme = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        theme = prefs.getInt(KEY_THEME_TYPE, theme);

        AppCompatDelegate.setDefaultNightMode(theme);
        Log.i("blah", "onCreate: " + theme);
        setSystemUIFlags();
    }

    private void setSystemUIFlags() {
        int uiMode = getCurrentTheme();
        View decorView = getWindow().getDecorView();
        switch (uiMode) {
            case Configuration.UI_MODE_NIGHT_YES:
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

                break;
            case Configuration.UI_MODE_NIGHT_NO:
            default:
                if (Build.VERSION.SDK_INT >= 26) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                } else {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }


        }
    }

    private int getCurrentTheme() {
        return getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    }

    private void setThemeType(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme);
        prefs.edit()
                .putInt(KEY_THEME_TYPE, theme)
                .apply();
    }


    void showThemeDialog() {
        String[] options = new String[3];
        options[0] = getString(R.string.action_theme_day);
        options[1] = getString(R.string.action_theme_night);
        if (Build.VERSION.SDK_INT > 28)
            options[2] = getString(R.string.action_theme_system);
        else
            options[2] = getString(R.string.action_theme_battery);


        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(getString(R.string.action_theme))
                .typeface("kanit.ttf", "open_sans.ttf")
                .itemsColor(getColor(R.color.textWhite))
                .items(options)
                .backgroundColor(getColor(R.color.windowBackground))
                .itemsCallbackSingleChoice(getThemeAsIndex(),
                        (dialog1, itemView, which, text) -> {
                            switch (which) {
                                case 0:
                                    setThemeType(AppCompatDelegate.MODE_NIGHT_NO);
                                    break;
                                case 1:
                                    setThemeType(AppCompatDelegate.MODE_NIGHT_YES);
                                    break;
                                case 2:
                                    if (Build.VERSION.SDK_INT > 28)
                                        setThemeType(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                                    else
                                        setThemeType(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                                    break;
                            }
                            return true;
                        })
                .build();
        dialog.show();
    }

    private int getThemeAsIndex() {
        int currentTheme = prefs.getInt(KEY_THEME_TYPE, AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
        switch (currentTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return 0;
            case AppCompatDelegate.MODE_NIGHT_YES:
                return 1;
            case AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY:
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                return 2;
        }
    }
}
