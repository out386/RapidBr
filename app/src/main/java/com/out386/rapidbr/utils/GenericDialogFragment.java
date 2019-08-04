package com.out386.rapidbr.utils;

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.out386.rapidbr.R;

public class GenericDialogFragment extends DialogFragment {

    public static final String KEY_RECORD_DIALOG_SHOWN = "recordDialogShown";

    private OnPositiveButtonTappedListener positiveListener;
    private OnNegativeButtonTappedListener negativeListener;
    private String title;
    private String message;
    private String positiveText;
    private String negativeText;
    private boolean cancelable = true;

    public static GenericDialogFragment newInstance() {
        return new GenericDialogFragment();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null)
            dismiss();

        if (positiveText == null)
            positiveText = getString(R.string.ok);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (title != null)
            builder.setTitle(title);
        builder.setMessage(message)
                .setPositiveButton(positiveText, (dialog, id) -> {
                    if (positiveListener != null)
                        positiveListener.onButtonPressed();
                });
        if (negativeText != null) {
            builder.setNegativeButton(negativeText, (dialog, id) -> {
                if (negativeListener != null)
                    negativeListener.onButtonPressed(dialog);
            });
        }
        builder.setCancelable(cancelable);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(cancelable);
        return dialog;
    }

    public GenericDialogFragment setOnPositiveButtonTappedListener(OnPositiveButtonTappedListener positiveListener) {
        this.positiveListener = positiveListener;
        return this;
    }

    public GenericDialogFragment setOnNegativeButtonTappedListener(OnNegativeButtonTappedListener negativeListener) {
        this.negativeListener = negativeListener;
        return this;
    }

    public GenericDialogFragment setMessage(String message) {
        this.message = message;
        return this;
    }

    public GenericDialogFragment setTitle(String title) {
        this.title = title;
        return this;
    }

    public GenericDialogFragment setPositiveText(String positiveText) {
        this.positiveText = positiveText;
        return this;
    }

    public GenericDialogFragment setNegativeText(String negativeText) {
        this.negativeText = negativeText;
        return this;
    }

    public GenericDialogFragment setDialogCancelable(boolean isCancelable) {
        this.cancelable = isCancelable;
        return this;
    }

    public interface OnPositiveButtonTappedListener {
        void onButtonPressed();
    }

    public interface OnNegativeButtonTappedListener {
        void onButtonPressed(DialogInterface dialog);
    }
}
