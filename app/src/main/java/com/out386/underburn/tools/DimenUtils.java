package com.out386.underburn.tools;

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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class DimenUtils {
    /**
     * Get real metrics of default display
     * @param context Context
     * @return The real metrics of default display
     */
    private static DisplayMetrics getRealDisplayMetrics(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display;
        if (windowManager != null && (display = windowManager.getDefaultDisplay()) != null) {
            DisplayMetrics dm = new DisplayMetrics();
            display.getRealMetrics(dm);
            return dm;
        } else {
            return null;
        }
    }

    /**
     * Get the real height of screen
     * @param context Context
     * @return The real height of screen
     */
    public static int getRealHeight(Context context) {
        DisplayMetrics dm = getRealDisplayMetrics(context);
        return dm != null ? dm.heightPixels : 0;
    }

    /**
     * Get the real width of screen
     * @param context Context
     * @return The real width of screen
     */
    public static int getRealWidth(Context context) {
        DisplayMetrics dm = getRealDisplayMetrics(context);
        return dm != null ? dm.widthPixels : 0;
    }
}
