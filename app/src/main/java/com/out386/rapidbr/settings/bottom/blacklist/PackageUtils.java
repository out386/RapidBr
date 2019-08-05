package com.out386.rapidbr.settings.bottom.blacklist;

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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerItem;

import java.util.List;

public class PackageUtils {

    public static String getAppName(PackageManager pm, String packageName, ApplicationInfo applicationInfo) {
        if (applicationInfo != null) {
            CharSequence name = pm.getApplicationLabel(applicationInfo);
            return (name == null || "".contentEquals(name)) ? packageName : name.toString();
        } else
            return packageName;
    }

    /**
     * Fetches the icon for a given app. Returns the default Oreo icon if no icon exists.
     *
     * @param context     A Context
     * @param pm          An instance of PackageManager
     * @param packageName The name of the package to fetch the icon for
     * @return The icon for the package
     */
    public static Bitmap getIcon(Context context, PackageManager pm, String packageName) {
        Bitmap appIcon;
        Drawable drawable;
        try {
            drawable = pm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Drawable icon = context.getDrawable(R.drawable.ic_profiles_default_icon);
            if (icon == null)
                return null;
            return getBitmapFromDrawable(icon);
        }
        if (drawable instanceof BitmapDrawable)
            appIcon = ((BitmapDrawable) drawable).getBitmap();
        else
            appIcon = getBitmapFromDrawable(drawable);
        return appIcon;
    }

    public static void setIcon(Context context, PackageManager packageManager, BlacklistAppsItem app) {
        app.setAppIcon(getIcon(context, packageManager, app.getPackageName()));
    }

    // Author: Evgenii Kanivets
    @NonNull
    private static Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    /**
     * Takes an BlacklistPickerItem and sets its contents to an BlacklistAppsItem.
     *
     * @param item The target BlacklistPickerItem
     * @return The BlacklistAppsItem containing the data from {@code item}
     */
    static BlacklistAppsItem pickerToAppItem(BlacklistPickerItem item) {
        return new BlacklistAppsItem.Builder()
                .withPackage(item.getPackageName())
                .withName(item.getAppName())
                .withIcon(item.getAppIcon())
                .build();

    }

    /**
     * Checks if newItem is in apps
     *
     * @param apps    The list to search in
     * @param newItem The item to search for
     * @return True if the item is unique, false otherwise
     */
    static boolean checkUnique(List<BlacklistAppsItem> apps, BlacklistAppsItem newItem) {
        if (newItem == null)
            return false;
        for (int i = 0; i < apps.size(); i++) {
            BlacklistAppsItem current = apps.get(i);
            if (current.getPackageName().equals(newItem.getPackageName())) {
                return false;
            }
        }
        return true;
    }

}
