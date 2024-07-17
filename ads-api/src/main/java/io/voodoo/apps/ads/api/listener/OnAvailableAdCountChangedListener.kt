package io.voodoo.apps.ads.api.listener

import io.voodoo.apps.ads.api.AdClient

fun interface OnAvailableAdCountChangedListener {

    /**
     * Called when the return of [io.voodoo.apps.ads.api.AdClient.getAvailableAdCount] changes.
     *
     * Note: when loading an ad, if the ad is immediately blocked, the listener might not get called
     * because the ad could be marked as not "servable" [io.voodoo.apps.ads.api.model.Ad.canBeServed]
     * before it's added to the pool (thus when added to the pool, the number of available ad is unchanged).
     */
    fun onAvailableAdCountChanged(client: AdClient<*>, count: AdClient.AdCount)
}
