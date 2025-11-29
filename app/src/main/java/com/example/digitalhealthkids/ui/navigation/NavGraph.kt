package com.example.digitalhealthkids.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.digitalhealthkids.ui.auth.LoginScreen
import com.example.digitalhealthkids.ui.home.HomeScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

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
                onLoginSuccess = { childId, deviceId ->
                    navController.navigate("home/$childId/$deviceId") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )

        }

        composable("home/{childId}/{deviceId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable

            HomeScreen(
                childId = childId,
                deviceId = deviceId
            )
        }

    }

}

