package io.voodoo.apps.ads.admob.util

import android.view.View
import android.view.ViewGroup

internal fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}
