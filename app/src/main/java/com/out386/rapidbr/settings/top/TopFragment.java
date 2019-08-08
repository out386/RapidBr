package com.out386.rapidbr.settings.top;

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
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.out386.rapidbr.R;

public class TopFragment extends Fragment {

    private TextView runningText;
    private TextView scheduledText;
    private TextView filterText;
    private TextView blacklistText;
    private Context context;
    private String[] statusMessage = new String[2];
    private int[] statusColour = new int[2];

    public TopFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_top, container, false);
        runningText = v.findViewById(R.id.top_frag_status);
        scheduledText = v.findViewById(R.id.top_frag_schedule);
        filterText = v.findViewById(R.id.top_frag_filter);
        blacklistText = v.findViewById(R.id.top_frag_blacklist);
        getStatusRes();
        return v;
    }

    private void getStatusRes() {
        Resources resources = getResources();
        statusMessage[0] = resources.getString(R.string.top_status_running);
        statusColour[0] = resources.getColor(R.color.textTopRunning, context.getTheme());
        statusMessage[1] = resources.getString(R.string.top_status_not_running);
        statusColour[1] = resources.getColor(R.color.textTopNotRunning, context.getTheme());
    }

    public void setStatus(boolean isRunning) {
        if (isRunning) {
            runningText.setText(statusMessage[0]);
            runningText.setTextColor(statusColour[0]);
        } else {
            runningText.setText(statusMessage[1]);
            runningText.setTextColor(statusColour[1]);
        }
    }

    public void setScheduled(String scheduled) {
        scheduledText.setText(scheduled);
    }

    public void setFilter(String filter) {
        filterText.setText(filter);
    }

    public void setBlacklist(String blacklist) {
        blacklistText.setText(blacklist);
    }

    private void setSecondaryStatus() {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
    }
}
