package io.voodoo.apps.ads.compose.model

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.api.AdClient
import io.voodoo.apps.ads.api.model.Ad

// wrapper for compose stability
@Immutable
class AdClientHolder<T : Ad>(val client: AdClient<T>) : AdClient<T> by client {

    override fun equals(other: Any?): Boolean {
        return this === other || (other is AdClientHolder<*> && other.client === client)
    }

    override fun hashCode(): Int {
        return client.hashCode()
    }
}
