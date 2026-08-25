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
import com.bankpoker.app.ui.screens.HomeScreen
import com.bankpoker.app.ui.screens.TableDetailScreen
import com.bankpoker.app.ui.screens.TablesScreen
import com.bankpoker.app.viewmodel.GroupDetailViewModel
import com.bankpoker.app.viewmodel.GroupDetailViewModelFactory
import com.bankpoker.app.viewmodel.GroupsViewModel
import com.bankpoker.app.viewmodel.GroupsViewModelFactory
import com.bankpoker.app.viewmodel.StatsViewModel
import com.bankpoker.app.viewmodel.StatsViewModelFactory
import com.bankpoker.app.viewmodel.TableDetailViewModel
import com.bankpoker.app.viewmodel.TableDetailViewModelFactory
import com.bankpoker.app.viewmodel.TablesViewModel
import com.bankpoker.app.viewmodel.TablesViewModelFactory

@Composable
fun AppNavigation(
    navController: NavHostController,
    database: BankPokerDatabase
) {
    val repository = PokerRepository(
        pokerTableDao = database.pokerTableDao(),
        playerDao = database.playerDao(),
        buyInDao = database.buyInDao(),
        exitRecordDao = database.exitRecordDao(),
        playerGroupDao = database.playerGroupDao(),
        groupBalanceDao = database.groupBalanceDao(),
        paymentDao = database.paymentDao()
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onQuickTableClick = {
                    navController.navigate(Screen.Tables.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onGroupsClick = {
                    navController.navigate(Screen.Groups.route)
                }
            )
        }

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

        composable(Screen.Groups.route) {
            val viewModel: GroupsViewModel = viewModel(
                factory = GroupsViewModelFactory(repository)
            )
            GroupsScreen(
                viewModel = viewModel,
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val viewModel: GroupDetailViewModel = viewModel(
                factory = GroupDetailViewModelFactory(repository, groupId)
            )
            GroupDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTableClick = { tableId ->
                    navController.navigate(Screen.TableDetail.createRoute(tableId))
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
