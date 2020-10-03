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
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.out386.rapidbr.R;
import com.out386.rapidbr.ads.AdManager;
import com.out386.rapidbr.ads.OnAdLoadedListener;
import com.out386.rapidbr.settings.bottom.views.ButtonHideNestedScrollView;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.KEY_BR_ICON_COLOUR;

public class ButtonColourFragment extends Fragment implements OnAdLoadedListener {
    private static final int AD_REQUESTER_ID = 2;
    private SharedPreferences prefs;
    private OnButtonColourChangedListener colourListener;
    private ButtonHideNestedScrollView scrollView;
    private View rootView;
    private boolean isFragmentStopped = true;
    //private AdManager adManager;
    private UnifiedNativeAdView currentAd;

    public ButtonColourFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnButtonColourChangedListener) {
            colourListener = (OnButtonColourChangedListener) context;
        }
        //adManager = AdManager.getInstance(context);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        colourListener = null;
        //adManager = null;
    }


    @Override
    public void onStart() {
        super.onStart();
        isFragmentStopped = false;
        //adManager.getAd(this, AD_REQUESTER_ID);
    }

    @Override
    public void onStop() {
        super.onStop();
        isFragmentStopped = true;
        if (currentAd != null)
            currentAd.destroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_button_colour, container, false);
        Context context = requireContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        scrollView = rootView.findViewById(R.id.button_recycler_parent);
        RecyclerView recyclerView = rootView.findViewById(R.id.button_recycler);

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

        return rootView;
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


    @Override
    public void onAdLoaded(@Nullable UnifiedNativeAd ad) {
        if (isFragmentStopped)
            return;

        LinearLayout adViewRoot = rootView.findViewById(R.id.ad_view);
        currentAd = AdManager.inflateAd(adViewRoot, ad);
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
