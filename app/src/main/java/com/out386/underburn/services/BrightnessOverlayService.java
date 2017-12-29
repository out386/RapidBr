package com.out386.underburn.services;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.out386.underburn.R;
import com.out386.underburn.tools.Utils;

public class BrightnessOverlayService extends Service implements View.OnTouchListener {

    public static final String KEY_SB_HEIGHT = "statusbarHeight";
    public static final String KEY_OVERLAY_X = "overlayX";
    public static final String KEY_OVERLAY_Y = "overlayY";
    public static final String KEY_OVERLAY_BUTTON_COLOUR = "floatingColour";
    public static final String KEY_OVERLAY_BUTTON_ALPHA = "overlayButtonAlpha";
    public static final String KEY_SCREEN_DIM_AMOUNT = "screenDimAmount";
    public static final String ACTION_SCREEN_DIM_AMOUNT = "actionScreenDimAmount";
    public static final int DEF_OVERLAY_BUTTON_COLOUR = 0x4A4A4A;
    public static final int DEF_OVERLAY_BUTTON_ALPHA = 50;

    private static final int BUTTON_TOUCH_SLOP = 15;
    private static final int BRIGHTNESS_CHANGE_FACTOR = 60;
    private View topLeftView;
    private ImageView brightnessSlider;
    private View dimView;
    private WindowManager.LayoutParams dimViewParams;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;
    private WindowManager wm;
    private DisplayMetrics display;
    private boolean moveWasBrightness = true;
    private float lastX;
    private float lastY;
    private Handler brightnessHandler = new Handler();
    private Handler scaleHandler = new Handler();
    private Handler translateHandler = new Handler();
    private BrightnessRunnable brightnessRunnable = new BrightnessRunnable();
    private ScaleRunnable scaleRunnable = new ScaleRunnable();
    private TranslateRunnable translateRunnable = new TranslateRunnable();
    private boolean brightnessUp;
    private int statusbarHeight;
    private int brightnessChangeBy;
    private SharedPreferences prefs;
    private float imageAlpha;
    private int alertType;
    public static float screenDimAmount = 0.0f;
    private BroadcastReceiver screenDimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float amount = intent.getFloatExtra(KEY_SCREEN_DIM_AMOUNT, 0.0f);
            if (amount > 0.8f)
                amount = 0.8f; // Prevent things from getting too dark
            setDimmerBrightness(amount);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        statusbarHeight = prefs
                .getInt(KEY_SB_HEIGHT, 1);
        screenDimAmount = prefs.getInt(KEY_SCREEN_DIM_AMOUNT, 0) / 100f;
        imageAlpha = prefs.getInt(KEY_OVERLAY_BUTTON_ALPHA, DEF_OVERLAY_BUTTON_ALPHA);
        imageAlpha /= 100F;

        display = this.getResources().getDisplayMetrics();
        int sliderX = prefs.getInt(KEY_OVERLAY_X, 0);
        // As there's a new type in O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            alertType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            alertType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

        setupBrightnessButton(sliderX, alertType);
        setupReferenceView(alertType);
        if (screenDimAmount > 0.0f)
            setupDimmerView();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(screenDimReceiver, new IntentFilter(ACTION_SCREEN_DIM_AMOUNT));

        // Handler instead of startDelay to prevent slider width from being 0
        new Handler().postDelayed(() -> {
            if (brightnessSlider != null)
                brightnessSlider.animate()
                        .translationX(
                                (brightnessSlider.getWidth() / 2F) * (sliderX <= 10 ? -1 : 1)) //Move button left or right
                        .setDuration(500)
                        .alpha(imageAlpha)
                        .start();
        }, 1000);

    }

    private void setupBrightnessButton(int sliderX, int alertType) {
        final float scale = getResources().getDisplayMetrics().density;
        int buttonImageTintColour = prefs.getInt(KEY_OVERLAY_BUTTON_COLOUR, DEF_OVERLAY_BUTTON_COLOUR);
        int pixels = (int) (64 * scale + 0.5f);
        ColorStateList csl = ColorStateList.valueOf(buttonImageTintColour)
                .withAlpha(0xFF);
        brightnessSlider = new ImageView(this);
        brightnessSlider.setScaleType(ImageView.ScaleType.FIT_XY);
        brightnessSlider.setImageResource(R.drawable.ic_overlay_brightness);
        brightnessSlider.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        brightnessSlider.setImageTintList(csl);
        brightnessSlider.setOnTouchListener(this);

        WindowManager.LayoutParams params = new WindowManager
                .LayoutParams(pixels,
                pixels,
                alertType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;

        params.x = sliderX;
        params.y = prefs.getInt(KEY_OVERLAY_Y, 300);
        wm.addView(brightnessSlider, params);
    }

    private void setupDimmerView() {
        dimView = new View(this);
        dimView.setBackgroundColor(Color.BLACK);
        if (alertType == WindowManager.LayoutParams.TYPE_SYSTEM_ERROR) {
            dimViewParams = new WindowManager
                    .LayoutParams(0, 0, alertType,
                    (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_DIM_BEHIND
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                            & 0xFFFFFF7F
                            & 0xFFDFFFFF,
                    PixelFormat.OPAQUE);
            dimViewParams.dimAmount = screenDimAmount;
        } else {
            int max = Math.max(Utils.getRealWidth(this),
                    Utils.getRealHeight(this)) + 200;
            dimViewParams = new WindowManager
                    .LayoutParams(max, max, alertType,
                    (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS),
                    PixelFormat.TRANSPARENT);
            dimView.setAlpha(screenDimAmount);
        }

        wm.addView(dimView, dimViewParams);
    }

    private void setupReferenceView(int alertType) {
        topLeftView = new View(this);
        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                alertType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        topLeftParams.gravity = Gravity.START | Gravity.TOP;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
        topLeftParams.width = 0;
        topLeftParams.height = 0;
        wm.addView(topLeftView, topLeftParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        screenDimAmount = 0.0f;
        if (brightnessSlider != null) {
            int[] location = new int[2];
            brightnessSlider.getLocationOnScreen(location);
            int xPos = location[0];
            int yPos = location[1];
            prefs.edit()
                    .putInt(KEY_OVERLAY_X, xPos)
                    .putInt(KEY_OVERLAY_Y, yPos - statusbarHeight)
                    .apply();
            wm.removeView(brightnessSlider);
            wm.removeView(topLeftView);
            brightnessSlider = null;
            topLeftView = null;
        }
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(screenDimReceiver);
        if (dimView != null)
            wm.removeView(dimView);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getRawX();
            float y = event.getRawY();

            moving = false;

            int[] location = new int[2];
            brightnessSlider.getLocationOnScreen(location);
            originalXPos = location[0];
            originalYPos = location[1];

            offsetX = originalXPos - x;
            lastX = x;
            lastY = y;
            offsetY = originalYPos - y;

            translateHandler.removeCallbacks(translateRunnable);
            translateHandler.post(translateRunnable.set(1, 0));

            scaleHandler.removeCallbacks(scaleRunnable);
            scaleHandler.postDelayed(scaleRunnable, 600);

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

            float x = event.getRawX();
            float y = event.getRawY();
            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams) brightnessSlider.getLayoutParams();

            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);

            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }

            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);

            wm.updateViewLayout(brightnessSlider, params);
            moving = true;

            float movedBy = Math.abs(lastY - y);
            if (movedBy > BUTTON_TOUCH_SLOP || Math.abs(lastX - x) > BUTTON_TOUCH_SLOP) {
                brightnessChangeBy = (int) (movedBy / BRIGHTNESS_CHANGE_FACTOR);
                boolean brightnessUpNow = lastY - y >= 0;
                if (moveWasBrightness) {
                    brightnessUp = brightnessUpNow;
                    scaleHandler.removeCallbacks(scaleRunnable);
                    brightnessHandler.removeCallbacks(brightnessRunnable);
                    brightnessHandler.post(brightnessRunnable);
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams) brightnessSlider.getLayoutParams();
            scaleHandler.removeCallbacks(scaleRunnable);
            if (moveWasBrightness) {
                brightnessHandler.removeCallbacks(brightnessRunnable);
                params.y = originalYPos - statusbarHeight;
            } else {
                moveWasBrightness = true;
                scaleSlider(false);
            }
            int finalPos;
            if (event.getRawX() <= display.widthPixels / 2) {
                params.x = 0;
                finalPos = -(brightnessSlider.getWidth() / 2);
            } else {
                params.x = display.widthPixels - brightnessSlider.getWidth();
                finalPos = (brightnessSlider.getWidth() / 2);
            }

            translateHandler.removeCallbacks(translateRunnable);
            translateHandler.postDelayed(translateRunnable.set(imageAlpha, finalPos), 1000);


            if (moving) {
                wm.updateViewLayout(brightnessSlider, params);
                return true;
            }
            wm.updateViewLayout(brightnessSlider, params);
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setBrightnessCompat(int brightness) {
        if (brightness < 0)
            setBrightnessCompat(0);
        else if (brightness > 255)
            setBrightnessCompat(255);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(getApplicationContext())) {
                setBrightness(brightness);
            } else
                requestSettingsPermission();
        } else
            setBrightness(brightness);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestSettingsPermission() {
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private int getBrightness() {
        ContentResolver cResolver = getApplicationContext().getContentResolver();
        int brightness;
        try {
            brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
        return brightness;
    }

    private void setBrightness(int brightness) {
        ContentResolver cResolver = getApplicationContext().getContentResolver();
        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    private void scaleSlider(boolean scaleUp) {
        float factor;
        if (scaleUp) {
            factor = 1.5F;
            brightnessSlider.setImageResource(R.drawable.ic_overlay_brightness_move);
        } else {
            factor = 1;
            brightnessSlider.setImageResource(R.drawable.ic_overlay_brightness);
        }
        brightnessSlider.animate()
                .setDuration(250)
                .scaleY(factor)
                .scaleX(factor)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private class BrightnessRunnable implements Runnable {
        @Override
        public void run() {
            int newbr = getBrightness() + (brightnessUp ? brightnessChangeBy : -brightnessChangeBy);
            setBrightnessCompat(newbr);
            brightnessHandler.postDelayed(this, 250);
        }
    }

    private class ScaleRunnable implements Runnable {
        @Override
        public void run() {
            moveWasBrightness = false;
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(100);
            scaleSlider(true);
        }
    }

    private class TranslateRunnable implements Runnable {
        float alpha;
        float translateX;

        @Override
        public void run() {
            if (brightnessSlider != null)
                brightnessSlider.animate()
                        .alpha(alpha)
                        .setDuration(250)
                        .translationX(translateX)
                        .start();
        }

        TranslateRunnable set(float alpha, float translateX) {
            this.alpha = alpha;
            this.translateX = translateX;
            return this;
        }
    }

    private void setDimmerBrightness(float amount) {
        screenDimAmount = amount;
        if (dimView != null) {
            if (alertType == WindowManager.LayoutParams.TYPE_SYSTEM_ERROR)
                dimViewParams.dimAmount = screenDimAmount;
            else
                dimView.setAlpha(screenDimAmount);
            wm.updateViewLayout(dimView, dimViewParams);
        } else
            setupDimmerView();
    }

}
