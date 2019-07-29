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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.out386.rapidbr.R;

public class CardLayout extends RelativeLayout {

    public CardLayout(Context context) {
        this(context, null);
    }

    public CardLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CardLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        inflate(getContext(), R.layout.view_settings_card, this);
        ImageView image = findViewById(R.id.settings_card_image);
        TextView text = findViewById(R.id.settings_card_text);
        RelativeLayout root = findViewById(R.id.settings_card_root);
        TypedArray a = getContext()
                .obtainStyledAttributes(attrs, R.styleable.CardLayout, 0, 0);
        String textStr = a.getString(R.styleable.CardLayout_text);
        int imageRes = a.getResourceId(R.styleable.CardLayout_image, R.drawable.ic_launcher_background);
        int imageTint = a.getResourceId(R.styleable.CardLayout_image_tint, R.color.colorAccent);
        a.recycle();

        Resources resources = getResources();
        Resources.Theme theme = getContext().getTheme();

        ColorStateList imageTintList = ColorStateList.valueOf(resources.getColor(imageTint, theme))
                .withAlpha(0xFF);
        image.setImageResource(imageRes);
        image.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        image.setImageTintList(imageTintList);
        root.setBackground(getRipple(resources, theme, imageTint));
        text.setText(textStr);
    }

    private RippleDrawable getRipple(Resources resources, Resources.Theme theme, int imageTint) {

        int rippleTintColor = ColorUtils.setAlphaComponent(
                resources.getColor(imageTint, theme), 0x30);
        ColorStateList csl = new ColorStateList(new int[][]{
                new int[]{}
        },
                new int[]{rippleTintColor}
        );
        Drawable cardDrawable = resources.getDrawable(R.drawable.card, theme);
        Drawable cardDrawableFilled = resources.getDrawable(R.drawable.card_filled, theme);

        return new RippleDrawable(csl, cardDrawable, cardDrawableFilled);
    }

}
