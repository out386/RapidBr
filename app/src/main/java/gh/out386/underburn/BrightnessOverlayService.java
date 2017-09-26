package gh.out386.underburn;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

/**
 * Created by J on 9/26/2017.
 */

public class BrightnessOverlayService extends Service implements View.OnTouchListener, View.OnClickListener {

    public static final String KEY_SB_HEIGHT = "statusbarHeight";
    private static final int BUTTON_TOUCH_SLOP = 30;
    private static final int BRIGHTNESS_CHANGE_FACTOR = 30;
    private View topLeftView;
    private Button overlayedButton;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;
    private WindowManager wm;
    private DisplayMetrics display;
    private boolean moveWasBrightness;
    private float lastX;
    private float lastY;
    private Handler brightnessHandler = new Handler();
    private BrightnessRunnable brightnessRunnable = new BrightnessRunnable();
    private boolean brightnessUp;
    private int statusbarHeight;
    private int brightnessChangeBy;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int alertType;
        statusbarHeight = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getInt(KEY_SB_HEIGHT, 1);

        overlayedButton = new Button(this);
        overlayedButton.setText("Overlay button");
        overlayedButton.setOnTouchListener(this);
        overlayedButton.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            alertType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            alertType = WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager
                .LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                alertType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;

        display = this.getResources().getDisplayMetrics();

        params.x = display.widthPixels - overlayedButton.getWidth();
        params.y = 300;
        wm.addView(overlayedButton, params);

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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayedButton != null) {
            wm.removeView(overlayedButton);
            wm.removeView(topLeftView);
            overlayedButton = null;
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
            int[] l = new int[2];
            overlayedButton.getLocationOnScreen(location);
            originalXPos = location[0];
            originalYPos = location[1];

            offsetX = originalXPos - x;
            lastX = x;
            lastY = y;
            offsetY = originalYPos - y;

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

            float x = event.getRawX();
            float y = event.getRawY();
            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams) overlayedButton.getLayoutParams();

            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);

            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }

            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);

            wm.updateViewLayout(overlayedButton, params);
            moving = true;

            if (Math.abs(lastX - x) <= BUTTON_TOUCH_SLOP) {
                float movedBy = Math.abs(lastY - y);
                if (movedBy > BUTTON_TOUCH_SLOP) {
                    brightnessChangeBy = (int) (movedBy / BRIGHTNESS_CHANGE_FACTOR);
                    boolean brightnessUpNow = lastY - y >= 0;
                    if (!moveWasBrightness || !(brightnessUpNow == brightnessUp)) {
                        brightnessUp = brightnessUpNow;
                        brightnessHandler.removeCallbacks(brightnessRunnable);
                        brightnessHandler.post(brightnessRunnable);
                        moveWasBrightness = true;
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (moveWasBrightness) {
                brightnessHandler.removeCallbacks(brightnessRunnable);
                moveWasBrightness = false;
                WindowManager.LayoutParams params =
                        (WindowManager.LayoutParams) overlayedButton.getLayoutParams();
                params.y = originalYPos - statusbarHeight;
                wm.updateViewLayout(overlayedButton, params);
            }
            if (moving) {
                if (event.getRawX() <= display.widthPixels / 2) {
                    WindowManager.LayoutParams params =
                            (WindowManager.LayoutParams) overlayedButton.getLayoutParams();
                    params.x = 0;
                    wm.updateViewLayout(overlayedButton, params);
                } else {
                    WindowManager.LayoutParams params =
                            (WindowManager.LayoutParams) overlayedButton.getLayoutParams();
                    params.x = display.widthPixels - overlayedButton.getWidth();

                    wm.updateViewLayout(overlayedButton, params);
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "Overlay button click event", Toast.LENGTH_SHORT).show();
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

    private class BrightnessRunnable implements Runnable {
        @Override
        public void run() {
            int newbr = getBrightness() + (brightnessUp ? brightnessChangeBy : -brightnessChangeBy);
            setBrightnessCompat(newbr);
            brightnessHandler.postDelayed(this, 50);
        }
    }
}
