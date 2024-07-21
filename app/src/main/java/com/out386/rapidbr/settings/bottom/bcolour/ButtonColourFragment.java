package com.out386.rapidbr.settings.bottom.bcolour;

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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.bottom.views.ButtonHideNestedScrollView;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_BR_ICON_COLOUR;

public class ButtonColourFragment extends Fragment {
    private SharedPreferences prefs;
    private OnButtonColourChangedListener colourListener;
    private ButtonHideNestedScrollView scrollView;

    public ButtonColourFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnButtonColourChangedListener) {
            colourListener = (OnButtonColourChangedListener) context;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        colourListener = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_button_colour, container, false);
        Context context = requireContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        scrollView = v.findViewById(R.id.button_recycler_parent);
        RecyclerView recyclerView = v.findViewById(R.id.button_recycler);

        scrollView.setupButtonHideListener(requireActivity());

        float itemSize = getResources().getDimension(R.dimen.button_colour_item_size);
        int columnCount = getRecyclerColumnCount(context, scrollView, scrollView, itemSize);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), columnCount);

        int[] colours = getResources().getIntArray(R.array.obutton_colours);
        int defColour = prefs.getInt(KEY_BR_ICON_COLOUR,
                getResources().getColor(R.color.obutton_blue, context.getTheme()));

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(
                new ColourRecyclerAdapter(getContext(),
                        new OnColourChangedListener(),
                        colours,
                        defColour)
        );

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        scrollView.forceButtonShow();
    }

    private int getRecyclerColumnCount(Context context, View parent, View child, float pxWidth) {
        int totalPadding = parent.getPaddingRight() + parent.getPaddingLeft()
                + child.getPaddingRight() + child.getPaddingLeft();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels - totalPadding;
        return Math.round(screenWidth / pxWidth);
    }

    private class OnColourChangedListener implements ColourRecyclerAdapter.OnItemChangedListener {
        @Override
        public void onItemChanged(int colour) {
            prefs.edit()
                    .putInt(KEY_BR_ICON_COLOUR, colour)
                    .apply();
            if (colourListener != null)
                colourListener.onColourChanged(colour);
        }
    }
}
