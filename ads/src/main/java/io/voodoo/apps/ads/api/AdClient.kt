package io.voodoo.apps.ads.api

import io.voodoo.apps.ads.model.Ad
import java.io.Closeable

interface AdClient<T : Ad> : Closeable {

    val type: Ad.Type

    fun getAdReadyToDisplay(): T?
    suspend fun fetchAd(vararg localKeyValues: Pair<String, Any>): T
}