package com.out386.rapidbr.ads;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

public interface OnAdLoadedListener {
    void onAdLoaded(@Nullable UnifiedNativeAd ad);
}
