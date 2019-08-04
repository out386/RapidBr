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


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.mikepenz.fastadapter_extensions.drag.ItemTouchCallback;
import com.mikepenz.fastadapter_extensions.drag.SimpleDragCallback;
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback;
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeDragCallback;
import com.out386.rapidbr.R;
import com.out386.rapidbr.settings.bottom.blacklist.io.LoadProfilesAppsRunnable;
import com.out386.rapidbr.settings.bottom.blacklist.io.WriteProfilesAppsRunnable;
import com.out386.rapidbr.settings.bottom.blacklist.picker.AppProfileActivityListener;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerItem;
import com.out386.rapidbr.utils.GenericDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.checkUnique;
import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.pickerToAppItem;
import static com.out386.rapidbr.utils.ViewUtils.animateView;

public class BlacklistFragment extends Fragment implements
        OnClickListener<AppProfilesAppsItem>, ItemTouchCallback,
        SimpleSwipeCallback.ItemSwipeCallback, AppProfileFragmentListener {

    public static final String FILE_APP_PROFILES_APPS_LIST = "profilesAppsList";
    private static final String KEY_NEW_APP_ITEM_LIST = "newAppItemList";
    private static final String KEY_LAYOUT_MANAGER_STATE = "layoutState";
    private static final String KEY_APP_HELP_SHOWN = "profilesHelpShown";
    private static boolean isWriting = false;

    private ItemAdapter<AppProfilesAppsItem> itemAdapter;
    private FastAdapter<AppProfilesAppsItem> fastAdapter;
    private RecyclerView recyclerView;
    private Button addButton;
    private LinearLayoutManager layoutManager;
    private Parcelable layoutManagerState;
    private CheckBox appBrightnessCheckbox;
    private TextView appBrightness;
    private TextView noApps;
    private SeekBar appBrightnessSeekbar;
    private LinearLayout appBrightnessRootHolder;
    private ExecutorService loadAppsExecutor;

    public BlacklistFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itemAdapter = new ItemAdapter<>();
        fastAdapter = FastAdapter.with(itemAdapter);
        fastAdapter
                .withSelectable(true)
                .withOnClickListener(this);
        loadAppsExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_blacklist, container, false);
        addButton = v.findViewById(R.id.blacklist_add_button);
        noApps = v.findViewById(R.id.blacklist_no_apps_text);
        recyclerView = v.findViewById(R.id.blacklist_recycler);
        layoutManager = new LinearLayoutManager(getContext());

        Drawable deleteDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
        int deleteColour = ContextCompat.getColor(getContext(), R.color.blacklistDelete);
        SimpleDragCallback touchCallback = new SimpleSwipeDragCallback(
                this,
                this,
                deleteDrawable,
                ItemTouchHelper.LEFT,
                deleteColour
        )
                .withBackgroundSwipeRight(deleteColour)
                .withLeaveBehindSwipeRight(deleteDrawable);
        touchCallback.setIsDragEnabled(false);

        ItemTouchHelper touchHelper = new ItemTouchHelper(touchCallback);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(fastAdapter);
        touchHelper.attachToRecyclerView(recyclerView);

        addButton.setOnClickListener(view ->
                // The delay is to let the ripple animation complete
                new Handler().postDelayed(() -> {
                    AppProfileActivityListener listener = ((AppProfileActivityListener) getActivity());
                    if (listener != null)
                        listener.onShowPicker();
                }, 150)
        );

        if (savedInstanceState != null) {
            layoutManagerState = savedInstanceState.getParcelable(KEY_LAYOUT_MANAGER_STATE);
            //noinspection unchecked
            ArrayList<AppProfilesAppsItem> allAppItems =
                    (ArrayList<AppProfilesAppsItem>) savedInstanceState
                            .getSerializable(KEY_NEW_APP_ITEM_LIST);
            fetchApps(allAppItems);
        } else if (itemAdapter.getAdapterItems() == null || itemAdapter.getAdapterItems().size() == 0)
            fetchApps(null);

        setupButtonScroll();
        return v;
    }

    private void setupButtonScroll() {
        int animTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        recyclerView.clearOnScrollListeners();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 5) {
                    addButton.animate()
                            .setDuration(animTime)
                            .translationY(0);
                } else if (dy > 5) {
                    addButton.animate()
                            .setDuration(animTime)
                            .translationY(addButton.getHeight() << 1);
                }
            }
        });
    }

    /**
     * Hides the RecyclerView and shows a TextView if there are no selected apps
     */
    private void showNoApps() {
        recyclerView.setVisibility(View.GONE);
        animateView(noApps, true);
    }

    /**
     * Shows the RecyclerView and hides a TextView if there are some selected apps
     */
    private void showApps() {
        recyclerView.setVisibility(View.VISIBLE);
        animateView(noApps, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(KEY_NEW_APP_ITEM_LIST,
                (ArrayList<AppProfilesAppsItem>) itemAdapter.getAdapterItems()
        );
        if (layoutManager != null)
            outState.putParcelable(KEY_LAYOUT_MANAGER_STATE, layoutManager.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onClick(View v, IAdapter<AppProfilesAppsItem> adapter, AppProfilesAppsItem item, int position) {
        setupDialog(position, item.getAppName(), item.getStartUnderburn(), item.getBrightness());
        return true;
    }

    /**
     * Clears the RecyclerView adapter and sets the parameter list to it. Can be called on a
     * background thread.
     *
     * @param apps The List of apps to show in the RecyclerView
     */
    private void setListData(List<AppProfilesAppsItem> apps) {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(() -> {
                itemAdapter.clear();
                if (apps != null && apps.size() > 0) {
                    showApps();
                    itemAdapter.add(apps);
                    if (layoutManagerState != null)
                        layoutManager.onRestoreInstanceState(layoutManagerState);   // Used to restore scroll position
                } else
                    showNoApps();
            });
        isWriting = false;
    }

    /**
     * Called by the Activity when an app is picked on the app picker fragment. Adds the item to the
     * RecyclerView adapter if it does not already exist.
     *
     * @param item The app item that was picked in the picker fragment
     */
    @Override
    public void onAppPicked(BlacklistPickerItem item) {
        Context context = getContext();
        if (context != null) {
            // Will always be true
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (!prefs.getBoolean(KEY_APP_HELP_SHOWN, false)) {
                prefs.edit()
                        .putBoolean(KEY_APP_HELP_SHOWN, true)
                        .apply();
                showProfilesHelp();
            }
        }
        AppProfilesAppsItem newItem = pickerToAppItem(item);
        if (checkUnique(itemAdapter.getAdapterItems(), newItem)) {
            itemAdapter.add(newItem);
            showApps();
        }
    }

    private void showProfilesHelp() {
        FragmentManager manager = getFragmentManager();
        if (manager == null)
            return; // Won't be hit
        GenericDialogFragment.newInstance()
                .setTitle(getString(R.string.sett_blacklist_help_title))
                .setMessage(getString(R.string.sett_blacklist_help_text))
                .show(manager, null);
    }

    /**
     * Manages loading a list app items on a background thread. Can read from saved data on disk.
     * Sets icons to the items, and calls {@link #setListData(List)}
     *
     * @param allAppItems If null, attempts to read the list from disk. Else sets icons to this
     *                    list.
     */
    private void fetchApps(ArrayList<AppProfilesAppsItem> allAppItems) {
        Context context = getContext();
        if (context == null || isWriting) {
            return;
        }

        LoadProfilesAppsRunnable loadAppsRunnable = new LoadProfilesAppsRunnable(
                context.getApplicationContext(), allAppItems, this::setListData
        );
        isWriting = true;
        loadAppsExecutor.submit(loadAppsRunnable);
    }

    /**
     * Saves the List in the itemAdapter to disk on a background thread
     */
    @Override
    public synchronized void onSaveNeeded() {
        ArrayList<AppProfilesAppsItem> allAppItems =
                (ArrayList<AppProfilesAppsItem>) itemAdapter.getAdapterItems();
        Context context = getContext();
        if (context == null)
            return;
        WriteProfilesAppsRunnable writeProfilesAppsRunnable = new WriteProfilesAppsRunnable(
                context, allAppItems
        );
        // This will return quickly, so just spawning a thread
        new Thread(writeProfilesAppsRunnable).start();
    }

    private void setupDialog(int position, String appName, boolean behaviour, float brightness) {
        Context context = getContext();
        if (context == null)
            return;

        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(appName)
                .customView(R.layout.blacklist_apps_behaviour_dialog_view, true)
                .positiveText(R.string.ok)
                .onPositive((dialog1, which) -> {
                    // Item pos == 0 means "Start" is selected
                    updateListItem(position,
                            appBrightnessCheckbox.isChecked() ? appBrightnessSeekbar.getProgress() : -1
                    );
                })
                .build();

        LinearLayout appBrightnessCheckboxHolder = null;
        View customView = dialog.getCustomView();
        if (customView != null) {
            appBrightnessCheckbox = customView.findViewById(R.id.app_brightness_checkbox);
            appBrightness = customView.findViewById(R.id.app_brightness);
            appBrightnessSeekbar = customView.findViewById(R.id.app_brightness_seekbar);
            appBrightnessCheckboxHolder = customView.findViewById(R.id.app_brightness_checkbox_holder);
            appBrightnessRootHolder = customView.findViewById(R.id.app_brightness_root_holder);
        } else
            return; // Won't happen

        appBrightnessCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            animateView(appBrightnessSeekbar, isChecked);
            if (isChecked)
                appBrightness.setText(String.format(getString(R.string.sett_blacklist_set_brightness), appBrightnessSeekbar.getProgress()));
            else
                appBrightness.setText(R.string.sett_blacklist_brightness_unchanged);
        });

        appBrightnessCheckboxHolder.setOnClickListener(v -> {
            appBrightnessCheckbox.setChecked(!appBrightnessCheckbox.isChecked());
        });

        appBrightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && appBrightnessCheckbox.isChecked())
                    appBrightness.setText(String.format(getString(R.string.sett_blacklist_set_brightness), progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (brightness > -1) {
            appBrightnessSeekbar.setProgress((int) (brightness / 2.55F));   // The progressbar is in percentages
            appBrightnessCheckbox.setChecked(true); // The checkbox listener will set the brightness text
        }

        dialog.show();
    }

    private void updateListItem(int position, float brightness) {
        if (brightness > -1)
            brightness = brightness * 2.55F;    // As the parameter is in percentages
        AppProfilesAppsItem item = itemAdapter.getAdapterItem(position);
        item.setAppBehaviour(false);
        // As brightness cannot be changed if behaviour == true
        item.setAppBrightness(brightness);
        itemAdapter.remove(position);
        itemAdapter.add(position, item);
        AppProfileActivityListener listener = ((AppProfileActivityListener) getActivity());
        if (listener != null)
            listener.onAppChanged();
    }


    // FastAdapter's Swipe to delete

    @Override
    public void itemTouchDropped(int oldPosition, int newPosition) {
    }

    @Override
    public void itemSwiped(int position, int direction) {
        itemAdapter.remove(position);
        if (itemAdapter.getAdapterItemCount() == 0)
            showNoApps();
        AppProfileActivityListener listener = ((AppProfileActivityListener) getActivity());
        if (listener != null)
            listener.onAppChanged();
    }

    @Override
    public boolean itemTouchOnMove(int oldPosition, int newPosition) {
        return false;
    }

}
