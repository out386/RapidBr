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
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.core.graphics.ColorUtils;

import com.out386.rapidbr.R;

public class ColourItem extends RelativeLayout {
    private ImageView image;
    private ImageView check;
    private int strokeWidth;
    private int colour;
    private int size;

    public ColourItem(Context context) {
        this(context, null);
    }

    public ColourItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColourItem(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColourItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.item_button_colour, this);
        image = findViewById(R.id.button_image);
        check = findViewById(R.id.button_check);
        check.setScaleX(0);
        check.setScaleY(0);
    }

    public void setColour(int colour, int strokeWidth, float size) {
        this.colour = colour;
        this.strokeWidth = strokeWidth;
        this.size = (int) size;
        setColour(false);
    }

    public void setChecked(boolean isChecked) {
        if (isChecked) {
            check.animate()
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .scaleY(1)
                    .scaleX(1);
            setColour(true);
        } else {
            check.animate()
                    .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                    .scaleY(0)
                    .scaleX(0);
            setColour(false);
        }

    }

    public int getColour() {
        return colour;
    }

    private void setColour(boolean isEnabled) {
        int alpha;
        int strokeColour;
        if (isEnabled) {
            alpha = 0xCF;
            strokeColour = ColorUtils.blendARGB(colour, 0xFFFFFFFF, 0.3f);
        } else {
            alpha = 0x3F;
            strokeColour = colour;
        }
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.OVAL);
        gradientDrawable.setColor(ColorUtils.setAlphaComponent(colour, alpha));
        gradientDrawable.setStroke(strokeWidth, strokeColour);
        gradientDrawable.setSize(size, size);
        image.setImageDrawable(gradientDrawable);
    }
}
