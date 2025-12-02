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
        // 1. Giriş Ekranı
        composable("login") {
            LoginScreen(
                onLoginSuccess = { childId, deviceId ->
                    navController.navigate("home/$childId/$deviceId") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 2. Ana Ekran (Home) - Hatayı düzelttik ve callback ekledik
        composable(
            route = "home/{childId}/{deviceId}",
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable

            HomeScreen(
                childId = childId,
                deviceId = deviceId,
                // Ana ekrandan detay butonuna basıldığında burası çalışır:
                onNavigateToDetail = { dayIndex ->
                    navController.navigate("detail/$dayIndex")
                }
            )
        }

        // 3. Detay Ekranı (Yeni Sayfa)
        composable(
            route = "detail/{dayIndex}",
            arguments = listOf(
                navArgument("dayIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val dayIndex = backStackEntry.arguments?.getInt("dayIndex") ?: 0

            DailyDetailScreen(
                initialDayIndex = dayIndex,
                navController = navController
            )
        }
    }
}