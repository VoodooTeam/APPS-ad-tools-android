package io.voodoo.apps.ads.applovin.util

import android.view.View
import android.view.ViewGroup

internal fun View.removeFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}
