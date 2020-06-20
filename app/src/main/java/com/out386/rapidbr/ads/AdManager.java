package com.out386.rapidbr.ads;

/*
 * Copyright (C) 2020 Ritayan Chakraborty <ritayanout@gmail.com>
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.out386.rapidbr.BuildConfig;
import com.out386.rapidbr.R;

import static android.view.View.GONE;
import static com.google.android.gms.ads.formats.NativeAdOptions.ADCHOICES_TOP_RIGHT;

public class AdManager {
    private static final int NUM_ADS = 5;

    private static AdManager adManager;
    private AdLoader adLoader;
    private int currentAdId;
    private OnAdLoadedListener currentAdListener;
    private boolean isAdLoading = true;
    private UnifiedNativeAd[] loadedAds = new UnifiedNativeAd[NUM_ADS];

    private AdManager(Context context) {
        Context applicationContext = context.getApplicationContext();
        adLoader = new AdLoader.Builder(applicationContext, applicationContext.getString(R.string.app_ad_id))
                .forUnifiedNativeAd(new UnifiedAdLoaderListener())
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .setAdChoicesPlacement(ADCHOICES_TOP_RIGHT)
                        .build())
                .build();

        AdRequest.Builder adBuilder = new AdRequest.Builder();
        /*if (BuildConfig.DEBUG) {
            adBuilder.addTestDevice(BuildConfig.AD_TEST_DEVICE_ID);
        }*/
        adLoader.loadAds(adBuilder.build(), NUM_ADS);
    }

    public static AdManager getInstance(Context context) {
        if (adManager == null)
            adManager = new AdManager(context);
        return adManager;
    }

    private class UnifiedAdLoaderListener implements UnifiedNativeAd.OnUnifiedNativeAdLoadedListener {
        private int numLoadedAds = 0;

        @Override
        public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
            loadedAds[numLoadedAds++] = unifiedNativeAd;
            isAdLoading = adLoader.isLoading();

            if (currentAdListener != null && !adLoader.isLoading()) {
                getAd(currentAdListener, currentAdId);
                currentAdListener = null;
                currentAdId = -1;
            }
        }
    }

    public void getAd(OnAdLoadedListener onAdLoadedListener, int requesterId) {
        if (loadedAds[requesterId] != null) {
            onAdLoadedListener.onAdLoaded(loadedAds[requesterId]);
        } else if (isAdLoading) {
            currentAdListener = onAdLoadedListener;
            currentAdId = requesterId;
        } else if (loadedAds[0] != null) {
            onAdLoadedListener.onAdLoaded(loadedAds[0]);
        } else {
            onAdLoadedListener.onAdLoaded(null);
        }
    }

    private static AdPair getViewForAd(
            LayoutInflater inflater, UnifiedNativeAd ad) {
        RelativeLayout adHolder = (RelativeLayout) inflater.inflate(R.layout.ad_layout, null);
        UnifiedNativeAdView adView = adHolder.findViewById(R.id.ad_view);

        ImageView image = adView.findViewById(R.id.ad_icon);
        TextView headline = adView.findViewById(R.id.ad_headline);
        TextView advertiser = adView.findViewById(R.id.ad_advertiser);
        TextView body = adView.findViewById(R.id.ad_body);
        TextView price = adView.findViewById(R.id.ad_price);
        RatingBar rating = adView.findViewById(R.id.ad_stars);
        AppCompatButton button = adView.findViewById(R.id.ad_call_to_action);
        MediaView mediaView = adView.findViewById(R.id.ad_media);

        if (ad.getIcon() != null) {
            image.setImageDrawable(ad.getIcon().getDrawable());
        } else {
            // getIcon should never return null according to the docs, but it's still happening anyway.
            if (ad.getMediaContent() != null) {
                mediaView.setVisibility(View.VISIBLE);
                image.setVisibility(GONE);
                adView.setMediaView(mediaView);
                mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER);
            }
        }
        headline.setText(ad.getHeadline());
        body.setText(ad.getBody());
        if (ad.getAdvertiser() != null) {
            advertiser.setVisibility(View.VISIBLE);
            advertiser.setText(ad.getAdvertiser());
            adView.setAdvertiserView(advertiser);
        } else {
            advertiser.setVisibility(GONE);
        }
        if (ad.getPrice() != null) {
            price.setVisibility(View.VISIBLE);
            price.setText(ad.getPrice());
            adView.setPriceView(price);
        } else {
            price.setVisibility(GONE);
        }
        if (ad.getStarRating() != null) {
            rating.setVisibility(View.VISIBLE);
            rating.setRating((float) ((double) ad.getStarRating()));
            adView.setStarRatingView(rating);
        } else {
            rating.setVisibility(GONE);
        }
        button.setText(ad.getCallToAction());

        adView.setIconView(image);
        adView.setHeadlineView(headline);
        adView.setBodyView(body);
        adView.setCallToActionView(button);
        adView.setNativeAd(ad);
        return new AdPair(adView, adHolder);
    }

    public static UnifiedNativeAdView inflateAd(LinearLayout rootView, UnifiedNativeAd ad) {
        if (ad == null)
            // TODO: Show an error
            return null;

        LayoutInflater inflater = LayoutInflater.from(rootView.getContext());
        AdPair adPair = getViewForAd(inflater, ad);
        rootView.removeAllViews();
        rootView.addView(adPair.getAdHolderView());
        rootView.setVisibility(View.VISIBLE);
        return adPair.getNativeAdView();
    }

    private static class AdPair {
        private UnifiedNativeAdView nativeAdView;
        private View adHolderView;

        AdPair(UnifiedNativeAdView nativeAdView, View adHolderView) {
            this.nativeAdView = nativeAdView;
            this.adHolderView = adHolderView;
        }

        UnifiedNativeAdView getNativeAdView() {
            return nativeAdView;
        }

        View getAdHolderView() {
            return adHolderView;
        }
    }
}
