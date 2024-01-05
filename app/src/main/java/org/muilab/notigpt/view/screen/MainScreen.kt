package org.muilab.notigpt.view.screen

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.muilab.notigpt.view.component.AppBottomNavigation
import org.muilab.notigpt.view.component.BottomNavItem
import org.muilab.notigpt.viewModel.DrawerViewModel
import org.muilab.notigpt.viewModel.GPTViewModel

@Composable
fun MainScreen(context: Context, drawerViewModel: DrawerViewModel, gptViewModel: GPTViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController, startDestination = BottomNavItem.Home.screen_route) {
                composable(BottomNavItem.Home.screen_route) {
                    HomeScreen(context, drawerViewModel, gptViewModel)
                }
                composable(BottomNavItem.Settings.screen_route) {
                    SettingsScreen()
                }
            }
        }
    }
}