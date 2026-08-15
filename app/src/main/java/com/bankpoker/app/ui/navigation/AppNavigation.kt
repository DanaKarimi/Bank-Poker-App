package com.bankpoker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bankpoker.app.data.local.BankPokerDatabase
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.ui.screens.TableDetailScreen
import com.bankpoker.app.ui.screens.TablesScreen
import com.bankpoker.app.viewmodel.TableDetailViewModel
import com.bankpoker.app.viewmodel.TablesViewModel
import com.bankpoker.app.viewmodel.TablesViewModelFactory
import com.bankpoker.app.viewmodel.TableDetailViewModelFactory
import com.bankpoker.app.viewmodel.StatsViewModel
import com.bankpoker.app.viewmodel.StatsViewModelFactory
import com.bankpoker.app.ui.screens.StatsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    database: BankPokerDatabase
) {
    val repository = PokerRepository(
        pokerTableDao = database.pokerTableDao(),
        playerDao = database.playerDao(),
        buyInDao = database.buyInDao(),
        exitRecordDao = database.exitRecordDao()
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Tables.route
    ) {
        composable(Screen.Tables.route) {
            val viewModel: TablesViewModel = viewModel(
                factory = TablesViewModelFactory(repository)
            )
            TablesScreen(
                viewModel = viewModel,
                onTableClick = { tableId ->
                    navController.navigate(Screen.TableDetail.createRoute(tableId))
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                }
            )
        }

        composable(
            route = Screen.TableDetail.route,
            arguments = listOf(
                navArgument("tableId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tableId = backStackEntry.arguments?.getString("tableId") ?: return@composable
            val viewModel: TableDetailViewModel = viewModel(
                factory = TableDetailViewModelFactory(repository, tableId)
            )
            TableDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Stats.route) {
            val viewModel: StatsViewModel = viewModel(
                factory = StatsViewModelFactory(repository)
            )
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
