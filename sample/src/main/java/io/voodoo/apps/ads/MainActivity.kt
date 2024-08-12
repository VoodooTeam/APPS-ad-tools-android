package io.voodoo.apps.ads

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import io.voodoo.apps.ads.compose.model.AdClientArbitrageurHolder
import io.voodoo.apps.ads.feature.ads.AdsInitiliazer
import io.voodoo.apps.ads.feature.ads.FeedAdClientArbitrageurFactory
import io.voodoo.apps.ads.feature.feed.FeedViewModel
import io.voodoo.apps.privacy.VoodooPrivacyManager
import io.voodoo.apps.privacy.config.SourcepointConfiguration
import io.voodoo.apps.privacy.model.VoodooPrivacyConsent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val feedViewModel: FeedViewModel by viewModels { FeedViewModel.Factory }

    // This is bound to the activity, and shouldn't be leaked outside the activity scope
    private val feedAdClientArbitrageur by lazy { FeedAdClientArbitrageurFactory().create(this) }

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
                //Might be executed from background thread
                it.printStackTrace()
                onPrivacyError()
            }
        )
    }

    private val _isConsentUiShown = mutableStateOf(false)

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
        voodooConsentManager.setOnStatusUpdate {
            _isConsentUiShown.value = it == VoodooPrivacyManager.ConsentStatus.UI_SHOWN
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
            val isConsentUiShown by remember { _isConsentUiShown }
            val adsEnabled = AdsInitiliazer.adsEnabled.collectAsStateWithLifecycle().value

            AppNavHost(
                modifier = modifier.fillMaxSize(),
                feedViewModel = feedViewModel,
                feedAdClientArbitrageur = if (adsEnabled) {
                    AdClientArbitrageurHolder(feedAdClientArbitrageur)
                } else {
                    null
                },
                onNavigateToPrivacyEdit = {
                    voodooConsentManager.changePrivacyConsent()
                }
            )

            BackHandler(enabled = isConsentUiShown) {
                voodooConsentManager.closeIfVisible()
            }
        }
    }


    private fun onPrivacyError() {
        runOnUiThread {
            Toast.makeText(applicationContext, "Privacy loading failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onReceiveConsent(consent: VoodooPrivacyConsent) {
        if (consent.adConsent || !consent.gdprApplicable) {
            //Ads can only being initialized when consent is retrieved / when privacy is not applicable
            lifecycleScope.launch(Dispatchers.Default) {
                AdsInitiliazer().init(this@MainActivity, consent.doNotSellDataEnabled)
                MobileAds.initialize(this@MainActivity) {}
            }
        }
    }
}
