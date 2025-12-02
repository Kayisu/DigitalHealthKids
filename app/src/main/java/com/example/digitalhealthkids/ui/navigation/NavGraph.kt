package com.example.digitalhealthkids.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.digitalhealthkids.ui.auth.LoginScreen
import com.example.digitalhealthkids.ui.home.HomeScreen
import com.example.digitalhealthkids.ui.home.components.DailyDetailScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userId, deviceId -> // 🔥 Refactor
                    navController.navigate("home/$userId/$deviceId") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "home/{userId}/{deviceId}", // 🔥 Refactor
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable

            HomeScreen(
                userId = userId, // 🔥 Refactor
                deviceId = deviceId,
                onNavigateToDetail = { dayIndex ->
                    navController.navigate("detail/$userId/$deviceId/$dayIndex")
                }
            )
        }

        composable(
            route = "detail/{userId}/{deviceId}/{dayIndex}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType },
                navArgument("dayIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val dayIndex = backStackEntry.arguments?.getInt("dayIndex") ?: 0

            DailyDetailScreen(
                userId = userId,
                deviceId = deviceId,
                initialDayIndex = dayIndex,
                navController = navController
            )
        }
    }
}