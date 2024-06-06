package io.voodoo.apps.ads.api

fun interface LocalExtrasProvider {
    suspend fun getLocalExtras(): List<Pair<String, Any>>
}
