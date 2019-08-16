package com.out386.rapidbr.services.overlay;

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

import android.os.Handler;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.DEF_OVERLAY_BUTTON_ALPHA;

class ButtonAnim {
    private static final int DELAY = 1000;
    private static final int DELAY_SHORT = 600;
    private static final int ANIM_DURATION = 300;

    private ImageView brightnessButton;
    private DisplayMetrics displayMetrics;
    private Vibrator vibrator;
    private ScaleRunnable scaleRunnable;
    private TranslateRunnable translateRunnable;
    private Handler scaleHandler;
    private Handler translateHandler;
    private BrightnessOverlayService service;

    ButtonAnim(ImageView brightnessButton, DisplayMetrics displayMetrics, Vibrator vibrator,
               BrightnessOverlayService service) {
        this.brightnessButton = brightnessButton;
        this.displayMetrics = displayMetrics;
        this.vibrator = vibrator;
        this.service = service;
        scaleRunnable = new ScaleRunnable();
        translateRunnable = new TranslateRunnable();
        scaleHandler = new Handler();
        translateHandler = new Handler();
    }

    void shutdown() {
        if (scaleHandler != null && scaleRunnable != null)
            scaleHandler.removeCallbacksAndMessages(null);
        if (translateHandler != null && translateRunnable != null)
            translateHandler.removeCallbacksAndMessages(null);
    }

    void showButton() {
        translateHandler.removeCallbacks(translateRunnable);
        translateHandler.post(translateRunnable.show());
    }

    void hideButtonDelayed() {
        translateHandler.removeCallbacks(translateRunnable);
        translateHandler.postDelayed(
                translateRunnable.hide(), DELAY);
    }

    void scaleUpButtonDelayed() {
        scaleHandler.removeCallbacks(scaleRunnable);
        scaleHandler.postDelayed(scaleRunnable, DELAY_SHORT);
    }

    void cancelScale() {
        scaleHandler.removeCallbacks(scaleRunnable);
    }

    void scaleSlider(boolean scaleUp) {
        float factor;
        if (scaleUp) {
            factor = 1.5F;
        } else {
            factor = 1;
        }
        brightnessButton.animate()
                .setDuration(ANIM_DURATION)
                .scaleY(factor)
                .scaleX(factor)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private class ScaleRunnable implements Runnable {
        @Override
        public void run() {
            service.moveWasBrightness = false;
            if (vibrator != null)
                vibrator.vibrate(100);
            scaleSlider(true);
        }
    }

    private class TranslateRunnable implements Runnable {
        boolean isHide;

        @Override
        public void run() {
            if (brightnessButton != null) {
                float translateX;
                float alpha;

                if (isHide) {
                    translateX = getHidePosition();
                    alpha = DEF_OVERLAY_BUTTON_ALPHA;
                } else {
                    translateX = 0;
                    alpha = 1;
                }

                brightnessButton.animate()
                        .alpha(alpha)
                        .setDuration(ANIM_DURATION)
                        .translationX(translateX)
                        .start();
            }
        }

        private int getHidePosition() {
            float currentPos = ((WindowManager.LayoutParams) brightnessButton.getLayoutParams()).x;
            int position = -brightnessButton.getWidth() / 2;

            // If the button is in the right side of the screen, hide it to the right side instead
            if (currentPos > displayMetrics.widthPixels / 2)
                position *= -1;
            return position;
        }

        TranslateRunnable hide() {
            isHide = true;
            return this;
        }

        TranslateRunnable show() {
            isHide = false;
            return this;
        }
    }
}
