package com.example.omnilog.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.omnilog.ui.theme.BrandGold
import com.example.omnilog.ui.theme.BrandGradientEnd
import com.example.omnilog.ui.theme.BrandGradientMid
import com.example.omnilog.ui.theme.BrandGradientStart
import com.example.omnilog.ui.theme.BrandEmerald
import com.example.omnilog.ui.theme.BrandAmber
import com.example.omnilog.viewmodel.MainViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Splash    : Screen("splash",    "Splash",  Icons.Default.Launch)
    object Login     : Screen("login",     "Login",   Icons.Default.Login)
    object Dashboard : Screen("dashboard", "Home",    Icons.Default.Home)
    object Logs      : Screen("logs",      "Logs",    Icons.Default.History)
    object Pantry    : Screen("pantry",    "Pantry",  Icons.Default.Kitchen)
    object Finance   : Screen("finance",   "Finance", Icons.Default.AccountBalance)
    object SplitExpense : Screen("split_expense", "Split Bills", Icons.Default.CallSplit)
    object Debt      : Screen("debt",      "Planner", Icons.Default.Calculate)
    object Profile   : Screen("profile",   "Profile", Icons.Default.Person)

    // Debt Workspace Sub-screens
    object DebtDashboard : Screen("debt_dashboard", "Workspace", Icons.Default.Dashboard)
    object DebtList      : Screen("debt_list",      "Debts",     Icons.Default.List)
    object DebtRoadmap   : Screen("debt_roadmap",   "Roadmap",   Icons.Default.Timeline)
    object DebtSimulator : Screen("debt_simulator", "Simulator", Icons.Default.TrendingUp)
    object DebtPayments  : Screen("debt_payments",  "Payments",  Icons.Default.Receipt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val userAccount by viewModel.userAccount.collectAsState()
    val isDebtMode by viewModel.isDebtMode.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute != Screen.Login.route && currentRoute != Screen.Splash.route

    val isDark = isSystemInDarkTheme()

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
                    // Premium snackbar
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandGradientStart.copy(alpha = 0.95f), BrandGradientEnd.copy(alpha = 0.95f))
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(data.visuals.message, color = Color.White,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            topBar = {
                if (showBars) {
                    // Frosted glass top bar
                    val barBg = if (isDark) Color(0x99131929) else Color(0xCCFFFFFF)
                    val barBorder = if (isDark) Color(0x22FFFFFF) else Color(0x44C4B5FD)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(barBg)
                            .border(
                                width = 0.5.dp,
                                brush = Brush.verticalGradient(listOf(Color.Transparent, barBorder)),
                                shape = RoundedCornerShape(0.dp)
                            )
                    ) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                            navigationIcon = {
                                if (isDebtMode) {
                                    TextButton(
                                        onClick = {
                                            viewModel.setDebtMode(false)
                                            navController.navigate(Screen.Dashboard.route) {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Home", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isDebtMode) {
                                        RoutineLogo(size = 34.dp)
                                        Spacer(Modifier.width(10.dp))
                                    }
                                    Column {
                                        if (isDebtMode) {
                                            GradientText(
                                                "Debt Workspace",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Text("Projected payoff planning",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline)
                                        } else {
                                            GradientText(
                                                "RoutineLog",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            userAccount?.let {
                                                Text(
                                                    "Welcome, ${it.name}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            actions = {
                                val syncStatus by viewModel.cloudSyncStatus.collectAsState()
                                val isConnected = syncStatus.contains("Connected")
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isConnected) BrandEmerald.copy(alpha = 0.15f)
                                            else BrandAmber.copy(alpha = 0.15f)
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isConnected) BrandEmerald.copy(alpha = 0.5f) else BrandAmber.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isConnected) BrandEmerald else BrandAmber)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = syncStatus,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isConnected) BrandEmerald else BrandAmber
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            },
            bottomBar = {
                if (showBars) {
                    FloatingBottomNavBar(navController, viewModel)
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
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 20 }
        },
        exitTransition = {
            fadeOut(tween(180))
        },
        popEnterTransition = {
            fadeIn(tween(220))
        },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutVertically(tween(220)) { it / 20 }
        }
    ) {
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
        composable(Screen.Finance.route)   { FinanceScreen(viewModel, navController) }
        composable(Screen.SplitExpense.route) { SplitExpenseScreen(viewModel) }
        composable(Screen.Debt.route)      { DebtPlannerScreen(viewModel) }
        composable(Screen.Profile.route)   {
            ProfileScreen(
                viewModel = viewModel,
                onSignOut = {
                    viewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Debt Workspace routes
        composable(Screen.DebtDashboard.route) { DebtDashboardScreen(viewModel) }
        composable(Screen.DebtList.route)      { DebtPlannerScreen(viewModel) }
        composable(Screen.DebtRoadmap.route)   { DebtRoadmapScreen(viewModel) }
        composable(Screen.DebtSimulator.route) { DebtSimulatorScreen(viewModel) }
        composable(Screen.DebtPayments.route)  { DebtPaymentLogsScreen(viewModel) }
    }
}

@Composable
fun FloatingBottomNavBar(navController: NavHostController, viewModel: MainViewModel) {
    val isDebtMode by viewModel.isDebtMode.collectAsState()
    val isPremiumActive by viewModel.isPremiumActive.collectAsState()
    val isDark = isSystemInDarkTheme()

    val items = if (isDebtMode) {
        listOf(
            Screen.DebtDashboard, Screen.DebtList, Screen.DebtRoadmap,
            Screen.DebtSimulator, Screen.DebtPayments
        )
    } else {
        listOf(
            Screen.Dashboard, Screen.Logs, Screen.Pantry,
            Screen.Finance, Screen.SplitExpense, Screen.Debt, Screen.Profile
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Premium glassmorphic background and border brushes
    val pillBgBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xF2161D30), Color(0xF20D1220))
        } else {
            listOf(Color(0xFAFFFFFF), Color(0xF5F3F4F6))
        }
    )
    val pillBorderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0x40FFFFFF), Color(0x18FFFFFF))
        } else {
            listOf(Color(0x80C4B5FD), Color(0x30C4B5FD))
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Drop shadow container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = BrandGradientStart.copy(alpha = if (isDark) 0.15f else 0.05f),
                    spotColor = BrandGradientStart.copy(alpha = if (isDark) 0.4f else 0.15f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(pillBgBrush)
                .border(1.dp, pillBorderBrush, RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    FloatingNavItem(
                        screen = screen,
                        selected = selected,
                        showPremiumDot = screen == Screen.Profile && isPremiumActive && !selected,
                        onClick = {
                            if (screen == Screen.Debt) {
                                viewModel.setDebtMode(true)
                                navController.navigate(Screen.DebtDashboard.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    screen: Screen,
    selected: Boolean,
    showPremiumDot: Boolean = false,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "navScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        animationSpec = tween(200),
        label = "navAlpha"
    )
    val isDark = isSystemInDarkTheme()

    // Vibrant selection pill styling
    val activeBgBrush = Brush.horizontalGradient(
        colors = listOf(
            BrandGradientStart.copy(alpha = if (isDark) 0.2f else 0.12f),
            BrandGradientMid.copy(alpha = if (isDark) 0.2f else 0.12f)
        )
    )
    val activeBorderBrush = Brush.horizontalGradient(
        colors = listOf(
            BrandGradientStart.copy(alpha = 0.5f),
            BrandGradientEnd.copy(alpha = 0.5f)
        )
    )

    Row(
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) activeBgBrush else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .border(
                width = if (selected) 1.dp else 0.dp,
                brush = if (selected) activeBorderBrush else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Icon with optional premium dot overlay
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                tint = if (selected) BrandGradientStart else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = iconAlpha)
            )
            if (showPremiumDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrandGold)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
            exit = fadeOut(tween(150)) + shrinkHorizontally(tween(150))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = screen.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
