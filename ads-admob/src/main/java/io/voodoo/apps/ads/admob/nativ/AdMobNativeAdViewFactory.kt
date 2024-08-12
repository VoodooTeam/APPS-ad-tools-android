package io.voodoo.apps.ads.admob.nativ

import android.content.Context
import androidx.annotation.UiThread
import com.google.android.gms.ads.nativead.NativeAdView

interface AdMobNativeAdViewFactory {

    @UiThread
    fun create(context: Context): NativeAdView
}
