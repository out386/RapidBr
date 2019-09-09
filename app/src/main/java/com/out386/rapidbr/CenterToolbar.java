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
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.out386.rapidbr.utils.DimenUtils;

public class CenterToolbar extends Toolbar {
    private TextView toolbarTv;
    private int[] arr = new int[2];

    public CenterToolbar(Context context) {
        this(context, null);
    }

    public CenterToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.toolbarStyle);
    }

    public CenterToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        toolbarTv = getToolbarTV();
        addView(toolbarTv, params);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int width = DimenUtils.getRealWidth(getContext());
        toolbarTv.getLocationOnScreen(arr);
        int targetPos = (width / 2) - (toolbarTv.getWidth() / 2 + arr[0]);
        toolbarTv.setTranslationX(toolbarTv.getTranslationX() + targetPos);
    }

    public TextView getToolbarTV() {
        Context context = getContext();
        Resources.Theme theme = context.getTheme();
        TextView toolbarTV = new TextView(getContext());
        toolbarTV.setTextAppearance(R.style.ToolbarText);

        String appName = context.getString(R.string.app_name);
        int appName1Length = context.getString(R.string.app_name1).length();
        SpannableString toolbarString = new SpannableString(appName);
        toolbarString.setSpan(
                new ForegroundColorSpan(getResources()
                        .getColor(R.color.colorAccent, theme)),
                0,
                appName1Length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarString.setSpan(
                new ForegroundColorSpan(getResources()
                        .getColor(R.color.toolbarText2, theme)),
                appName1Length,
                appName.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarTV.setText(toolbarString);
        return toolbarTV;
    }
}
