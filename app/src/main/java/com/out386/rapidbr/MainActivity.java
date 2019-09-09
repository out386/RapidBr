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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.out386.rapidbr.services.overlay.BrightnessOverlayService;
import com.out386.rapidbr.settings.MainActivityListener;
import com.out386.rapidbr.settings.bottom.bcolour.OnButtonColourChangedListener;
import com.out386.rapidbr.settings.bottom.screenfilter.OnScreenFilterSettingsChangedListener;
import com.out386.rapidbr.settings.top.TopFragment;
import com.out386.rapidbr.utils.BoolUtils;

import java.lang.ref.WeakReference;

import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_IS_OVERLAY_RUNNING;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_OVERLAY_BUTTON_COLOUR;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_SCREEN_DIM_ENABLED;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_SCREEN_DIM_STATUS;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_SET_CLIENT_MESSENGER;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.MSG_UNSET_CLIENT_MESSENGER;
import static com.out386.rapidbr.services.overlay.BrightnessOverlayService.NOTIF_CHANNEL_ID;
import static com.out386.rapidbr.services.overlay.ServiceLauncher.sendMessageToBrightnessService;
import static com.out386.rapidbr.services.overlay.ServiceLauncher.toggleBrightnessService;
import static com.out386.rapidbr.utils.DimenUtils.getActionbarHeight;

public class MainActivity extends ThemeActivity implements MainActivityListener,
        OnButtonColourChangedListener, OnScreenFilterSettingsChangedListener {

    private static final String KEY_CURRENTLY_MAIN_FRAG = "CURRENTLY_MAIN_FRAG";
    private static final String KEY_TOP_FRAG = "KEY_TOP_FRAG";
    private boolean isCurrentlyMainFrag = true;
    private AppBarLayout appBarLayout;
    private MaterialButton startButton;
    private boolean isQueuedToggle;
    private BrightnessConnection brConnection;
    private TopFragment topFragment;
    private Messenger clientMessenger;
    private Messenger serviceMessenger;
    private SharedPreferences prefs;
    private boolean isBOSStarted;
    private boolean isBOSPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        createNotifChannel();
        setupViews();
        setTopFragment();
        if (savedInstanceState != null) {
            isCurrentlyMainFrag =
                    savedInstanceState.getBoolean(KEY_CURRENTLY_MAIN_FRAG, true);
            if (isCurrentlyMainFrag)
                onMainFragment();
            else
                onAltFragment();
        }

        if (isCurrentlyMainFrag)
            fixScrolling(false);
        else
            fixScrolling(true);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_CURRENTLY_MAIN_FRAG, isCurrentlyMainFrag);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    private void onBrServiceStatusChanged(boolean isStarted) {
        topFragment.setStatus(isStarted);
        String text;
        Drawable icon;
        if (isStarted) {
            text = getResources().getString(R.string.stop_app);
            icon = getDrawable(R.drawable.ic_notif_stop);
        } else {
            text = getResources().getString(R.string.start_app);
            icon = getDrawable(R.drawable.ic_notif_start);
        }
        startButton.setText(text);
        startButton.setIcon(icon);
    }

    private void onScreenFilterChanged(int percent) {
        topFragment.setFilter(percent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serviceMessenger != null)
            setClientMessenger();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceMessenger != null) {
            boolean result = sendMessageToBrightnessService(
                    serviceMessenger, MSG_UNSET_CLIENT_MESSENGER, 0, 0);
            if (!result)
                serviceMessenger = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindBrService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(brConnection);
    }

    private void setupViews() {
        startButton = findViewById(R.id.app_start_button);
        startButton.setOnClickListener(view -> {
            if (serviceMessenger != null) {
                toggleOverlay();
            } else {
                isQueuedToggle = true;
                bindBrService();
            }
        });
        setupToolbar();
        setupInsets();
    }

    private void toggleOverlay() {
        boolean result = toggleBrightnessService(getApplicationContext(), serviceMessenger, prefs);
        if (!result)
            serviceMessenger = null;
    }

    private void bindBrService() {
        if (brConnection == null)
            brConnection = new BrightnessConnection();
        bindService(
                new Intent(this, BrightnessOverlayService.class),
                brConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    private void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String CHANNEL_NAME = getString(R.string.notif_channel_name);
            final String CHANNEL_DESC = getString(R.string.notif_channel_desc);
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(false);
            channel.enableVibration(false);
            NotificationManager notificationManager =
                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
            if (notificationManager != null)
                notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupInsets() {
        View decorView = getWindow().getDecorView();
        appBarLayout = findViewById(R.id.app_bar_layout);
        FrameLayout topView = findViewById(R.id.topView);
        FrameLayout bottomView = findViewById(R.id.bottom_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        FrameLayout.LayoutParams topViewParams = (FrameLayout.LayoutParams) topView.getLayoutParams();
        ViewGroup.LayoutParams toolbarParams = toolbar.getLayoutParams();
        CoordinatorLayout.LayoutParams appBarParams =
                (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();

        decorView.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int leftInset = insets.getSystemWindowInsetLeft();
            int rightInset = insets.getSystemWindowInsetRight();
            int bottomInset = insets.getSystemWindowInsetBottom();
            int actionbarHeight = getActionbarHeight(this);

            toolbarParams.height = (actionbarHeight > -1 ? actionbarHeight : toolbarParams.height) +
                    topInset;

            CoordinatorLayout.LayoutParams appBarParamsNew =
                    new CoordinatorLayout.LayoutParams(appBarParams.width,
                            appBarParams.height + toolbarParams.height);
            topViewParams.setMargins(0, toolbarParams.height, 0, 0);

            appBarLayout.setLayoutParams(appBarParamsNew);
            toolbar.setLayoutParams(toolbarParams);
            topView.setLayoutParams(topViewParams);

            toolbar.setPadding(leftInset, topInset, rightInset, 0);
            topView.setPadding(leftInset, 0, rightInset, 0);
            bottomView.setPadding(leftInset, 0, rightInset, 0);
            decorView.setPadding(0, 0, 0, bottomInset);

            return insets.consumeSystemWindowInsets();
        });
    }

    private void fixScrolling(boolean finallyCollapsed) {
        // Without this, the bottom fragment won't scroll fully.
        appBarLayout.setExpanded(finallyCollapsed, false);
        new Handler()
                .postDelayed(() -> appBarLayout.setExpanded(!finallyCollapsed, false),
                        250
                );
    }

    private void setTopFragment() {
        if (topFragment == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentByTag(KEY_TOP_FRAG);
            if (fragment instanceof TopFragment) {
                topFragment = (TopFragment) fragment;
            } else {
                topFragment = new TopFragment();
                fragmentManager.beginTransaction()
                        .replace(R.id.topView, topFragment, KEY_TOP_FRAG)
                        .commit();
            }
        }

    }

    @Override
    public void onAltFragment() {
        isCurrentlyMainFrag = false;
        appBarLayout.setExpanded(false);
    }

    @Override
    public void onMainFragment() {
        isCurrentlyMainFrag = true;
        appBarLayout.setExpanded(true);
    }

    @Override
    public boolean getBOSRunning() {
        return isBOSPaused || isBOSStarted;
    }

    @Override
    public void onColourChanged(int colour) {
        boolean result = sendMessageToBrightnessService(
                serviceMessenger, MSG_OVERLAY_BUTTON_COLOUR, colour, 0);
        if (!result)
            serviceMessenger = null;
    }

    @Override
    public void onScreenFilterEnabledChanged(boolean isEnabled) {
        boolean result = sendMessageToBrightnessService(
                serviceMessenger, MSG_SCREEN_DIM_ENABLED, isEnabled ? 1 : -1, 0);
        if (!result)
            serviceMessenger = null;
    }

    private void setClientMessenger() {
        if (clientMessenger == null)
            clientMessenger = new Messenger(new ClientHandler(this));
        Message message =
                Message.obtain(null, MSG_SET_CLIENT_MESSENGER);
        message.replyTo = clientMessenger;
        try {
            serviceMessenger.send(message);
        } catch (RemoteException ignored) {
            // Dammit.
        }
    }

    private static class ClientHandler extends Handler {
        WeakReference<MainActivity> activity;

        ClientHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = activity.get();
            if (mainActivity == null)
                return;

            switch (msg.what) {
                case MSG_IS_OVERLAY_RUNNING:
                    boolean[] arg1 = BoolUtils.unpackBool(msg.arg1);
                    mainActivity.isBOSStarted = arg1[0];
                    mainActivity.isBOSPaused = arg1[1];
                    mainActivity.onBrServiceStatusChanged(arg1[0]);
                    mainActivity.onScreenFilterChanged(msg.arg2);
                    break;
                case MSG_SCREEN_DIM_STATUS:
                    mainActivity.onScreenFilterChanged(msg.arg1);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private class BrightnessConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            serviceMessenger = new Messenger(binder);
            setClientMessenger();
            // The service will send a status update after being bound, so not asking for status here
            if (isQueuedToggle) {
                isQueuedToggle = false;
                toggleOverlay();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceMessenger = null;
        }
    }
}
