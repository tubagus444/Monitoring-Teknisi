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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skynet.monitoring.MainActivity
import com.skynet.monitoring.data.repository.TaskRepository
import com.skynet.monitoring.ui.screens.auth.LoginScreen
import com.skynet.monitoring.ui.screens.notifications.NotificationScreen
import com.skynet.monitoring.ui.screens.profile.ProfileScreen
import com.skynet.monitoring.ui.screens.tasks.TaskDetailScreen
import com.skynet.monitoring.ui.screens.tasks.TaskListScreen
import com.skynet.monitoring.ui.screens.working.WorkingScreen
import kotlinx.coroutines.launch

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Ambil TaskRepository via Hilt untuk resolusi deep-link (report_id → task_id).
    // Hanya dipakai saat pertama kali app dibuka via tap notifikasi.
    val taskRepository: TaskRepository = hiltViewModel<DeepLinkHelperViewModel>().taskRepository

    // Handle deep-link dari tap notifikasi (saat app background/killed).
    // MainActivity menyimpan report_id dari intent extras ke deepLinkReportId StateFlow.
    // Kita consume sekali, resolve ke task assignment ID, lalu navigasi ke detail tugas.
    LaunchedEffect(Unit) {
        val activity = context as? MainActivity ?: return@LaunchedEffect
        val reportId = activity.consumeDeepLinkReportId() ?: return@LaunchedEffect

        scope.launch {
            taskRepository.getTasks()
                .onSuccess { tasks ->
                    val task = tasks.firstOrNull { it.reportId == reportId }
                    if (task != null) {
                        navController.navigate(Routes.taskDetail(task.id)) {
                            launchSingleTop = true
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Tugas tidak ditemukan atau sudah selesai", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure {
                    android.widget.Toast.makeText(context, "Gagal memuat tugas. Periksa koneksi Anda.", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }

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
                    onResumeWork = { id -> navController.navigate(Routes.working(id)) },
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
                    onMinimize = {
                        // "Kecilkan": kembali ke daftar tugas tanpa menyetop GPS service.
                        // Bottom nav tersedia lagi; teknisi bisa cek tugas/notifikasi/profil.
                        // Masuk lagi via Detail Tugas → "Lanjutkan Perbaikan".
                        navController.navigate(Routes.TASKS) {
                            popUpTo(Routes.TASKS) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationScreen(
                    onNavigateToTask = { id -> navController.navigate(Routes.taskDetail(id)) },
                )
            }

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
