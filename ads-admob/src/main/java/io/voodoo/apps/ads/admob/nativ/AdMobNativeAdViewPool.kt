package io.voodoo.apps.ads.admob.nativ

import android.content.Context
import com.google.android.gms.ads.nativead.NativeAdView
import io.voodoo.apps.ads.admob.util.ViewPool

internal class AdMobNativeAdViewPool(
    private val factory: AdMobNativeAdViewFactory,
) : ViewPool<NativeAdView>() {

    override fun createView(context: Context): NativeAdView {
        return factory.create(context)
    }
}
