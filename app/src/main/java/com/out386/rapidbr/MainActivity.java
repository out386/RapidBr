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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.out386.rapidbr.services.BrightnessOverlayService;
import com.out386.rapidbr.settings.OnNavigationListener;
import com.out386.rapidbr.settings.top.TopFragment;

import java.lang.ref.WeakReference;

import static com.out386.rapidbr.services.BrightnessOverlayService.DEF_OVERLAY_BUTTON_COLOUR;
import static com.out386.rapidbr.services.BrightnessOverlayService.KEY_SCREEN_DIM_AMOUNT;
import static com.out386.rapidbr.services.BrightnessOverlayService.MSG_IS_OVERLAY_RUNNING;
import static com.out386.rapidbr.services.BrightnessOverlayService.MSG_SET_CLIENT_MESSENGER;
import static com.out386.rapidbr.services.BrightnessOverlayService.MSG_TOGGLE_OVERLAY;
import static com.out386.rapidbr.services.BrightnessOverlayService.MSG_UNSET_CLIENT_MESSENGER;
import static com.out386.rapidbr.settings.bottom.bcolour.ButtonColourFragment.KEY_BR_ICON_COLOUR;

public class MainActivity extends AppCompatActivity implements OnNavigationListener {

    private static final String KEY_CURRENTLY_MAIN_FRAG = "CURRENTLY_MAIN_FRAG";
    private static final String KEY_TOP_FRAG = "KEY_TOP_FRAG";
    private boolean isCurrentlyMainFrag = true;
    private AppBarLayout appBarLayout;
    private Button startButton;
    private boolean isQueuedToggle;
    private BrightnessConnection brConnection;
    private TopFragment topFragment;
    private Messenger clientMessenger;
    private Messenger serviceMessenger;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
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

    private void onBrServiceStatusChanged(int status) {
        boolean isStarted = status != 0;
        topFragment.setStatus(isStarted);
        String text;
        if (isStarted)
            text = getResources().getString(R.string.stop_app);
        else
            text = getResources().getString(R.string.start_app);
        startButton.setText(text);
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
        if (serviceMessenger != null)
            sendMessage(MSG_UNSET_CLIENT_MESSENGER, 0, 0);
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
        setupInsets();
        setupToolbarText();
    }

    private void toggleOverlay() {
        int overlayButtonColour = prefs.getInt(KEY_BR_ICON_COLOUR, DEF_OVERLAY_BUTTON_COLOUR);
        int screenDimAmount = prefs.getInt(KEY_SCREEN_DIM_AMOUNT, 0);
        sendMessage(MSG_TOGGLE_OVERLAY, overlayButtonColour, screenDimAmount);
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

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        decorView.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int leftInset = insets.getSystemWindowInsetLeft();
            int rightInset = insets.getSystemWindowInsetRight();
            int bottomInset = insets.getSystemWindowInsetBottom();
            int actionbarHeight = getActionbarHeight();

            toolbarParams.height = (actionbarHeight > -1 ? actionbarHeight : toolbarParams.height) +
                    topInset;

            CoordinatorLayout.LayoutParams appBarParamsNew =
                    new CoordinatorLayout.LayoutParams(appBarParams.width,
                            appBarParams.height + toolbarParams.height);
            topViewParams.setMargins(0, toolbarParams.height, 0, 0);

            appBarLayout.setLayoutParams(appBarParamsNew);
            toolbar.setLayoutParams(toolbarParams);
            topView.setLayoutParams(topViewParams);

            toolbar.setPadding(leftInset, 0, rightInset, 0);
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

    private int getActionbarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return -1;
    }

    private void setupToolbarText() {
        TextView toolbarTV = findViewById(R.id.toolbarText);
        String appName = getString(R.string.app_name);
        int appName1Length = getString(R.string.app_name1).length();
        SpannableString toolbarString = new SpannableString(appName);
        toolbarString.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.colorAccent, getTheme())),
                0,
                appName1Length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarString.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.toolbarText2, getTheme())),
                appName1Length,
                appName.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarTV.setText(toolbarString);
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

    private void sendMessage(int what, int arg1, int arg2) {
        if (serviceMessenger != null) {
            try {
                serviceMessenger.send(Message.obtain(
                        null, what, arg1, arg2));
            } catch (RemoteException e) {
                serviceMessenger = null;
            }
        }
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
            if (mainActivity != null) {
                if (msg.what == MSG_IS_OVERLAY_RUNNING)
                    mainActivity.onBrServiceStatusChanged(msg.arg1);
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
