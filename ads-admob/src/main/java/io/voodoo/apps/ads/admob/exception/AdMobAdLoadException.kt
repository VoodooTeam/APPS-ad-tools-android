package io.voodoo.apps.ads.admob.exception

import com.google.android.gms.ads.LoadAdError
import java.io.IOException

class AdMobAdLoadException(val error: LoadAdError) : IOException(error.message)
