package com.out386.rapidbr.settings.bottom.views;

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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.out386.rapidbr.R;

public class SwitchItem extends RelativeLayout {
    private Switch itemSwitch;
    private TextView label;
    private RelativeLayout root;
    private OnCheckedChangeListener checkedListener;

    public SwitchItem(Context context) {
        this(context, null);
    }

    public SwitchItem(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwitchItem(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        inflate(getContext(), R.layout.view_switch_item, this);
        label = findViewById(R.id.settings_switch_text);
        itemSwitch = findViewById(R.id.settings_switch_switch);
        TypedArray a = getContext()
                .obtainStyledAttributes(attrs, R.styleable.SwitchItem, 0, 0);
        String textStr = a.getString(R.styleable.SwitchItem_switchText);
        a.recycle();
        label.setText(textStr);
        setListeners();
    }

    private void setListeners() {
        root = findViewById(R.id.settings_switch_root);
        root.setOnClickListener(view -> itemSwitch.setChecked(!itemSwitch.isChecked()));
        itemSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkedListener != null)
                checkedListener.onChecked(isChecked);
        });
    }

    public void setChecked(boolean isChecked) {
        itemSwitch.setChecked(isChecked);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        checkedListener = listener;
    }

    public void setEnabled(boolean isEnabled) {
        itemSwitch.setEnabled(isEnabled);
        if (isEnabled) {
            label.setTextColor(getContext().getColor(R.color.textHeaderSub));
            setListeners();
        } else {
            label.setTextColor(getContext().getColor(R.color.textSub));
            root.setOnClickListener(null);
            root.setClickable(false);
            itemSwitch.setOnClickListener(null);
        }
    }

    public interface OnCheckedChangeListener {
        void onChecked(boolean isChecked);
    }
}
