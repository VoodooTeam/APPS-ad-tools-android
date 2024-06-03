package io.voodoo.apps.ads

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.applovin.sdk.AppLovinSdk
import io.voodoo.apps.ads.compose.model.AdArbitrageurHolder
import io.voodoo.apps.ads.feature.ads.AdArbitrageurFactory
import io.voodoo.apps.ads.feature.ads.AdsInitiliazer
import io.voodoo.apps.ads.feature.feed.FeedScreen
import io.voodoo.apps.ads.feature.feed.FeedViewModel
import io.voodoo.apps.privacy.VoodooPrivacyManager
import io.voodoo.apps.privacy.config.SourcepointConfiguration
import io.voodoo.apps.privacy.model.VoodooPrivacyConsent

class MainActivity : ComponentActivity() {

    private val feedViewModel: FeedViewModel by viewModels { FeedViewModel.Factory }

    // This is bound to the activity, and shouldn't be leaked outside the activity scope
    private val arbitrageur by lazy { AdArbitrageurFactory(application).create(this) }

    private val voodooConsentManager by lazy {
        VoodooPrivacyManager(
            lifecycleOwner = this,
            currentActivity = this,
            autoShowPopup = true,
            sourcepointConfiguration = SourcepointConfiguration(
                accountId = 1909,
                propertyId = 36309,
                gdprPrivacyManagerId = "1142456",
                usMspsPrivacyManagerId = "1143800",
                propertyName = "voodoo.native.app"
            ),
            onConsentReceived = {
                onReceiveConsent(it)
            },
            onError = {
                it.printStackTrace()
                onPrivacyError()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.BLACK
        setContent {
            // see https://issuetracker.google.com/issues/336842920#comment5
            CompositionLocalProvider(
                androidx.lifecycle.compose.LocalLifecycleOwner provides androidx.compose.ui.platform.LocalLifecycleOwner.current,
            ) {
                MainActivityContent()
            }
        }
        voodooConsentManager.initializeConsent()
    }

    @Composable
    private fun MainActivityContent(
        modifier: Modifier = Modifier
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = Color.Black,
                surface = Color.Black
            )
        ) {
            Box(modifier = modifier.fillMaxSize()) {
                val context = LocalContext.current
                val adsEnabled = AdsInitiliazer.adsEnabled.collectAsStateWithLifecycle().value

                FeedScreen(
                    viewModel = feedViewModel,
                    adArbitrageur = if (adsEnabled) AdArbitrageurHolder(arbitrageur) else null,
                    onNavigateToMediationDebugger = {
                        AppLovinSdk.getInstance(context.applicationContext).showMediationDebugger()
                    },
                    onNavigateToPrivacyEdit = {
                        voodooConsentManager.changePrivacyConsent()
                    }
                )
            }
        }
    }


    private fun onPrivacyError() {
        Toast.makeText(applicationContext, "Privacy loading failed", Toast.LENGTH_SHORT).show()
    }

    private fun onReceiveConsent(consent: VoodooPrivacyConsent) {
        if (consent.adConsent || !consent.privacyApplicable) {
            //Ads can only being initialized when consent is retrieved / when privacy is not applicable
            AdsInitiliazer().init(this, consent.doNotSellDataEnabled)
        }
    }
}
