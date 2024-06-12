package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.api.model.Ad

fun interface AdRevenueListener {
    fun onAdRevenuePaid(ad: Ad)
}
