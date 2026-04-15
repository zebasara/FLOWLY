package com.flowly.move.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.flowly.move.data.local.UserPreferences
import com.flowly.move.ui.theme.FlowlyAccent
import com.flowly.move.ui.theme.FlowlyBg
import com.flowly.move.ui.screens.auth.*
import com.flowly.move.ui.screens.canjes.*
import com.flowly.move.ui.screens.fondo.FondoPremiosScreen
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
import com.flowly.move.ui.screens.admin.AdminCanjesScreen
import com.flowly.move.ui.screens.admin.AdminStoreScreen
import com.flowly.move.ui.screens.misiones.MisionesScreen
import com.flowly.move.ui.screens.store.StoreScreen
import com.flowly.move.ui.screens.video.VideoScreen

@Composable
fun FlowlyNavGraph() {
    val context = LocalContext.current
    val prefs   = remember { UserPreferences(context) }

    // initial = null → sabemos que DataStore aún no cargó
    // Cuando emite el primer valor, pasan a Boolean/String → ya podemos navegar
    val isLoggedIn:        Boolean? by prefs.isLoggedIn.collectAsState(initial = null)
    val isOnboardingDone:  Boolean? by prefs.isOnboardingDone.collectAsState(initial = null)
    val isProfileComplete: Boolean? by prefs.isProfileComplete.collectAsState(initial = null)
    val userId:            String?  by prefs.userId.collectAsState(initial = null)

    // Timeout de seguridad: si DataStore tarda más de 4s (disco lleno, corrupción,
    // primer arranque muy lento) forzamos el flujo de login en vez de pantalla negra
    var loadTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(4_000)
        loadTimedOut = true
    }

    // Copias locales inmutables para que el smart-cast funcione y evitar !! sobre
    // delegated properties (que Kotlin no puede smart-castear de forma segura)
    val loggedIn    = isLoggedIn
    val onboarding  = isOnboardingDone
    val profileDone = isProfileComplete
    val uid         = userId

    // Mostrar spinner mientras DataStore carga (< 50 ms en el 99% de los casos)
    if (!loadTimedOut && (loggedIn == null || onboarding == null || profileDone == null || uid == null)) {
        Box(
            modifier = Modifier.fillMaxSize().background(FlowlyBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color       = FlowlyAccent,
                strokeWidth = 2.dp
            )
        }
        return
    }

    // DataStore es la fuente de verdad para la sesión.
    // Firebase Auth restaura su sesión del disco de forma asíncrona — usar currentUser
    // directamente en este punto puede devolver null en arranque frío aunque el usuario
    // esté logueado. DataStore se escribe explícitamente en login/logout, por eso es fiable.
    // Si hubo timeout, asumir valores seguros: onboarding=visto, loggedIn=false
    val safeLoggedIn    = loggedIn    ?: false
    val safeOnboarding  = onboarding  ?: true
    val safeProfileDone = profileDone ?: false
    val safeUid         = uid         ?: ""

    val startDest = when {
        !safeOnboarding                             -> Routes.ONBOARDING
        !safeLoggedIn                               -> Routes.LOGIN
        !safeProfileDone && safeUid.isNotBlank()    -> Routes.completeProfile(safeUid)
        !safeProfileDone                            -> Routes.LOGIN
        else                                        -> Routes.HOME
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDest) {

        composable(Routes.ONBOARDING) {
            val scope = rememberCoroutineScope()
            OnboardingScreen {
                // Guardar en DataStore ANTES de navegar — sin esto la sesión
                // se reinicia en cada arranque frío porque isOnboardingDone queda false
                scope.launch { prefs.setOnboardingDone() }
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
                navArgument("amount")    { defaultValue = "0"    },
                navArgument("move")      { defaultValue = "0"    },
                navArgument("categoria") { defaultValue = "cash" }
            )
        ) { back ->
            ConfirmCanjeScreen(
                amount    = back.arguments?.getString("amount")    ?: "0",
                move      = back.arguments?.getString("move")      ?: "0",
                categoria = back.arguments?.getString("categoria") ?: "cash",
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

        composable(Routes.FONDO_PREMIOS) {
            FondoPremiosScreen(navController = navController)
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

        composable(Routes.MISIONES) {
            MisionesScreen(navController = navController)
        }

        composable(Routes.ADMIN_CANJES) {
            AdminCanjesScreen(navController = navController)
        }
        composable(Routes.ADMIN_STORE) {
            AdminStoreScreen(navController = navController)
        }
    }
}
