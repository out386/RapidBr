package com.out386.rapidbr.settings.bottom.blacklist.picker;

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
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.out386.rapidbr.R;

import java.util.Map;
import java.util.TreeMap;

public class BlacklistPickerFragment extends Fragment implements OnClickListener<BlacklistPickerItem> {

    private ItemAdapter<BlacklistPickerItem> itemAdapter;
    private ProgressDialog progressDialog;
    private BlacklistActivityListener listener;

    public BlacklistPickerFragment() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (BlacklistActivityListener) context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_blacklist_picker, container, false);
        RecyclerView recyclerView = v.findViewById(R.id.blacklist_picker_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        itemAdapter = new ItemAdapter<>();
        FastAdapter<BlacklistPickerItem> fastAdapter = FastAdapter.with(itemAdapter);
        fastAdapter
                .withSelectable(true)
                .withOnClickListener(this);
        recyclerView.setAdapter(fastAdapter);

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fetchApps();
    }

    @Override
    public boolean onClick(View v, IAdapter<BlacklistPickerItem> adapter, BlacklistPickerItem item, int position) {
        listener.onAppPicked(item);
        return true;
    }

    private void setListData(TreeMap<String, BlacklistPickerItem> apps) {
        if (progressDialog != null)
            progressDialog.dismiss();
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(() -> {
                itemAdapter.clear();
                if (apps != null && apps.size() > 0) {
                    for (Map.Entry<String, BlacklistPickerItem> entry : apps.entrySet())
                        itemAdapter.add(entry.getValue());
                }
            });
    }


    private void fetchApps() {
        Context context = getContext();
        if (context == null)
            return;
        progressDialog = new ProgressDialog(context);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getResources().getString(R.string.sett_blacklist_loading));
        progressDialog.setCancelable(false);
        progressDialog.show();

        AllAppsStore appsStore = AllAppsStore.getInstance(requireContext());
        appsStore.fetchApps(this::setListData, true);
    }
}
