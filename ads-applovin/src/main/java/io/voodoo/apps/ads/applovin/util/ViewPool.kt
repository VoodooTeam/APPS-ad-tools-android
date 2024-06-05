package io.voodoo.apps.ads.applovin.util

import android.content.Context
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.UiThread

internal abstract class ViewPool<T : View> {

    var maxSize: Int = 5
        set(value) {
            field = value
            ensureSize()
        }
    private val pool = mutableListOf<T>()

    fun getOrNull(): T? = synchronized(pool) { pool.removeFirstOrNull() }

    @UiThread
    fun getOrCreate(context: Context): T {
        return getOrNull()
            ?: createView(context)
                .apply {
                    // State should not be saved for pooled views
                    isSaveEnabled = false
                    isSaveFromParentEnabled = false
                }
    }

    @CallSuper
    open fun release(view: T) {
        check(view.parent == null) { "View must be detached from parent before releasing it" }
        synchronized(this) {
            pool.add(view)
            ensureSize()
        }
    }

    @CallSuper
    open fun clear() {
        synchronized(pool) {
            pool.forEach { destroy(it) }
            pool.clear()
        }
    }

    abstract fun createView(context: Context): T

    protected open fun destroy(view: T) {}

    private fun ensureSize() {
        synchronized(pool) {
            while (pool.size > maxSize) {
                destroy(pool.removeAt(0))
            }
        }
    }
}

