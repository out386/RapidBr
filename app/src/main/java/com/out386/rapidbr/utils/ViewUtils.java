package com.out386.rapidbr.utils;

import android.view.View;
import android.view.animation.Interpolator;

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

public class ViewUtils {

    public static void animateView(View view, boolean show, Interpolator showInterpolator,
                                   Interpolator hideInterpolator, long showDuration,
                                   long hideDuration) {
        if (show) {
            view.setAlpha(0);
            view.setTranslationY(-view.getHeight());
            view.animate()
                    .alpha(1)
                    .translationY(0)
                    .setDuration(showDuration)
                    .setInterpolator(showInterpolator)
                    .start();
        } else {
            view.animate()
                    .alpha(0)
                    .translationY(-view.getHeight())
                    .setDuration(hideDuration)
                    .setInterpolator(hideInterpolator)
                    .start();
        }
    }
}
