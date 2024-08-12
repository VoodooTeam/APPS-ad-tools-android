package io.voodoo.apps.ads.admob.util

import com.google.android.gms.ads.nativead.NativeAd
import io.voodoo.apps.ads.api.model.Ad

val NativeAd.id: Ad.Id get() = Ad.Id(System.identityHashCode(this).toString())

fun NativeAd.buildInfo(
    // Special case to allow override in rewarded ads
    // because the placement value before the display is null
    placement: String? = null,
): Ad.Info {
    return Ad.Info(
        adUnit = "",
        network = "",
        revenue = 0.0,
        revenuePrecision = "",
        cohortId = null,
        creativeId = null,
        placement = null,
        reviewCreativeId = null,
        formatLabel = null,
        requestLatencyMillis = 0,
    ) // TODO
}