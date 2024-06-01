package io.voodoo.apps.ads.compose.model

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.api.model.Ad

// wrapper for compose stability
@Immutable
class AdHolder(val ad: Ad) {

    override fun equals(other: Any?): Boolean {
        return this === other || (other is AdHolder && other.ad === ad)
    }

    override fun hashCode(): Int {
        return ad.hashCode()
    }
}
