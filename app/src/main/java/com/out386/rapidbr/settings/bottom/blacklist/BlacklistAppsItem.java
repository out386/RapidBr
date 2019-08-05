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
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.out386.rapidbr.R;

import java.io.Serializable;
import java.util.List;

public class BlacklistAppsItem extends AbstractItem<BlacklistAppsItem, BlacklistAppsItem.ViewHolder> implements Serializable {

    private static final float DEFAULT_BRIGHTNESS = 178.5f;     // 70% of 255
    private String packageName;
    private String appName;
    private transient Bitmap appIcon;
    /**
     * True means, "Start"
     */
    private boolean startRapidbr;
    private float brightness;

    private BlacklistAppsItem(BlacklistAppsItem.Builder builder) {
        appName = builder.appName;
        appIcon = builder.appIcon;
        packageName = builder.packageName;
        startRapidbr = builder.behaviour;
        brightness = builder.brightness;
    }

    @Override
    public int getType() {
        return R.id.app_profiles_apps_item_root;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.blacklist_apps_item;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    String getPackageName() {
        return packageName;
    }

    String getAppName() {
        return appName;
    }

    /**
     * Getter for startRapidbr
     *
     * @return True to start RapidBr. False otherwise.
     */
    boolean getStartRapidbr() {
        return startRapidbr;
    }

    /**
     * @return The brightness to be used for this app, between -1F to 255F. -1F means brightness
     * will be unchanged
     */
    float getBrightness() {
        return brightness;
    }

    void setAppIcon(Bitmap appIcon) {
        this.appIcon = appIcon;
    }

    void setAppBehaviour(boolean behaviour) {
        this.startRapidbr = behaviour;
    }

    /**
     * Set the brightness to be used for this app, between -1 to 255. -1 means brightness will be
     * unchanged
     */
    void setAppBrightness(float brightness) {
        if (brightness < -1)
            brightness = -1;
        else if (brightness > 255)
            brightness = 255;
        this.brightness = brightness;
    }

    protected static class ViewHolder extends FastAdapter.ViewHolder<BlacklistAppsItem> {
        TextView name;
        TextView behaviour;
        TextView brightness;
        ImageView icon;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.app_name);
            behaviour = view.findViewById(R.id.app_behaviour);
            brightness = view.findViewById(R.id.app_brightness);
            icon = view.findViewById(R.id.app_icon);
        }

        @Override
        public void bindView(BlacklistAppsItem item, List<Object> payloads) {
            name.setText(item.appName);
            if (item.startRapidbr) {
                behaviour.setText(R.string.sett_blacklist_on_stop);
                brightness.setVisibility(View.GONE);
            } else {
                behaviour.setText("FIX THIS");
                brightness.setVisibility(View.VISIBLE);
                Context context = brightness.getContext();
                String text;
                if (item.brightness == -1.0F)
                    text = context.getString(R.string.sett_blacklist_unchanged);
                else {
                    String message = context.getString(R.string.sett_blacklist_changed);
                    int brightnessPercent = (int) (item.brightness / 2.55F);
                    text = String.format(message, brightnessPercent);
                }
                brightness.setText(text);
            }

            icon.setImageBitmap(item.appIcon);
        }

        @Override
        public void unbindView(BlacklistAppsItem item) {
            name.setText(null);
            behaviour.setText(null);
            brightness.setText(null);
            icon.setImageDrawable(null);
        }
    }

    public static class Builder {

        private String packageName;
        private String appName;
        private boolean behaviour = false;
        private float brightness = DEFAULT_BRIGHTNESS;
        private Bitmap appIcon;

        BlacklistAppsItem.Builder withPackage(String name) {
            this.packageName = name;
            return this;
        }

        BlacklistAppsItem.Builder withName(String name) {
            this.appName = name;
            return this;
        }

        public BlacklistAppsItem.Builder withBehaviour(boolean behaviour) {
            this.behaviour = behaviour;
            return this;
        }

        public BlacklistAppsItem.Builder withBrightness(float brightness) {
            this.brightness = brightness;
            return this;
        }

        BlacklistAppsItem.Builder withIcon(Bitmap icon) {
            this.appIcon = icon;
            return this;
        }

        BlacklistAppsItem build() {
            return new BlacklistAppsItem(this);
        }
    }
}
