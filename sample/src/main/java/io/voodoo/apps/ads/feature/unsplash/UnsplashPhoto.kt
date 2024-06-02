package io.voodoo.apps.ads.feature.unsplash

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnsplashPhoto(
    val id: String,
    val description: String?,
    val urls: UnsplashPhotoUrls,
    val user: UnsplashUser
) {

    @Serializable
    data class UnsplashPhotoUrls(
        val raw: String,
        val full: String,
        val regular: String,
        val small: String,
        val thumb: String
    )

    @Serializable
    data class UnsplashUser(
        val name: String,
        val username: String,
        @SerialName("profile_image") val profileImage: UnsplashUserProfileImage
    ) {

        @Serializable
        data class UnsplashUserProfileImage(
            val small: String,
            val medium: String,
            val large: String,
        )
    }
}
