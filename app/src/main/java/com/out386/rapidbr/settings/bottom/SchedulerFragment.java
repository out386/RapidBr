package com.out386.rapidbr.settings.bottom;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.out386.rapidbr.R;

public class SchedulerFragment extends Fragment {


    public SchedulerFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scheduler, container, false);
    }

}
