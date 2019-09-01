package com.out386.rapidbr.services.overlay;

import android.annotation.TargetApi;
import android.app.Notification;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Vibrator;
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
import com.out386.rapidbr.services.blacklist.AppBlacklistService;
import com.out386.rapidbr.utils.DimenUtils;
import com.out386.rapidbr.utils.NotificationActivity;

import static com.out386.rapidbr.services.blacklist.AppBlacklistService.KEY_BLACKLIST_LIST;
import static com.out386.rapidbr.settings.bottom.blacklist.BlacklistFragment.KEY_BLACKLIST_BUNDLE;
import static com.out386.rapidbr.settings.bottom.blacklist.BlacklistFragment.KEY_BLACKLIST_ENABLED;
import static com.out386.rapidbr.utils.SizeUtils.dpToPx;

public class BrightnessOverlayService extends Service implements View.OnTouchListener {

    public final static String NOTIF_CHANNEL_ID = "channelStandard";
    public static final String KEY_OVERLAY_X = "overlayX";
    public static final String KEY_OVERLAY_Y = "overlayY";
    public static final String KEY_SCREEN_DIM_AMOUNT = "screenDimAmount";
    public static final String KEY_BR_ICON_COLOUR = "br_icon_colour";

    public static final int MSG_OVERLAY_BUTTON_COLOUR = 1;
    public static final int MSG_SCREEN_DIM_AMOUNT = 2;
    public static final int MSG_TOGGLE_OVERLAY = 3;
    public static final int MSG_SET_CLIENT_MESSENGER = 6;
    public static final int MSG_UNSET_CLIENT_MESSENGER = 7;
    public static final int MSG_IS_OVERLAY_RUNNING = 8;
    public static final int MSG_DUMMY = 10;
    public static final int DEF_OVERLAY_BUTTON_COLOUR = 0x0288D1;
    public static final float DEF_OVERLAY_BUTTON_ALPHA = 0.5f;
    public static final String ACTION_START = BuildConfig.APPLICATION_ID + ".START";
    public static final String ACTION_PAUSE = BuildConfig.APPLICATION_ID + ".PAUSE";
    static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP";
    private static final int NOTIFY_ID = 9906;

    private static final int BRIGHTNESS_CHANGE_FACTOR = 20;
    private static final int BRIGHTNESS_CHANGE_FACTOR_LOW = 40;
    public static float screenDimAmount = 0.0f;
    private static int buttonTouchSlop;
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
    private Notification notificationPause;
    private Notification notificationResume;
    private NotificationManager notificationManager;
    private Messenger serviceMessenger;
    private Messenger clientMessenger;
    private ButtonAnim buttonAnim;

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (ACTION_START.equals(i.getAction())) {
            setGlobals(i);

            // See comment in startOverlay
            // Using Bundle instead of boolean because it will also have the apps list in the future
            Bundle b = i.getBundleExtra(KEY_BLACKLIST_BUNDLE);
            startOverlay(b);
        } else if (ACTION_PAUSE.equals(i.getAction())) {
            pauseOverlay(true);
        } else if (ACTION_STOP.equals(i.getAction())) {
            stopOverlay();
        }

        return START_NOT_STICKY;
    }

    private void setGlobals(Intent i) {
        int buttonColourTemp = i.getIntExtra(KEY_BR_ICON_COLOUR, DEF_OVERLAY_BUTTON_COLOUR);
        float screenDimAmountTemp = i.getIntExtra(KEY_SCREEN_DIM_AMOUNT, 0) / 100f;

        // Set new values only if there are no values currently set, or if the intent has new values
        if (buttonColour == 0 || buttonColourTemp != DEF_OVERLAY_BUTTON_COLOUR)
            buttonColour = buttonColourTemp;
        if (screenDimAmount == 0.0f || screenDimAmountTemp != 0.0f)
            screenDimAmount = screenDimAmountTemp;
    }

    @Override
    public IBinder onBind(Intent intent) {
        serviceMessenger = new Messenger(new ServiceHandler());
        return serviceMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        buttonTouchSlop = dpToPx(getApplicationContext(), 15);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        prefs = getSharedPreferences("brightnessPrefs", MODE_PRIVATE);

        display = this.getResources().getDisplayMetrics();
        // As there's a new type in O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            alertType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        else
            alertType = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
    }

    private void startOverlay(Bundle settings) {
        // TODO: Check whether adaptive brightness was enabled
        initialSliderX = prefs.getInt(KEY_OVERLAY_X, 0);
        initialSliderY = prefs.getInt(KEY_OVERLAY_Y, 300);
        isOverlayRunning = true;
        setupBrightnessButton(alertType);
        setupReferenceView(alertType);
        setDimmerBrightness();
        buttonAnim.hideButtonDelayed();
        sendIsRunning();
        startService(new Intent(this, BrightnessOverlayService.class));

        /*
         * Managing ABS here instead of from outside this service to keep ABS in sync with this
         * service better, as this service can be started in various ways, from multiple places.
         */
        /*
         * ABS is not stopped when this service is paused. So if this method is called from
         * onStartCommand without a bundle, while ABS is enabled, it won't be a problem, as
         * ABS will already be running, started when this method was first called, either by a
         * Message, or from onStartCommand with an Intent with all values set.
         */
        if (settings != null) {
            boolean isABSEnabled = settings.getBoolean(KEY_BLACKLIST_ENABLED);
            if (isABSEnabled) {
                Intent startIntent = new Intent(this, AppBlacklistService.class);
                startIntent
                        .putExtra(KEY_BLACKLIST_LIST, settings.getSerializable(KEY_BLACKLIST_LIST));
                startService(startIntent);
            }
        }

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
        if (isForPause) {
            if (notificationResume == null)
                notificationResume = getResumeNotification();
            sendNotification(notificationResume);
        }
    }

    private void stopOverlay() {
        pauseOverlay(false);
        stopService(new Intent(this, AppBlacklistService.class));
        stopForeground(true);
        stopSelf();
    }

    private void setButtonColour() {
        if (brightnessSlider != null) {
            if (buttonColour == 0)
                buttonColour = DEF_OVERLAY_BUTTON_COLOUR;
            ColorStateList csl = ColorStateList.valueOf(buttonColour).withAlpha(0xFF);
            brightnessSlider.setImageTintList(csl);
        }
        if (buttonAnim != null)
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
        setButtonColour();

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

    private void toggleOverlay(int colour, int dim, Bundle settings) {
        buttonColour = colour;
        screenDimAmount = dim / 100f;
        if (isOverlayRunning)
            stopOverlay();
        else
            startOverlay(settings);
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
        stopService(new Intent(this, AppBlacklistService.class));
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
            if (movedBy > buttonTouchSlop || Math.abs(lastX - x) > buttonTouchSlop) {
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
        if (notificationPause == null)
            notificationPause = getPauseNotification();
        sendNotification(notificationPause);
        startForeground(NOTIFY_ID, notificationPause);
    }

    private Notification getPauseNotification() {
        NotificationCompat.Builder notificationBuilderPause =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID);
        Intent notificationIntent =
                new Intent(getApplicationContext(), NotificationActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notificationBuilderPause
                .setSound(null)
                .setVibrate(null)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setContentTitle(getString(R.string.notif_running))
                .setContentText(getString(R.string.notif_subtext))
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setTicker(getString(R.string.notif_running))
                .addAction(
                        R.drawable.ic_notif_pause, getString(R.string.notif_pause),
                        buildPendingIntent(ACTION_PAUSE))
                .addAction(
                        R.drawable.ic_notif_stop, getString(R.string.notif_stop),
                        buildPendingIntent(ACTION_STOP));

        return notificationBuilderPause.build();
    }

    private Notification getResumeNotification() {
        NotificationCompat.Builder notificationBuilderPause =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID);
        Intent notificationIntent =
                new Intent(getApplicationContext(), NotificationActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notificationBuilderPause
                .setSound(null)
                .setVibrate(null)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent)
                .setContentTitle(getString(R.string.notif_paused))
                .setContentText(getString(R.string.notif_subtext))
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                .setTicker(getString(R.string.notif_paused))
                .addAction(R.drawable.ic_notif_start, getString(R.string.notif_resume),
                        buildPendingIntent(ACTION_START))
                .addAction(
                        R.drawable.ic_notif_stop, getString(R.string.notif_stop),
                        buildPendingIntent(ACTION_STOP));

        return notificationBuilderPause.build();
    }

    private void sendNotification(Notification notification) {
        if (notificationManager == null)
            notificationManager =
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        notificationManager.notify(NOTIFY_ID, notification);

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
                    setButtonColour();
                    break;
                case MSG_SCREEN_DIM_AMOUNT:
                    screenDimAmount = msg.arg1;
                    setDimmerBrightness();
                    break;
                case MSG_TOGGLE_OVERLAY:
                    Bundle bundle = null;
                    if (msg.obj instanceof Bundle)
                        bundle = (Bundle) msg.obj;
                    toggleOverlay(msg.arg1, msg.arg2, bundle);
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
