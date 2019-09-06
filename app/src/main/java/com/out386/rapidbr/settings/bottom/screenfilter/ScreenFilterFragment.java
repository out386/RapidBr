package com.out386.rapidbr.settings.bottom.screenfilter;

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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.bottom.views.SwitchItem;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_SCREEN_FILTER_ENABLED;

public class ScreenFilterFragment extends Fragment {

    private SharedPreferences prefs;
    private SwitchItem filterEnable;
    private OnScreenFilterSettingsChangedListener listener;

    public ScreenFilterFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnScreenFilterSettingsChangedListener)
            listener = (OnScreenFilterSettingsChangedListener) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_screen_filter, container, false);
        filterEnable = v.findViewById(R.id.sfilter_enable_switch);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        setupViews();
    }

    private void setupViews() {
        boolean isEnabled = prefs.getBoolean(KEY_SCREEN_FILTER_ENABLED, true);
        filterEnable.setChecked(isEnabled);
        setupListeners();
    }


    private void setupListeners() {
        filterEnable.setOnCheckedChangeListener(isChecked -> {
            prefs.edit()
                    .putBoolean(KEY_SCREEN_FILTER_ENABLED, isChecked)
                    .apply();
            if (listener != null)
                listener.onScreenFilterEnabledChanged(isChecked);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
