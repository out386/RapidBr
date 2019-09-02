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

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.out386.rapidbr.R;

import java.util.List;

public class BlacklistPickerItem extends
        AbstractItem<BlacklistPickerItem, BlacklistPickerItem.ViewHolder>
        implements Comparable<BlacklistPickerItem> {

    private String packageName;
    private String appName;
    private Bitmap appIcon;

    private BlacklistPickerItem(BlacklistPickerItem.Builder builder) {
        appName = builder.appName;
        appIcon = builder.appIcon;
        packageName = builder.packageName;
    }

    @Override
    public int getType() {
        return R.id.app_profiles_picker_item_root;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.blacklist_picker_item;
    }

    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public Bitmap getAppIcon() {
        return appIcon;
    }

    protected static class ViewHolder extends FastAdapter.ViewHolder<BlacklistPickerItem> {
        TextView name;
        ImageView icon;

        ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.app_name);
            icon = view.findViewById(R.id.app_icon);
        }

        @Override
        public void bindView(BlacklistPickerItem item, List<Object> payloads) {
            name.setText(item.appName);
            icon.setImageBitmap(item.appIcon);
        }

        @Override
        public void unbindView(BlacklistPickerItem item) {
            name.setText(null);
            icon.setImageDrawable(null);
        }
    }

    static class Builder {
        private String packageName;
        private String appName;
        private Bitmap appIcon;

        BlacklistPickerItem.Builder withPackage(String name) {
            this.packageName = name;
            return this;
        }

        BlacklistPickerItem.Builder withName(String name) {
            this.appName = name;
            return this;
        }

        BlacklistPickerItem.Builder withIcon(Bitmap icon) {
            this.appIcon = icon;
            return this;
        }

        BlacklistPickerItem build() {
            return new BlacklistPickerItem(this);
        }
    }

    @Override
    public int compareTo(@NonNull BlacklistPickerItem o) {
        if (this.appName != null)
            return this.appName.compareToIgnoreCase(o.appName);
        else
            return 1;
    }
}
