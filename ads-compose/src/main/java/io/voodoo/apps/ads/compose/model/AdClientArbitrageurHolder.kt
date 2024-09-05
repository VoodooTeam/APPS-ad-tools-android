package io.voodoo.apps.ads.compose.model

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.api.AdClientArbitrageur

// wrapper for compose stability
@Immutable
@Deprecated("Use compose config file instead for stability")
class AdClientArbitrageurHolder(val arbitrageur: AdClientArbitrageur) {

    override fun equals(other: Any?): Boolean {
        return this === other || (other is AdClientArbitrageurHolder && other.arbitrageur === arbitrageur)
    }

    override fun hashCode(): Int {
        return arbitrageur.hashCode()
    }
}
