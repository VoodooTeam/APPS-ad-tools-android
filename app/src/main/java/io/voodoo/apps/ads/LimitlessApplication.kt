package io.voodoo.apps.ads

import android.app.Application
import io.voodoo.apps.ads.feature.ads.AdsInitiliazer

class LimitlessApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AdsInitiliazer().init(this)
    }
}
