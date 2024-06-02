package io.voodoo.apps.ads.feature.unsplash

import android.content.Context
import android.util.Log
import io.voodoo.apps.ads.R
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

class UnsplashClient(
    private val applicationContext: Context
) {

    private val retrofit by lazy {
        val json = Json {
            ignoreUnknownKeys = true
        }

        Retrofit.Builder()
            .baseUrl(UnsplashApi.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json; charset=UTF8".toMediaType()))
            .build()
    }

    private val service by lazy { retrofit.create<UnsplashApi>() }

    suspend fun getContent(query: String): List<UnsplashPhoto> {
        return Json.decodeFromString(readMockFile())

        return service.searchPhotos(query).results.also { items ->
            // Log content
            items
                .filter { !it.description.isNullOrBlank() }
                .forEach {
                    Log.e("Limitless", Json.encodeToString(it))
                }
        }
    }

    private fun readMockFile(): String {
        return applicationContext.resources.openRawResource(R.raw.mock).bufferedReader().readText()
    }
}
