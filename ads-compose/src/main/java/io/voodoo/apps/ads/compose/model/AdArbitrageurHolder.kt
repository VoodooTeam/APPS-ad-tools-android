package io.voodoo.apps.ads.compose.model

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.api.AdArbitrageur

// wrapper for compose stability
@Immutable
class AdArbitrageurHolder(val arbitrageur: AdArbitrageur) {

    override fun equals(other: Any?): Boolean {
        return this === other || (other is AdArbitrageurHolder && other.arbitrageur === arbitrageur)
    }

    override fun hashCode(): Int {
        return arbitrageur.hashCode()
    }
}
