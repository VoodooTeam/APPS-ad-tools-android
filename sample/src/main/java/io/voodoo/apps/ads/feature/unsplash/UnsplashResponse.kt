package io.voodoo.apps.ads.feature.unsplash

import kotlinx.serialization.Serializable

@Serializable
data class UnsplashResponse(
    val results: List<UnsplashPhoto>
)
