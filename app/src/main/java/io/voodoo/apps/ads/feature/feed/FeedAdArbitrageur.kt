package io.voodoo.apps.ads.feature.feed

import androidx.compose.runtime.Immutable
import io.voodoo.apps.ads.api.AdArbitrageur

// wrapper for compose stability
@Immutable
class FeedAdArbitrageur(val arbitrageur: AdArbitrageur) {

    override fun equals(other: Any?): Boolean {
        return this === other || (other is FeedAdArbitrageur && other.arbitrageur === arbitrageur)
    }

    override fun hashCode(): Int {
        return arbitrageur.hashCode()
    }
}
