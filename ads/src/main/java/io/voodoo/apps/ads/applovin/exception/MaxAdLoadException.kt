package io.voodoo.apps.ads.applovin.exception

import com.applovin.mediation.MaxError
import java.io.IOException

class MaxAdLoadException(val error: MaxError) : IOException(error.message)
