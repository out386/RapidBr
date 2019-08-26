package com.out386.rapidbr.settings.bottom.blacklist;

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

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.out386.rapidbr.R;
import com.out386.rapidbr.ThemeActivity;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistActivityListener;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerFragment;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerItem;
import com.out386.rapidbr.utils.GenericDialogFragment;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;
import static com.out386.rapidbr.utils.DimenUtils.getActionbarHeight;

public class BlacklistActivity extends ThemeActivity implements BlacklistActivityListener {

    private static final String BLACKLIST_FRAGMENT_TAG = "blacklistFragment";
    private static final String KEY_BLOCK_BACK = "isSaveNeeded";

    private boolean isSaveNeeded = false;
    private BlacklistFragment blacklistFragment;
    private GenericDialogFragment dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom);
        setContentView(R.layout.activity_blacklist);

        if (savedInstanceState == null) {
            blacklistFragment = new BlacklistFragment();
            changeFragment(blacklistFragment, BLACKLIST_FRAGMENT_TAG, false);
        } else {
            isSaveNeeded = savedInstanceState.getBoolean(KEY_BLOCK_BACK);
            blacklistFragment = (BlacklistFragment) getSupportFragmentManager()
                    .findFragmentByTag(BLACKLIST_FRAGMENT_TAG);
            if (blacklistFragment == null)
                blacklistFragment = new BlacklistFragment();
        }

        setupToolbarText();
        setupInsets();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_BLOCK_BACK, isSaveNeeded);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onShowPicker() {
        isSaveNeeded = false;
        changeFragment(new BlacklistPickerFragment(), null, true);
    }

    @Override
    public void onAppPicked(BlacklistPickerItem item) {
        if (blacklistFragment != null)
            blacklistFragment.onAppPicked(item);
        onBackPressed();
        isSaveNeeded = true;
    }

    @Override
    public void onAppChanged() {
        isSaveNeeded = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        int permissionCode = checkUsagePermission();
        if (permissionCode == 0 || permissionCode == -1)
            showPermissionDialog(permissionCode);
    }

    @Override
    public void onBackPressed() {
        if (!isSaveNeeded)
            super.onBackPressed();
        else {
            if (blacklistFragment != null)
                blacklistFragment.onSaveNeeded();
            isSaveNeeded = false; // Outside above "if" just in case blacklistFragment somehow became null.
            onBackPressed();    // Better to not save than get the user stuck
        }
    }

    private void setupToolbarText() {
        TextView toolbarTV = findViewById(R.id.blacklist_toolbarText);
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

    private void setupInsets() {
        View decorView = getWindow().getDecorView();
        Toolbar toolbar = findViewById(R.id.toolbar);
        ViewGroup.LayoutParams toolbarParams = toolbar.getLayoutParams();

        decorView.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int leftInset = insets.getSystemWindowInsetLeft();
            int rightInset = insets.getSystemWindowInsetRight();
            int bottomInset = insets.getSystemWindowInsetBottom();
            int actionbarHeight = getActionbarHeight(this);

            toolbarParams.height = (actionbarHeight > -1 ? actionbarHeight : toolbarParams.height) +
                    topInset;

            toolbar.setLayoutParams(toolbarParams);
            toolbar.setPadding(leftInset, 0, rightInset, 0);
            decorView.setPadding(leftInset, 0, rightInset, bottomInset);

            return insets.consumeSystemWindowInsets();
        });
    }

    private void changeFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                .beginTransaction();

        if (fragment instanceof BlacklistPickerFragment)
            fragmentTransaction
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right);
        // BlacklistAppsFragment is only used once, when the activity starts, so not adding animations for that.

        fragmentTransaction
                .replace(R.id.app_blacklist_frame, fragment, tag);
        if (addToBackStack)
            fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * @return 0 if this app does not have Usage access, 1 if it does, -1 if it can't be granted
     * that permission
     */
    private int checkUsagePermission() {
        Context context = getApplicationContext();
        PackageManager pm = context.getPackageManager();

        Intent launchIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        if (launchIntent.resolveActivity(pm) == null)
            return -1;

        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null)
            return -1;

        ApplicationInfo info;
        try {
            info = pm.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();    // Waaaaaaooow
            return -1;
        }

        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, info.uid, context.getPackageName());
        return mode == MODE_ALLOWED ? 1 : 0;
    }

    private void showPermissionDialog(int code) {
        FragmentManager manager = getSupportFragmentManager();

        if (dialog != null)
            dialog.dismiss();

        dialog = GenericDialogFragment.newInstance()
                .setDialogCancelable(false);

        if (code == 0) {
            dialog.setTitle(getString(R.string.sett_blacklist_no_usage_access_title))
                    .setMessage(getString(R.string.sett_blacklist_no_usage_access))
                    .setOnPositiveButtonTappedListener(() ->
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        } else if (code == -1) {
            dialog.setTitle(getString(R.string.sett_blacklist_usage_access_unsupported))
                    .setMessage(getString(R.string.sett_blacklist_usage_access_unsupported_title));
        }
        dialog.show(manager, null);
    }
}