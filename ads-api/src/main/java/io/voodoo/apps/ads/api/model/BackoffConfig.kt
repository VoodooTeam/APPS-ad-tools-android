package io.voodoo.apps.ads.api.model

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class BackoffConfig(
    val factor: Float = 2f,
    val delay: Duration = 1.seconds,
    val maxDelay: Duration = Duration.INFINITE,
    val maxRetry: Int = Int.MAX_VALUE
) {

    /**
     * @param attempt [0, ∞[
     * @return delay for given attempt, or null if no more attempt should be made
     */
    fun getDelay(attempt: Int): Duration? {
        if (attempt >= maxRetry) return null
        val factor = factor.pow(attempt).toDouble()
        return (delay * factor).coerceAtMost(maxDelay)
    }
}
