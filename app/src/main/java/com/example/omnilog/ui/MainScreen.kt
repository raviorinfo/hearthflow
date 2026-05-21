package com.example.omnilog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.omnilog.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash    : Screen("splash",    "Splash",  Icons.Default.Launch)
    object Login     : Screen("login",     "Login",   Icons.Default.Login)
    object Dashboard : Screen("dashboard", "Home",    Icons.Default.Home)
    object Logs      : Screen("logs",      "Logs",    Icons.Default.History)
    object Pantry    : Screen("pantry",    "Pantry",  Icons.Default.Kitchen)
    object Finance   : Screen("finance",   "Finance", Icons.Default.AccountBalance)
    object Debt      : Screen("debt",      "Planner", Icons.Default.Calculate)
    object Store     : Screen("store",     "Store",   Icons.Default.Store)
    object Profile   : Screen("profile",   "Profile", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val userAccount by viewModel.userAccount.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute != Screen.Login.route && currentRoute != Screen.Splash.route

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(viewModel.uiState) {
            viewModel.uiState.collect { state ->
                if (state is MainViewModel.UiState.Error) {
                    snackbarHostState.showSnackbar(state.message, duration = SnackbarDuration.Short)
                } else if (state is MainViewModel.UiState.Success) {
                    snackbarHostState.showSnackbar(state.message, duration = SnackbarDuration.Short)
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { 
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(data.visuals.message)
                    }
                }
            },
            topBar = {
                if (showBars) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RoutineLogo(size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("RoutineLog",
                                        style = MaterialTheme.typography.titleLarge)
                                    userAccount?.let {
                                        Text(
                                            if (it.isPro) "✦ Pro" else "Credits: ${it.aiCredits}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (it.isPro) MaterialTheme.colorScheme.tertiary
                                                    else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                navController.navigate(Screen.Store.route)
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            if (userAccount?.isPro == false &&
                                (userAccount?.aiCredits ?: 0) < 5) {
                                TextButton(
                                    onClick = { navController.navigate(Screen.Store.route) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.ElectricBolt, null,
                                        modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Top up", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (showBars) {
                    BottomNavigationBar(navController)
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(navController, viewModel)
            }
        }
    }
}

@Composable
fun NavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onAnimationFinished = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(viewModel = viewModel, onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
        composable(Screen.Logs.route)      { LogsScreen(viewModel) }
        composable(Screen.Pantry.route)    { PantryScreen(viewModel) }
        composable(Screen.Finance.route)   { FinanceScreen(viewModel) }
        composable(Screen.Debt.route)      { DebtPlannerScreen(viewModel) }
        composable(Screen.Store.route)     { StoreScreen(viewModel) }
        composable(Screen.Profile.route)   { ProfileScreen(viewModel) }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Dashboard, Screen.Logs, Screen.Pantry,
        Screen.Finance, Screen.Debt, Screen.Store, Screen.Profile
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(screen.icon, contentDescription = screen.title,
                        modifier = Modifier.size(22.dp))
                },
                label = {
                    Text(screen.title, style = MaterialTheme.typography.labelSmall)
                },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
