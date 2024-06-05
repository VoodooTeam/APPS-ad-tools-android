package io.voodoo.apps.ads.applovin.util

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdWaterfallInfo
import com.applovin.mediation.MaxNetworkResponseInfo
import com.applovin.sdk.AppLovinSdkUtils

internal class MaxDummyAd(
    private val adUnit: String,
    private val format: MaxAdFormat
) : MaxAd {

    override fun getFormat(): MaxAdFormat = format
    override fun getSize() = AppLovinSdkUtils.Size(0, 0)
    override fun getAdUnitId() = adUnit
    override fun getNetworkName() = ""
    override fun getNetworkPlacement() = ""
    override fun getPlacement() = ""

    override fun getWaterfall(): MaxAdWaterfallInfo {
        return object : MaxAdWaterfallInfo {
            override fun getLoadedAd() = this@MaxDummyAd
            override fun getName() = ""
            override fun getTestName() = ""
            override fun getNetworkResponses() = emptyList<MaxNetworkResponseInfo>()
            override fun getLatencyMillis() = 0L
        }
    }

    override fun getRequestLatencyMillis() = 0L
    override fun getCreativeId() = null
    override fun getAdReviewCreativeId() = null
    override fun getRevenue(): Double = 0.0
    override fun getRevenuePrecision() = ""
    override fun getDspName() = null
    override fun getDspId() = null
    override fun getAdValue(p0: String?) = null
    override fun getAdValue(p0: String?, p1: String?) = ""
    override fun getNativeAd() = null
}
