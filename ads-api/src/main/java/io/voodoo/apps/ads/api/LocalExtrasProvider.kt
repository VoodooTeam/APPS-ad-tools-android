package io.voodoo.apps.ads.api

fun interface LocalExtrasProvider {
    fun getLocalExtras(): Array<Pair<String, Any>>
}
