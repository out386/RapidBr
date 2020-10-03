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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.out386.rapidbr.ads.AdManager;
import com.out386.rapidbr.ads.OnAdLoadedListener;
import com.out386.rapidbr.settings.MainActivityListener;
import com.out386.rapidbr.settings.bottom.blacklist.BlacklistActivity;
import com.out386.rapidbr.settings.bottom.views.ButtonHideNestedScrollView;
import com.out386.rapidbr.settings.bottom.views.CardLayout;
import com.out386.rapidbr.settings.bottom.views.SwitchItem;

import static com.out386.rapidbr.settings.bottom.scheduler.BootAlarmReceiver.KEY_START_ON_BOOT;

public class MainFragment extends Fragment implements OnAdLoadedListener {
    private static final int AD_REQUESTER_ID = 0;
    private MainActivityListener listener;
    private SharedPreferences prefs;
    private SwitchItem startOnBoot;
    private ButtonHideNestedScrollView scrollView;
    private View rootView;
    private boolean isFragmentStopped = true;
    private AdManager adManager;
    private UnifiedNativeAdView currentAd;

    public MainFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        setViewListeners(view);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivityListener) {
            listener = (MainActivityListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationListener and OnAdRequestListener");
        }
        adManager = AdManager.getInstance(context);
    }

    private void setViewListeners(View root) {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.bottom_view);
        CardLayout schedulerCard = root.findViewById(R.id.scheduler_card);
        CardLayout colourCard = root.findViewById(R.id.color_card);
        CardLayout blacklistCard = root.findViewById(R.id.blacklist_card);
        CardLayout screenFilterCard = root.findViewById(R.id.filter_card);
        startOnBoot = root.findViewById(R.id.boot_start_switch);

        setClickListener(navController, schedulerCard, R.id.action_bottom_to_scheduler);
        setClickListener(navController, colourCard, R.id.action_bottom_to_buttonColour);
        setClickListener(navController, screenFilterCard, R.id.action_bottom_to_screenFilter);

        blacklistCard.setOnClickListener(view -> {
            Intent startIntent = new Intent(requireContext(), BlacklistActivity.class);
            startIntent.putExtra(BlacklistActivity.KEY_BOS_START_OR_PAUSE_STAT,
                    listener.getBOSRunning());
            startActivity(startIntent);
        });

        startOnBoot.setOnCheckedChangeListener(isChecked -> {
            prefs.edit()
                    .putBoolean(KEY_START_ON_BOOT, isChecked)
                    .apply();
        });


        scrollView = root.findViewById(R.id.scroll_view);
        scrollView.setupButtonHideListener(requireActivity());
    }

    private void setClickListener(NavController navController, View target, int actionRes) {
        target.setOnClickListener(view -> {
            navController.navigate(actionRes);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        isFragmentStopped = false;
        adManager.getAd(this, AD_REQUESTER_ID);
    }

    @Override
    public void onStop() {
        super.onStop();
        isFragmentStopped = true;
        if (currentAd != null)
            currentAd.destroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Delay, because the top view scrolls and triggers a button hide
        scrollView.forceButtonShowDelayed(10);
        startOnBoot.setChecked(prefs.getBoolean(KEY_START_ON_BOOT, false));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        adManager = null;
    }

    @Override
    public void onAdLoaded(@Nullable UnifiedNativeAd ad) {
        if (isFragmentStopped)
            return;

        LinearLayout adViewRoot = rootView.findViewById(R.id.ad_view);
        currentAd = AdManager.inflateAd(adViewRoot, ad);
    }
}
