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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import com.out386.rapidbr.settings.bottom.blacklist.io.BlacklistAppsStore;
import com.out386.rapidbr.settings.bottom.blacklist.io.BlacklistAppsStore.OnBlacklistReadListener;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistActivityListener;
import com.out386.rapidbr.settings.bottom.blacklist.picker.BlacklistPickerItem;
import com.out386.rapidbr.settings.bottom.views.SwitchItem;

import java.util.ArrayList;
import java.util.List;

import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.checkUnique;
import static com.out386.rapidbr.settings.bottom.blacklist.PackageUtils.pickerToAppItem;
import static com.out386.rapidbr.utils.ViewUtils.animateView;

public class BlacklistFragment extends Fragment implements
        OnClickListener<BlacklistAppsItem>, ItemTouchCallback,
        SimpleSwipeCallback.ItemSwipeCallback, BlacklistFragmentListener, OnBlacklistReadListener {

    private static final String KEY_NEW_APP_ITEM_LIST = "newAppItemList";
    private static final String KEY_LAYOUT_MANAGER_STATE = "layoutState";
    public static final String KEY_BLACKLIST_ENABLED = "blacklistEnabled";
    public static final String KEY_BLACKLIST_BUNDLE = "blacklistBundle";

    private ItemAdapter<BlacklistAppsItem> itemAdapter;
    private FastAdapter<BlacklistAppsItem> fastAdapter;
    private RecyclerView recyclerView;
    private Button addButton;
    private LinearLayoutManager layoutManager;
    private Parcelable layoutManagerState;
    private TextView appBrightness;
    private TextView noApps;
    private SeekBar appBrightnessSeekbar;
    private SwitchItem enableSwitch;
    private BlacklistAppsStore blacklistAppsStore;
    private Handler mainHandler;
    private SharedPreferences prefs;

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
        blacklistAppsStore = BlacklistAppsStore.getInstance(requireContext());
        mainHandler = new Handler();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_blacklist, container, false);
        addButton = v.findViewById(R.id.blacklist_add_button);
        enableSwitch = v.findViewById(R.id.blacklist_enable_switch);
        noApps = v.findViewById(R.id.blacklist_no_apps_text);
        recyclerView = v.findViewById(R.id.blacklist_recycler);
        layoutManager = new LinearLayoutManager(getContext());
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        Drawable deleteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        int deleteColour = ContextCompat.getColor(context, R.color.blacklistDelete);
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

        addButton.setOnClickListener(v ->
                // The delay is to let the ripple animation complete
                new Handler().postDelayed(() -> {
                    BlacklistActivityListener listener = ((BlacklistActivityListener) getActivity());
                    if (listener != null)
                        listener.onShowPicker();
                }, 200)
        );

        enableSwitch.setChecked(prefs.getBoolean(KEY_BLACKLIST_ENABLED, false));
        enableSwitch.setOnCheckedChangeListener(isChecked ->
                prefs.edit()
                        .putBoolean(KEY_BLACKLIST_ENABLED, isChecked)
                        .apply()
        );

        if (savedInstanceState != null) {
            layoutManagerState = savedInstanceState.getParcelable(KEY_LAYOUT_MANAGER_STATE);
            //noinspection unchecked
            ArrayList<BlacklistAppsItem> allAppItems =
                    (ArrayList<BlacklistAppsItem>) savedInstanceState
                            .getSerializable(KEY_NEW_APP_ITEM_LIST);
            blacklistAppsStore.read(allAppItems, this);
        } else if (itemAdapter.getAdapterItems() == null || itemAdapter.getAdapterItems().size() == 0)
            blacklistAppsStore.read(null, this);

        setupButtonScroll();
    }

    private void setupButtonScroll() {
        int animTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        recyclerView.clearOnScrollListeners();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 1) {
                    addButton.animate()
                            .setDuration(animTime)
                            .translationY(0);
                } else if (dy > 1) {
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
        noApps.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the RecyclerView and hides a TextView if there are some selected apps
     */
    private void showApps() {
        recyclerView.setVisibility(View.VISIBLE);
        noApps.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(KEY_NEW_APP_ITEM_LIST,
                (ArrayList<BlacklistAppsItem>) itemAdapter.getAdapterItems()
        );
        if (layoutManager != null)
            outState.putParcelable(KEY_LAYOUT_MANAGER_STATE, layoutManager.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onClick(View v, IAdapter<BlacklistAppsItem> adapter, BlacklistAppsItem item, int position) {
        setupDialog(position, item.getAppName(), item.getBrightness());
        return true;
    }

    /**
     * Clears the RecyclerView adapter and sets the parameter list to it.
     *
     * @param apps The List of apps to show in the RecyclerView
     */
    @Override
    public void onBlacklistRead(ArrayList<BlacklistAppsItem> apps) {
        mainHandler.post(() -> {
            itemAdapter.clear();
            if (apps != null && apps.size() > 0) {
                showApps();
                itemAdapter.add(apps);
                recyclerView.setVisibility(View.VISIBLE);
                if (layoutManagerState != null)
                    layoutManager.onRestoreInstanceState(layoutManagerState);   // Used to restore scroll position
            } else
                showNoApps();
        });
    }

    /**
     * Called by the Activity when an app is picked on the app picker fragment. Adds the item to the
     * RecyclerView adapter if it does not already exist.
     *
     * @param item The app item that was picked in the picker fragment
     */
    @Override
    public void onAppPicked(BlacklistPickerItem item) {
        BlacklistAppsItem newItem = pickerToAppItem(item);
        if (checkUnique(itemAdapter.getAdapterItems(), newItem)) {
            itemAdapter.add(newItem);
            showApps();
        }
    }

    /**
     * Saves the List in the itemAdapter to disk on a background thread
     */
    @Override
    public synchronized void onSaveNeeded() {
        ArrayList<BlacklistAppsItem> allAppItems =
                (ArrayList<BlacklistAppsItem>) itemAdapter.getAdapterItems();
        Context context = getContext();
        if (context == null)
            return;
        blacklistAppsStore.write(allAppItems);
    }

    private void setupDialog(int position, String appName, float brightness) {
        Context context = getContext();
        if (context == null)
            return;

        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(appName)
                .customView(R.layout.blacklist_apps_behaviour_dialog_view, true)
                .positiveText(R.string.ok)
                .onPositive((dialog1, which) -> {
                    View v = dialog1.getCustomView();
                    if (v == null)
                        return;
                    CheckBox checkBox = v.findViewById(R.id.app_brightness_checkbox);
                    updateListItem(position,
                            checkBox.isChecked() ? appBrightnessSeekbar.getProgress() : -1
                    );
                })
                .build();

        LinearLayout appBrightnessCheckboxHolder;
        View customView = dialog.getCustomView();

        if (customView == null)
            return; // Won't happen

        CheckBox appBrightnessCheckbox = customView.findViewById(R.id.app_brightness_checkbox);
        appBrightness = customView.findViewById(R.id.app_brightness);
        appBrightnessSeekbar = customView.findViewById(R.id.app_brightness_seekbar);
        appBrightnessCheckboxHolder = customView.findViewById(R.id.app_brightness_checkbox_holder);

        appBrightnessCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            animateView(appBrightnessSeekbar, isChecked);
            if (isChecked)
                appBrightness.setText(String.format(getString(R.string.sett_blacklist_set_brightness), appBrightnessSeekbar.getProgress()));
            else
                appBrightness.setText(R.string.sett_blacklist_brightness_unchanged);
        });

        appBrightnessCheckboxHolder.setOnClickListener(v ->
                appBrightnessCheckbox.setChecked(!appBrightnessCheckbox.isChecked())
        );

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
        BlacklistAppsItem item = itemAdapter.getAdapterItem(position);
        item.setAppBrightness(brightness);
        itemAdapter.remove(position);
        itemAdapter.add(position, item);
        BlacklistActivityListener listener = ((BlacklistActivityListener) getActivity());
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
        BlacklistActivityListener listener = ((BlacklistActivityListener) getActivity());
        if (listener != null)
            listener.onAppChanged();
    }

    @Override
    public boolean itemTouchOnMove(int oldPosition, int newPosition) {
        return false;
    }

}
