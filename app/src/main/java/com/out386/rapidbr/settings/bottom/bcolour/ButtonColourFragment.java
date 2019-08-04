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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.out386.rapidbr.R;

public class ButtonColourFragment extends Fragment {

    private SharedPreferences prefs;

    public ButtonColourFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_button_colour, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        NestedScrollView recyclerRoot = v.findViewById(R.id.button_recycler_parent);
        RecyclerView recyclerView = v.findViewById(R.id.button_recycler);
        float itemSize = getResources().getDimension(R.dimen.button_colour_item_size);
        int columnCount = getRecyclerColumnCount(
                getContext(), recyclerRoot, recyclerRoot, itemSize);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), columnCount);

        int[] colours = getResources().getIntArray(R.array.obutton_colours);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new ColourRecyclerAdapter(getContext(),
                new OnColourChangedListener(), colours));

        return v;
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
            Toast.makeText(getContext(), String.valueOf(colour), Toast.LENGTH_SHORT).show();
        }
    }
}
