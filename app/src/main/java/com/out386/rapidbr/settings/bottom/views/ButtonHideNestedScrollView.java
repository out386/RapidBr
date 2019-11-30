package com.out386.rapidbr.settings.bottom.views;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;

public class ButtonHideNestedScrollView extends NestedScrollView {
    private OnButtonVisibilityEventListener buttonListener;
    private Handler handler;
    private ShowRunnable showRunnable;

    public ButtonHideNestedScrollView(@NonNull Context context) {
        this(context, null);
    }

    public ButtonHideNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonHideNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs,
                                      int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setupButtonHideListener(@NonNull FragmentActivity activity) {
        if (activity instanceof OnButtonVisibilityEventListener) {
            setupButtonHideListener((OnButtonVisibilityEventListener) activity);
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnNavigationListener");
        }
    }

    public void setupButtonHideListener(@NonNull OnButtonVisibilityEventListener buttonListener) {
        this.buttonListener = buttonListener;
        setOnScrollChangeListener(
                (OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int dy = scrollY - oldScrollY;
                    if (dy > 1)
                        buttonListener.onButtonVisibilityChanged(false);
                    else if (dy < 1)
                        buttonListener.onButtonVisibilityChanged(true);
                });

    }

    public void forceButtonShow() {
        if (buttonListener != null)
            buttonListener.onButtonVisibilityChanged(true);
    }

    public void forceButtonShowDelayed(int delay) {
        if (handler == null)
            handler = new Handler();
        if (showRunnable == null)
            showRunnable = new ShowRunnable();
        handler.postDelayed(showRunnable, delay);
    }

    public interface OnButtonVisibilityEventListener {
        void onButtonVisibilityChanged(boolean isShow);
    }

    class ShowRunnable implements Runnable {
        @Override
        public void run() {
            forceButtonShow();
        }
    }
}
