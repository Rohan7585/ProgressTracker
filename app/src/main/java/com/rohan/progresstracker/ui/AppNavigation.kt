package com.rohan.progresstracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rohan.progresstracker.ui.edit.TaskEditScreen
import com.rohan.progresstracker.ui.history.HistoryScreen
import com.rohan.progresstracker.ui.progress.ProgressScreen
import com.rohan.progresstracker.ui.todo.TodoScreen

/** All screen routes in one place. */
object Routes {
    const val PROGRESS = "progress"
    const val TODO = "todo"
    /** days = 7 or 30 */
    const val HISTORY = "history?days={days}"

    /** kind = "recurring" or "todo"; id = -1 means "add new". */
    const val EDIT_TASK = "edit_task?kind={kind}&id={id}"

    const val KIND_RECURRING = "recurring"
    const val KIND_TODO = "todo"

    fun history(days: Int) = "history?days=$days"
    fun editTask(kind: String, id: Long = -1L) = "edit_task?kind=$kind&id=$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PROGRESS) {
        composable(Routes.PROGRESS) {
            ProgressScreen(navController)
        }
        composable(Routes.TODO) {
            TodoScreen(navController)
        }
        composable(
            route = Routes.HISTORY,
            arguments = listOf(navArgument("days") { type = NavType.IntType; defaultValue = 7 }),
        ) {
            HistoryScreen(navController)
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
