package com.skynet.monitoring.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skynet.monitoring.ui.screens.auth.LoginScreen
import com.skynet.monitoring.ui.screens.notifications.NotificationScreen
import com.skynet.monitoring.ui.screens.profile.ProfileScreen
import com.skynet.monitoring.ui.screens.tasks.TaskDetailScreen
import com.skynet.monitoring.ui.screens.tasks.TaskListScreen
import com.skynet.monitoring.ui.screens.working.WorkingScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.TASKS, "Tugas", Icons.AutoMirrored.Filled.List),
    BottomNavItem(Routes.NOTIFICATIONS, "Notifikasi", Icons.Filled.Notifications),
    BottomNavItem(Routes.PROFILE, "Profil", Icons.Filled.Person),
)

@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.TASKS) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.TASKS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.TASKS) {
                TaskListScreen(
                    onTaskClick = { id -> navController.navigate(Routes.taskDetail(id)) },
                )
            }

            composable(
                route = Routes.TASK_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.IntType }),
            ) {
                TaskDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToWorking = { id ->
                        navController.navigate(Routes.working(id))
                    },
                )
            }

            composable(
                route = Routes.WORKING,
                arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.IntType }),
            ) {
                WorkingScreen(
                    onFinished = {
                        // Kembali ke daftar tugas (fresh) setelah selesai.
                        navController.navigate(Routes.TASKS) {
                            popUpTo(Routes.TASKS) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.NOTIFICATIONS) { NotificationScreen() }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
