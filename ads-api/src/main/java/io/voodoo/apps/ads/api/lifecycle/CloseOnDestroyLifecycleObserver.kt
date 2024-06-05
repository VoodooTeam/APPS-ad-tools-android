package io.voodoo.apps.ads.api.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.io.Closeable

internal class CloseOnDestroyLifecycleObserver(
    private val lifecycle: Lifecycle,
    private val closeable: Closeable
) : DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        closeable.close()
    }

    fun removeFromLifecycle() {
        lifecycle.removeObserver(this@CloseOnDestroyLifecycleObserver)
    }
}
