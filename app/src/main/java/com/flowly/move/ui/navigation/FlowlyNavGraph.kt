package com.flowly.move.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.*
import androidx.navigation.compose.*
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.ui.screens.auth.*
import com.flowly.move.ui.screens.blockchain.BlockchainScreen
import com.flowly.move.ui.screens.canjes.*
import com.flowly.move.ui.screens.home.HomeScreen
import com.flowly.move.ui.screens.holding.*
import com.flowly.move.ui.screens.levels.LevelsScreen
import com.flowly.move.ui.screens.map.MapScreen
import com.flowly.move.ui.screens.notifications.NotificationsScreen
import com.flowly.move.ui.screens.onboarding.OnboardingScreen
import com.flowly.move.ui.screens.profile.EditProfileScreen
import com.flowly.move.ui.screens.profile.ProfileScreen
import com.flowly.move.ui.screens.rankings.RankingsScreen
import com.flowly.move.ui.screens.referrals.ReferralsScreen
import com.flowly.move.ui.screens.store.StoreScreen
import com.flowly.move.ui.screens.video.VideoScreen

@Composable
fun FlowlyNavGraph() {
    val context = LocalContext.current
    val prefs   = remember { UserPreferences(context) }

    val isLoggedIn        by prefs.isLoggedIn.collectAsState(initial = false)
    val isOnboardingDone  by prefs.isOnboardingDone.collectAsState(initial = false)
    val isProfileComplete by prefs.isProfileComplete.collectAsState(initial = false)
    val userId            by prefs.userId.collectAsState(initial = "")

    val startDest = when {
        !isOnboardingDone                   -> Routes.ONBOARDING
        !isLoggedIn                         -> Routes.LOGIN
        !isProfileComplete && userId.isNotBlank() -> Routes.completeProfile(userId)
        !isProfileComplete                  -> Routes.LOGIN
        else                                -> Routes.HOME
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.ONBOARDING) {
            OnboardingScreen {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }
        }

        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }

        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }

        composable(
            Routes.COMPLETE_PROFILE,
            arguments = listOf(navArgument("uid") { defaultValue = "" })
        ) { back ->
            CompleteProfileScreen(
                uid = back.arguments?.getString("uid") ?: "",
                navController = navController
            )
        }

        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }

        composable(Routes.MAP) {
            MapScreen(navController = navController)
        }

        composable(Routes.VIDEO) {
            VideoScreen(navController = navController)
        }

        composable(Routes.CANJES) {
            CanjesScreen(navController = navController)
        }

        composable(
            Routes.CONFIRM_CANJE,
            arguments = listOf(
                navArgument("amount") { defaultValue = "0" },
                navArgument("move")   { defaultValue = "0" }
            )
        ) { back ->
            ConfirmCanjeScreen(
                amount = back.arguments?.getString("amount") ?: "0",
                move   = back.arguments?.getString("move")   ?: "0",
                navController = navController
            )
        }

        composable(Routes.MY_CANJES) {
            MyCanjesScreen(navController = navController)
        }

        composable(Routes.STORE) {
            StoreScreen(navController = navController)
        }

        composable(Routes.RANKINGS) {
            RankingsScreen(navController = navController)
        }

        composable(Routes.LEVELS) {
            LevelsScreen(navController = navController)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }

        composable(Routes.HOLDING) {
            HoldingScreen(navController = navController)
        }

        composable(
            Routes.CONFIRM_HOLDING,
            arguments = listOf(
                navArgument("move")   { type = NavType.IntType; defaultValue = 0 },
                navArgument("months") { type = NavType.IntType; defaultValue = 3 }
            )
        ) { back ->
            ConfirmHoldingScreen(
                move   = back.arguments?.getInt("move")   ?: 0,
                months = back.arguments?.getInt("months") ?: 3,
                navController = navController
            )
        }

        composable(Routes.BLOCKCHAIN) {
            BlockchainScreen(navController = navController)
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(navController = navController)
        }

        composable(Routes.REFERRALS) {
            ReferralsScreen(navController = navController)
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(navController = navController)
        }
    }
}
