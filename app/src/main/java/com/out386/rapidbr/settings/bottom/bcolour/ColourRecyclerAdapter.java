package com.out386.rapidbr.settings.bottom.bcolour;

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
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.out386.rapidbr.R;
import com.out386.rapidbr.utils.SizeUtils;

public class ColourRecyclerAdapter extends
        RecyclerView.Adapter<ColourRecyclerAdapter.ColorViewHolder> {

    private int[] colours;
    private int defColour;
    private int strokeWidth;
    private float size;
    private ColourItem currentColour;
    private OnItemChangedListener listener;

    ColourRecyclerAdapter(Context context, OnItemChangedListener listener, int[] colours,
                          int defColour) {
        this.colours = colours;
        this.listener = listener;
        strokeWidth = SizeUtils.dpToPx(context, 1);
        size = context.getResources().getDimension(R.dimen.button_colour_item_size);
        this.defColour = defColour;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ColourItem view = new ColourItem(parent.getContext());
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        holder.colorView.setColour(colours[position], strokeWidth, size);
        holder.colorView.setOnClickListener(view -> setCurrentColour(view, true));

        if (colours[position] == defColour)
            setCurrentColour(holder.colorView, false);
    }

    @Override
    public int getItemCount() {
        return colours.length;
    }

    private void setCurrentColour(View view, boolean callListener) {
        ColourItem colourItem = (ColourItem) view;
        if (currentColour == null) {
            colourItem.setChecked(true);
            currentColour = colourItem;
            if (callListener)
                listener.onItemChanged(currentColour.getColour());
        } else if (currentColour != colourItem) {
            currentColour.setChecked(false);
            colourItem.setChecked(true);
            currentColour = colourItem;
            if (callListener)
                listener.onItemChanged(currentColour.getColour());
        }
    }

    interface OnItemChangedListener {
        void onItemChanged(int colour);
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        ColourItem colorView;

        ColorViewHolder(@NonNull ColourItem itemView) {
            super(itemView);
            colorView = itemView;
        }
    }
}
