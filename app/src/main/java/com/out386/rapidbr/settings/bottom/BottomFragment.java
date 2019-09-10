package com.out386.rapidbr.settings.bottom;

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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.MainActivityListener;
import com.out386.rapidbr.settings.bottom.blacklist.BlacklistActivity;
import com.out386.rapidbr.settings.bottom.views.CardLayout;
import com.out386.rapidbr.settings.bottom.views.SwitchItem;

import static com.out386.rapidbr.settings.bottom.scheduler.BootReceiver.KEY_START_ON_BOOT;

public class BottomFragment extends Fragment {

    private MainActivityListener listener;
    private SharedPreferences prefs;

    public BottomFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        setViewListeners(v);
        return v;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivityListener) {
            listener = (MainActivityListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationListener");
        }
    }

    private void setViewListeners(View root) {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.bottom_view);
        CardLayout schedulerCard = root.findViewById(R.id.scheduler_card);
        CardLayout colourCard = root.findViewById(R.id.color_card);
        CardLayout blacklistCard = root.findViewById(R.id.blacklist_card);
        CardLayout screenFilterCard = root.findViewById(R.id.filter_card);
        SwitchItem startOnBoot = root.findViewById(R.id.boot_start_switch);

        setClickListener(navController, schedulerCard, R.id.action_bottom_to_scheduler);
        setClickListener(navController, colourCard, R.id.action_bottom_to_buttonColour);
        setClickListener(navController, screenFilterCard, R.id.action_bottom_to_screenFilter);

        blacklistCard.setOnClickListener(view -> {
            Intent startIntent = new Intent(requireContext(), BlacklistActivity.class);
            startIntent.putExtra(BlacklistActivity.KEY_BOS_START_OR_PAUSE_STAT,
                    listener.getBOSRunning());
            startActivity(startIntent);
        });

        startOnBoot.setChecked(prefs.getBoolean(KEY_START_ON_BOOT, false));
        startOnBoot.setOnCheckedChangeListener(isChecked -> {
            prefs.edit()
                    .putBoolean(KEY_START_ON_BOOT, isChecked)
                    .apply();
        });
    }

    private void setClickListener(NavController navController, View target, int actionRes) {
        target.setOnClickListener(view -> {
            listener.onAltFragment();
            navController.navigate(actionRes);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        listener.onMainFragment();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}
