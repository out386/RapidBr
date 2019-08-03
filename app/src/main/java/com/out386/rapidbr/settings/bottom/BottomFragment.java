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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.OnNavigationListener;

public class BottomFragment extends Fragment {

    private OnNavigationListener listener;

    public BottomFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom, container, false);
        setViewListeners(v);
        return v;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigationListener) {
            listener = (OnNavigationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnNavigationListener");
        }
    }

    private void setViewListeners(View root) {
        NavController navController = Navigation.findNavController(getActivity(), R.id.bottom_view);
        CardLayout schedulerCard = root.findViewById(R.id.scheduler_card);
        CardLayout colourCard = root.findViewById(R.id.color_card);

        setClickListener(navController, schedulerCard, R.id.action_bottom_to_scheduler);
        setClickListener(navController, colourCard, R.id.action_bottom_to_buttonColour);
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
