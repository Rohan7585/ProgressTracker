package com.rohan.progresstracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rohan.progresstracker.ui.about.AboutScreen
import com.rohan.progresstracker.ui.backup.DataTransferScreen
import com.rohan.progresstracker.ui.edit.TaskEditScreen
import com.rohan.progresstracker.ui.profile.ProfileScreen
import com.rohan.progresstracker.ui.progress.ProgressScreen
import com.rohan.progresstracker.ui.todo.TodoScreen
import kotlinx.coroutines.launch

/** All screen routes in one place. */
object Routes {
    const val PROGRESS = "progress"
    const val TODO = "todo"
    const val PROFILE = "profile"
    const val DATA_TRANSFER = "data_transfer"
    const val ABOUT = "about"

    /** kind = "recurring" or "todo"; id = -1 means "add new". */
    const val EDIT_TASK = "edit_task?kind={kind}&id={id}"

    const val KIND_RECURRING = "recurring"
    const val KIND_TODO = "todo"

    fun editTask(kind: String, id: Long = -1L) = "edit_task?kind=$kind&id=$id"
}

/** The two main screens; they have the bottom bar and the side menu. */
private val mainRoutes = setOf(Routes.PROGRESS, Routes.TODO)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe-to-open only on the main screens; other screens have a back arrow instead.
        gesturesEnabled = currentRoute in mainRoutes || drawerState.isOpen,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    if (route in mainRoutes) {
                        navController.switchTab(route)
                    } else if (route != currentRoute) {
                        navController.navigate(route) { launchSingleTop = true }
                    }
                },
            )
        },
    ) {
        NavHost(navController = navController, startDestination = Routes.PROGRESS) {
            composable(Routes.PROGRESS) {
                ProgressScreen(navController, onOpenDrawer = openDrawer)
            }
            composable(Routes.TODO) {
                TodoScreen(navController, onOpenDrawer = openDrawer)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(navController)
            }
            composable(Routes.DATA_TRANSFER) {
                DataTransferScreen(navController)
            }
            composable(Routes.ABOUT) {
                AboutScreen(navController)
            }
            composable(
                route = Routes.EDIT_TASK,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType; defaultValue = Routes.KIND_RECURRING },
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) {
                TaskEditScreen(navController)
            }
        }
        // Back closes the menu first. Declared after NavHost so it takes priority over its back handling.
        BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    }
}

/** Bottom bar shown on the two main screens (Progress and Todo). */
@Composable
fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Routes.PROGRESS,
            onClick = { navController.switchTab(Routes.PROGRESS) },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            label = { Text("Progress") },
        )
        NavigationBarItem(
            selected = currentRoute == Routes.TODO,
            onClick = { navController.switchTab(Routes.TODO) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Todo") },
        )
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
