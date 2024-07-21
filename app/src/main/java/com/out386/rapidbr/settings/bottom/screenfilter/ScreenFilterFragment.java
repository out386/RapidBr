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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.bottom.views.ButtonHideNestedScrollView;
import com.out386.rapidbr.settings.bottom.views.SwitchItem;
import com.out386.rapidbr.utils.ViewUtils;
import com.ramotion.fluidslider.FluidSlider;

import kotlin.Unit;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_SCREEN_FILTER_ENABLED;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_TEMP_FILTER_ENABLED;

public class ScreenFilterFragment extends Fragment {

    private SharedPreferences prefs;
    private SwitchItem filterEnable;
    private SwitchItem tempEnable;
    private FluidSlider tempSlider;
    private TextView tempTitle;
    private ButtonHideNestedScrollView scrollView;
    private FluidSlider tempIntenSlider;
    private TextView tempIntenTitle;
    private LinearLayout disableView;
    private OnScreenFilterSettingsChangedListener listener;
    private int colourAccent;
    private float tempPerc;
    private float tempAlphaPerc;

    public static final String KEY_FILTER_TEMPERATURE = "scrFilterTemp";
    private static final String KEY_FILTER_TEMPERATURE_PERC = "scrFilterTemp%";
    private static final String KEY_FILTER_TEMPERATURE_ALPHA = "scrFilterTempIntensity";

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
        tempEnable = v.findViewById(R.id.sfilter_temperature_switch);
        tempSlider = v.findViewById(R.id.sfilter_temp_slider);
        tempTitle = v.findViewById(R.id.sfilter_temp_title);
        tempIntenSlider = v.findViewById(R.id.sfilter_temp_inten_slider);
        tempIntenTitle = v.findViewById(R.id.sfilter_temp_inten_title);
        disableView = v.findViewById(R.id.sfilter_disable_temp);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        scrollView = v.findViewById(R.id.scroll_view);
        scrollView.setupButtonHideListener(requireActivity());

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        scrollView.forceButtonShow();
        setupViews();
    }

    private void setupViews() {
        boolean isEnabled = prefs.getBoolean(KEY_SCREEN_FILTER_ENABLED, true);
        boolean isTempEnabled = prefs.getBoolean(KEY_TEMP_FILTER_ENABLED, false);
        tempPerc = prefs.getFloat(KEY_FILTER_TEMPERATURE_PERC, 0F);
        tempAlphaPerc = prefs.getFloat(KEY_FILTER_TEMPERATURE_ALPHA, 0F);
        saveTemp(null);

        String tempText = percentToDisplay(true, tempPerc) + "%";
        String alphaText = percentToDisplay(false, tempAlphaPerc) + "%";
        tempSlider.setBubbleText(tempText);
        tempIntenSlider.setBubbleText(alphaText);
        tempSlider.setPosition(tempPerc);
        tempIntenSlider.setPosition(tempAlphaPerc);
        filterEnable.setChecked(isEnabled);
        tempEnable.setChecked(isTempEnabled);
        if (isTempEnabled)
            disableView.setVisibility(View.GONE);
        else
            disableView.setVisibility(View.VISIBLE);

        colourAccent = getResources().getColor(R.color.colorSlider, requireContext().getTheme());
        tempSlider.setColorBar(colourAccent);
        tempIntenSlider.setColorBar(colourAccent);
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

        tempEnable.setOnCheckedChangeListener(isChecked -> {
            prefs.edit()
                    .putBoolean(KEY_TEMP_FILTER_ENABLED, isChecked)
                    .apply();
            if (isChecked)
                disableView.setVisibility(View.GONE);
            else
                disableView.setVisibility(View.VISIBLE);
            saveTemp(null);
        });

        tempSlider.setPositionListener(pos -> {
            String posStr = percentToDisplay(true, pos) + "%";
            tempSlider.setBubbleText(posStr);
            tempPerc = pos;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(KEY_FILTER_TEMPERATURE_PERC, pos);
            saveTemp(editor);
            return Unit.INSTANCE;
        });

        tempIntenSlider.setPositionListener(pos -> {
            String posStr = percentToDisplay(false, pos) + "%";
            tempIntenSlider.setBubbleText(posStr);
            tempAlphaPerc = pos;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(KEY_FILTER_TEMPERATURE_ALPHA, pos);
            saveTemp(editor);
            return Unit.INSTANCE;
        });

        setViewHideListeners();
    }

    private int percentToDisplay(boolean temperature, float percent) {
        if (temperature)
            return (int) (percent * 100);
        else
            return (int) (percent * 49 + 1);
    }

    private void saveTemp(@Nullable SharedPreferences.Editor editor) {
        boolean isEnabled = prefs.getBoolean(KEY_TEMP_FILTER_ENABLED, false);
        if (isEnabled) {
            // Range = 1% - 50% of 255 (0xFF)
            int alpha = (int) Math.round(tempAlphaPerc * 124.95 + 2.55);

            // Low temp percentages in the slider is cool, high is warm
            float tempPerc = 1 - this.tempPerc;

            // 2000 kelvin to 10,000 kelvin
            int temp = (int) (tempPerc * 8000 + 2000);
            int tempColour = 0x1000000 * alpha + TemperatureCalc.getTemperatureRGB(temp);
            if (editor != null) {
                editor.putInt(KEY_FILTER_TEMPERATURE, tempColour)
                        .apply();
            }
            if (listener != null)
                listener.onColourTemperatureChanged(tempColour);
        } else {
            if (listener != null)
                listener.onColourTemperatureChanged(0x0);
        }
    }

    private void setViewHideListeners() {
        Interpolator interpolator = new DecelerateInterpolator();
        tempSlider.setBeginTrackingListener(() -> {
            ViewUtils.animateView(tempTitle, false, interpolator, interpolator,
                    400, 150);
            return Unit.INSTANCE;
        });
        tempSlider.setEndTrackingListener(() -> {
            ViewUtils.animateView(tempTitle, true, interpolator, interpolator,
                    400, 150);
            tempSlider.setColorBar(colourAccent);
            return Unit.INSTANCE;
        });

        tempIntenSlider.setBeginTrackingListener(() -> {
            ViewUtils.animateView(tempIntenTitle, false, interpolator, interpolator,
                    400, 150);
            return Unit.INSTANCE;
        });
        tempIntenSlider.setEndTrackingListener(() -> {
            ViewUtils.animateView(tempIntenTitle, true, interpolator, interpolator,
                    400, 150);
            tempIntenSlider.setColorBar(colourAccent);
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
