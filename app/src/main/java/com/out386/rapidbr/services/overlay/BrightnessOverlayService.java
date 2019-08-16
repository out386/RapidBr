package com.out386.rapidbr.services.overlay;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.out386.rapidbr.BuildConfig;
import com.out386.rapidbr.R;
import com.out386.rapidbr.utils.DimenUtils;
import com.out386.rapidbr.utils.NotificationActivity;

import static com.out386.rapidbr.settings.bottom.bcolour.ButtonColourFragment.KEY_BR_ICON_COLOUR;

public class BrightnessOverlayService extends Service implements View.OnTouchListener {

    public static final String KEY_OVERLAY_X = "overlayX";
    public static final String KEY_OVERLAY_Y = "overlayY";
    public static final String KEY_SCREEN_DIM_AMOUNT = "screenDimAmount";

    public static final int MSG_OVERLAY_BUTTON_COLOUR = 1;
    public static final int MSG_SCREEN_DIM_AMOUNT = 2;
    public static final int MSG_TOGGLE_OVERLAY = 3;
    public static final int MSG_SET_CLIENT_MESSENGER = 6;
    public static final int MSG_UNSET_CLIENT_MESSENGER = 7;
    public static final int MSG_IS_OVERLAY_RUNNING = 8;
    public static final int DEF_OVERLAY_BUTTON_COLOUR = 0x0288D1;
    public static final float DEF_OVERLAY_BUTTON_ALPHA = 0.5f;
    static final String ACTION_START = BuildConfig.APPLICATION_ID + ".START";
    static final String ACTION_PAUSE = BuildConfig.APPLICATION_ID + ".PAUSE";
    static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP";
    private static final int NOTIFY_ID = 9906;

    private static final int BUTTON_TOUCH_SLOP = 15;
    private static final int BRIGHTNESS_CHANGE_FACTOR = 20;
    private static final int BRIGHTNESS_CHANGE_FACTOR_LOW = 40;
    public static float screenDimAmount = 0.0f;
    boolean moveWasBrightness = true;
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
    private boolean isBrightnessHandlerActive = false;
    private float lastX;
    private float lastY;
    private Handler brightnessHandler = new Handler();
    private BrightnessRunnable brightnessRunnable = new BrightnessRunnable();
    private boolean brightnessUp;
    private float brightnessMovedBy;
    private SharedPreferences prefs;
    private int alertType;
    private int initialSliderX;
    private int initialSliderY;
    private int buttonColour;
    private boolean isOverlayRunning;
    private NotificationCompat.Builder notificationBuilder;
    private Messenger serviceMessenger;
    private Messenger clientMessenger;
    private ButtonAnim buttonAnim;

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (ACTION_START.equals(i.getAction())) {
            setGlobals(i);
            startOverlay();
        } else if (ACTION_PAUSE.equals(i.getAction())) {
            pauseOverlay(true);
        } else if (ACTION_STOP.equals(i.getAction())) {
            stopOverlay();
        }

        return START_NOT_STICKY;
    }

    private void setGlobals(Intent i) {
        buttonColour = i.getIntExtra(KEY_BR_ICON_COLOUR, DEF_OVERLAY_BUTTON_COLOUR);
        screenDimAmount = i.getIntExtra(KEY_SCREEN_DIM_AMOUNT, 0) / 100f;
    }

    @Override
    public IBinder onBind(Intent intent) {
        serviceMessenger = new Messenger(new ServiceHandler());
        return serviceMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        display = this.getResources().getDisplayMetrics();
        // As there's a new type in O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            alertType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            alertType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        initialSliderX = prefs.getInt(KEY_OVERLAY_X, 0);
        initialSliderY = prefs.getInt(KEY_OVERLAY_Y, 300);
    }

    private void startOverlay() {
        isOverlayRunning = true;
        setupBrightnessButton(alertType);
        setupReferenceView(alertType);
        setDimmerBrightness();
        buttonAnim.hideButtonDelayed();
        sendIsRunning();
        startService(new Intent(this, BrightnessOverlayService.class));
        foregroundify();
    }

    private void pauseOverlay(boolean isForPause) {
        isOverlayRunning = false;
        screenDimAmount = 0.0f;
        if (brightnessSlider != null) {
            int[] location = new int[2];
            brightnessSlider.getLocationOnScreen(location);
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);
            int xPos = location[0];
            int yPos = location[1];
            // TODO: yPos will get changed in action down, but if service is shut down before action up, it will not be what it should. Fix this.
            prefs.edit()
                    .putInt(KEY_OVERLAY_X, xPos)
                    .putInt(KEY_OVERLAY_Y, yPos - topLeftLocationOnScreen[1])
                    .apply();
            wm.removeView(brightnessSlider);
            wm.removeView(topLeftView);
            brightnessSlider = null;
            topLeftView = null;
            if (buttonAnim != null) {
                buttonAnim.shutdown();
                buttonAnim = null;
            }
        }
        if (dimView != null) {
            wm.removeView(dimView);
            dimView = null;
        }
        if (brightnessHandler != null && brightnessRunnable != null)
            brightnessHandler.removeCallbacksAndMessages(null);

        sendIsRunning();
        if (isForPause)
            setNotifActions();
    }

    private void stopOverlay() {
        pauseOverlay(false);
        stopForeground(true);
        stopSelf();
    }

    private void setButtonColour(boolean isAfterSetup) {
        if (buttonColour == 0)
            buttonColour = DEF_OVERLAY_BUTTON_COLOUR;
        ColorStateList csl = ColorStateList.valueOf(buttonColour).withAlpha(0xFF);
        brightnessSlider.setImageTintList(csl);
        if (isAfterSetup)
            buttonAnim.peekButton();
    }

    private void setupBrightnessButton(int alertType) {
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (64 * scale + 0.5f);

        brightnessSlider = new ImageView(this);
        brightnessSlider.setScaleType(ImageView.ScaleType.FIT_XY);
        brightnessSlider.setImageResource(R.drawable.ic_overlay_brightness);
        brightnessSlider.setImageTintMode(PorterDuff.Mode.SRC_ATOP);
        brightnessSlider.setOnTouchListener(this);
        setButtonColour(false);

        WindowManager.LayoutParams params = new WindowManager
                .LayoutParams(pixels,
                pixels,
                alertType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.START | Gravity.TOP;

        params.x = initialSliderX;
        params.y = initialSliderY;
        wm.addView(brightnessSlider, params);
        buttonAnim = new ButtonAnim(brightnessSlider, display,
                (Vibrator) getSystemService(Context.VIBRATOR_SERVICE), this);
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
            int max = Math.max(DimenUtils.getRealWidth(this),
                    DimenUtils.getRealHeight(this)) + 200;
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

    private void toggleOverlay(int colour, int dim) {
        buttonColour = colour;
        screenDimAmount = dim / 100f;
        if (isOverlayRunning)
            stopOverlay();
        else
            startOverlay();
    }

    private void sendIsRunning() {
        if (clientMessenger != null) {
            Message message = Message.obtain(null, MSG_IS_OVERLAY_RUNNING,
                    isOverlayRunning ? 1 : 0, 0);
            try {
                clientMessenger.send(message);
            } catch (RemoteException e) {
                clientMessenger = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        pauseOverlay(false);
        super.onDestroy();
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

            buttonAnim.showButton();
            buttonAnim.scaleUpButtonDelayed();


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

            moving = true;

            float movedBy = Math.abs(lastY - y);
            if (movedBy > BUTTON_TOUCH_SLOP || Math.abs(lastX - x) > BUTTON_TOUCH_SLOP) {
                brightnessMovedBy = movedBy;

                boolean brightnessUpNow = lastY - y >= 0;
                if (moveWasBrightness) {
                    // Comments are overrated.
                    if (originalXPos <= display.widthPixels / 2) {
                        brightnessSlider.setRotation(-(originalYPos - newY) / 2f);
                    } else {
                        brightnessSlider.setRotation((originalYPos - newY) / 2f);
                    }
                    brightnessUp = brightnessUpNow;
                    buttonAnim.cancelScale();
                    if (!isBrightnessHandlerActive) {
                        isBrightnessHandlerActive = true;
                        brightnessHandler.post(brightnessRunnable);
                    }
                } else {
                    params.x = newX - (topLeftLocationOnScreen[0]);
                }
            }
            params.y = newY - (topLeftLocationOnScreen[1]);

            wm.updateViewLayout(brightnessSlider, params);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float xToSnap;
            brightnessSlider.setRotation(0);
            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams) brightnessSlider.getLayoutParams();
            buttonAnim.cancelScale();

            if (moveWasBrightness) {
                xToSnap = originalXPos;
                int[] topLeftLocationOnScreen = new int[2];
                topLeftView.getLocationOnScreen(topLeftLocationOnScreen);
                isBrightnessHandlerActive = false;
                brightnessHandler.removeCallbacks(brightnessRunnable);
                params.y = originalYPos - topLeftLocationOnScreen[1];
            } else {
                xToSnap = event.getRawX();
                moveWasBrightness = true;
                buttonAnim.scaleSlider(false);
            }

            if (xToSnap <= display.widthPixels / 2)
                params.x = 0;
            else
                params.x = display.widthPixels - brightnessSlider.getWidth();

            buttonAnim.hideButtonDelayed();

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

    private void setDimmerBrightness() {
        if (screenDimAmount > 0) {
            if (dimView != null) {
                if (alertType == WindowManager.LayoutParams.TYPE_SYSTEM_ERROR)
                    dimViewParams.dimAmount = screenDimAmount;
                else
                    dimView.setAlpha(screenDimAmount);
                wm.updateViewLayout(dimView, dimViewParams);
            } else
                setupDimmerView();
        } else {
            if (dimView != null) {
                wm.removeView(dimView);
                dimView = null;
            }
        }
    }

    private void foregroundify() {
        final String CHANNEL_ID = "channelStandard";
        if (notificationBuilder == null)
            notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID);
        else
            notificationBuilder.mActions.clear();

        Intent notificationIntent = new Intent(getApplicationContext(), NotificationActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder
                .setSound(null)
                .setVibrate(null)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setContentTitle(getString(R.string.notif_running))
                .setContentText(getString(R.string.notif_subtext))
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setTicker(getString(R.string.notif_running));
        setNotifActions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String CHANNEL_NAME = getString(R.string.notif_channel_name);
            final String CHANNEL_DESC = getString(R.string.notif_channel_desc);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(false);
            channel.enableVibration(false);
            NotificationManager notificationManager =
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }

        startForeground(NOTIFY_ID, notificationBuilder.build());
    }

    private void setNotifActions() {
        if (notificationBuilder == null)
            return;

        notificationBuilder.mActions.clear();
        if (isOverlayRunning)
            notificationBuilder
                    .addAction(R.drawable.ic_notif_pause,
                            getString(R.string.notif_pause),
                            buildPendingIntent(ACTION_PAUSE))
                    .setContentTitle(getString(R.string.notif_running));
        else
            notificationBuilder
                    .addAction(R.drawable.ic_notif_start,
                            getString(R.string.notif_resume),
                            buildPendingIntent(ACTION_START))
                    .setContentTitle(getString(R.string.notif_paused));
        notificationBuilder
                .addAction(R.drawable.ic_notif_stop,
                        getString(R.string.notif_stop),
                        buildPendingIntent(ACTION_STOP));

        NotificationManager notificationManager =
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        if (notificationManager != null)
            notificationManager.notify(NOTIFY_ID, notificationBuilder.build());

    }

    private PendingIntent buildPendingIntent(String action) {
        Intent i = new Intent(this, getClass());
        i.setAction(action);
        return PendingIntent.getService(this, 0, i, 0);
    }

    private class BrightnessRunnable implements Runnable {
        @Override
        public void run() {
            int currentBrightness = getBrightness();
            int brightnessChangeDelay;
            int brightnessChangeBy;
            if (currentBrightness <= 25) {   // Because screen brightness does not change linearly in most devices.
                brightnessChangeBy = (int) (brightnessMovedBy / BRIGHTNESS_CHANGE_FACTOR_LOW);
                brightnessChangeDelay = 250;
            } else {
                brightnessChangeDelay = 150;
                brightnessChangeBy = (int) (brightnessMovedBy / BRIGHTNESS_CHANGE_FACTOR);
            }

            brightnessChangeBy = brightnessChangeBy == 0 ? 1 : brightnessChangeBy;  // Because a 0 change makes it look like the slider got stuck
            int newbr = currentBrightness + (brightnessUp ? brightnessChangeBy : -brightnessChangeBy);
            setBrightnessCompat(newbr);
            brightnessHandler.postDelayed(this, brightnessChangeDelay);
        }
    }

    private class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OVERLAY_BUTTON_COLOUR:
                    buttonColour = msg.arg1;
                    setButtonColour(true);
                    break;
                case MSG_SCREEN_DIM_AMOUNT:
                    screenDimAmount = msg.arg1;
                    setDimmerBrightness();
                    break;
                case MSG_TOGGLE_OVERLAY:
                    toggleOverlay(msg.arg1, msg.arg2);
                    break;
                case MSG_SET_CLIENT_MESSENGER:
                    clientMessenger = msg.replyTo;
                    sendIsRunning();
                    break;
                case MSG_UNSET_CLIENT_MESSENGER:
                    clientMessenger = null;
                    break;
                case MSG_IS_OVERLAY_RUNNING:
                    sendIsRunning();
                    break;
            }
            super.handleMessage(msg);
        }
    }

}
