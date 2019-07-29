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

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.out386.rapidbr.settings.bottom.BottomFragment;
import com.out386.rapidbr.settings.top.TopFragment;

public class MainActivity extends AppCompatActivity implements BottomFragment.OnBottomFragEvent {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupViews();
        if (savedInstanceState == null)
            setFragments();
    }

    private void setupViews() {
        View decorView = getWindow().getDecorView();
        FrameLayout bottomViewTopPadding = findViewById(R.id.bottom_view_top_padding);
        FrameLayout topView = findViewById(R.id.topView);
        FrameLayout bottomView = findViewById(R.id.bottom_view);
        LinearLayout toolbar = findViewById(R.id.toolbar);
        ViewGroup.LayoutParams toolbarParams = toolbar.getLayoutParams();
        ViewGroup.LayoutParams bottomViewTopPaddingParams = bottomViewTopPadding.getLayoutParams();

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        decorView.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int leftInset = insets.getSystemWindowInsetLeft();
            int rightInset = insets.getSystemWindowInsetRight();
            int bottomInset = insets.getSystemWindowInsetBottom();
            int actionbarHeight = getActionbarHeight();

            toolbarParams.height = (actionbarHeight > -1 ? actionbarHeight : toolbarParams.height) +
                    topInset;
            bottomViewTopPaddingParams.height += toolbarParams.height;

            bottomViewTopPadding.setLayoutParams(bottomViewTopPaddingParams);
            toolbar.setLayoutParams(toolbarParams);

            toolbar.setPadding(leftInset, 0, rightInset, 0);
            topView.setPadding(leftInset, 0, rightInset, 0);
            bottomView.setPadding(leftInset, 0, rightInset, 0);
            decorView.setPadding(0, 0, 0, bottomInset);

            return insets.consumeSystemWindowInsets();
        });
        setupToolbarText();
    }

    private int getActionbarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return -1;
    }

    private void setupToolbarText() {
        TextView toolbarTV = findViewById(R.id.toolbarText);
        String appName = getString(R.string.app_name);
        int appName1Length = getString(R.string.app_name1).length();
        SpannableString toolbarString = new SpannableString(appName);
        toolbarString.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.toolbarText1)),
                0,
                appName1Length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarString.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.toolbarText2)),
                appName1Length,
                appName.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarTV.setText(toolbarString);
    }

    private void setFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.topView, new TopFragment())
                .replace(R.id.bottom_view, new BottomFragment())
                .commit();
    }

    @Override
    public void onItemTapped() {

    }
}
