package io.voodoo.apps.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.applovin.sdk.AppLovinSdk
import io.voodoo.apps.ads.compose.model.AdClientArbitrageurHolder
import io.voodoo.apps.ads.feature.feed.FeedScreen
import io.voodoo.apps.ads.feature.feed.FeedViewModel
import io.voodoo.apps.ads.feature.profile.ProfileScreen

@Composable
fun AppNavHost(
    feedViewModel: FeedViewModel,
    feedAdClientArbitrageur: AdClientArbitrageurHolder?,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onNavigateToPrivacyEdit: () -> Unit,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val context = LocalContext.current

            FeedScreen(
                viewModel = feedViewModel,
                adClientArbitrageur = feedAdClientArbitrageur,
                onNavigateToMediationDebugger = {
                    AppLovinSdk.getInstance(context.applicationContext)
                        .showMediationDebugger()
                },
                onNavigateToPrivacyEdit = onNavigateToPrivacyEdit,
                onNavigateToProfileClick = {
                    navController.navigate("profile")
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                onNavigateBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
