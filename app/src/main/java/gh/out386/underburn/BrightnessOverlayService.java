package gh.out386.underburn;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class BrightnessOverlayService extends Service implements View.OnTouchListener {

    public static final String KEY_SB_HEIGHT = "statusbarHeight";
    public static final String KEY_OVERLAY_X = "overlayX";
    public static final String KEY_OVERLAY_Y = "overlayY";

    private static final int BUTTON_TOUCH_SLOP = 15;
    private static final int BRIGHTNESS_CHANGE_FACTOR = 30;
    private View topLeftView;
    private ImageView brightnessSlider;
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int alertType;
        prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        statusbarHeight = prefs
                .getInt(KEY_SB_HEIGHT, 1);

        brightnessSlider = new ImageView(this);
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (64 * scale + 0.5f);
        brightnessSlider.setScaleType(ImageView.ScaleType.FIT_XY);
        brightnessSlider.setImageResource(R.drawable.ic_overlay_brightness);
        brightnessSlider.setOnTouchListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            alertType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            alertType = WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager
                .LayoutParams(pixels,
                pixels,
                alertType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;

        display = this.getResources().getDisplayMetrics();

        int sliderX = prefs.getInt(KEY_OVERLAY_X, 0);
        params.x = sliderX;
        params.y = prefs.getInt(KEY_OVERLAY_Y, 300);
        wm.addView(brightnessSlider, params);

        topLeftView = new View(this);
        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                alertType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        topLeftParams.gravity = Gravity.START | Gravity.TOP;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
        topLeftParams.width = 0;
        topLeftParams.height = 0;
        wm.addView(topLeftView, topLeftParams);

        // Handler instead of startDelay to prevent slider width from being 0
        new Handler().postDelayed(() -> {
            if (brightnessSlider != null)
                brightnessSlider.animate()
                        .translationX(
                                (brightnessSlider.getWidth() / 2F) * (sliderX <= 10 ? -1 : 1)) //Move button left or right
                        .setDuration(500)
                        .alpha(0.5F)
                        .start();
        }, 1000);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
            translateHandler.postDelayed(translateRunnable.set(0.5F, finalPos), 1000);


            if (moving) {
                wm.updateViewLayout(brightnessSlider, params);
                return true;
            }
            wm.updateViewLayout(brightnessSlider, params);
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void setBrightnessCompat(int brightness) {
        if (brightness < 0)
            setBrightnessCompat(0);
        else if (brightness > 255)
            setBrightnessCompat(255);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(getApplicationContext()))
                setBrightness(brightness);
            else
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

    public int getBrightness() {
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
            brightnessHandler.postDelayed(this, 150);
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
}
