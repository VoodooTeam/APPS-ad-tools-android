package io.voodoo.apps.ads.api

import io.voodoo.apps.ads.model.Ad
import java.io.Closeable

interface AdClient<T : Ad> : Closeable {

    val type: Ad.Type

    fun getAdStatus(): Status
    suspend fun fetchAd(vararg localKeyValues: Pair<String, Any>): T

    enum class Status { NOT_LOADED, READY_UNSEEN, READY_SEEN, BLOCKED }
}
