package io.voodoo.apps.ads.admob.util

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.nativead.NativeAd
import io.voodoo.apps.ads.api.model.Ad

val NativeAd.id: Ad.Id get() = Ad.Id(System.identityHashCode(this).toString())

fun NativeAd.buildInfo(
    revenue: AdValue?,
    adUnit: String,
): Ad.Info {
    return Ad.Info(
        adUnit = adUnit,
        network = "",
        revenue = revenue?.valueMicros?.toDouble() ?: 0.0,
        revenuePrecision = revenue?.precisionType?.toString() ?: "",
        cohortId = null,
        creativeId = null,
        placement = null,
        reviewCreativeId = null,
        formatLabel = null,
        requestLatencyMillis = 0,
    )
}