package io.voodoo.apps.ads.util

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

val Context.activity: ComponentActivity
    get() {
        return when (this) {
            is ComponentActivity -> this
            is ContextWrapper -> baseContext.activity
            else -> throw IllegalStateException("Expected an activity context")
        }
    }
