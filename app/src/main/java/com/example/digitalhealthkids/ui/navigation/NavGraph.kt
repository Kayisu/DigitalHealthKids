package com.example.digitalhealthkids.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.digitalhealthkids.ui.auth.LoginScreen
import com.example.digitalhealthkids.ui.home.HomeScreen
import com.example.digitalhealthkids.ui.home.components.DailyDetailScreen
import com.example.digitalhealthkids.ui.home.components.AppDetailScreen // Bu dosyanın var olduğunu varsayıyorum
import com.example.digitalhealthkids.ui.policy.PolicyViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // 1. LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userId, deviceId ->
                    navController.navigate("home/$userId/$deviceId") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 2. HOME (GÜNCELLENDİ)
        composable(
            route = "home/{userId}/{deviceId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable

            HomeScreen(
                userId = userId,
                deviceId = deviceId,
                onNavigateToDetail = { dayIndex ->
                    // Günlük detay (Dashboard grafiği için)
                    navController.navigate("detail/$userId/$deviceId/$dayIndex")
                },
                onNavigateToAppDetail = { packageName, appName ->
                    navController.navigate("app_detail/$userId/$packageName/$appName")
                }
            )
        }

        // 3. DAILY DETAIL (MEVCUT)
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

        // 4. APP DETAIL (YENİ EKLENDİ)
        composable(
            route = "app_detail/{userId}/{packageName}/{appName}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("packageName") { type = NavType.StringType },
                navArgument("appName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val appName = backStackEntry.arguments?.getString("appName") ?: ""

            val policyViewModel: PolicyViewModel = hiltViewModel()

            AppDetailScreen(
                userId = userId,
                packageName = packageName,
                appName = appName,
                category = "Genel",
                onBackClick = { navController.popBackStack() },
                onAddPolicy = { pkg, limit ->
                    policyViewModel.addPolicy(pkg, limit)
                }
            )
        }
    }
}