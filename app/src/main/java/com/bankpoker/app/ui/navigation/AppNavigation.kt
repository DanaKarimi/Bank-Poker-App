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
import com.bankpoker.app.viewmodel.PlayerProfileViewModel
import com.bankpoker.app.viewmodel.PlayerProfileViewModelFactory
import com.bankpoker.app.ui.screens.GroupsScreen
import com.bankpoker.app.ui.screens.GroupDetailScreen
import com.bankpoker.app.ui.screens.GroupHistoryScreen
import com.bankpoker.app.ui.screens.StatsScreen
import com.bankpoker.app.ui.screens.PlayerProfileScreen
import com.bankpoker.app.viewmodel.GroupHistoryViewModel
import com.bankpoker.app.viewmodel.GroupHistoryViewModelFactory
import com.bankpoker.app.ui.screens.ServerTestScreen
import com.bankpoker.app.ui.screens.CreateGroupScreen
import com.bankpoker.app.ui.screens.RequestsScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


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
        paymentDao = database.paymentDao(),
        settlementRecordDao = database.settlementRecordDao(),
        entryFeeRecordDao = database.entryFeeRecordDao(),
        database = database
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                repository = repository,
                onQuickTableClick = {
                    navController.navigate(Screen.Tables.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onGroupsClick = {
                    navController.navigate(Screen.Groups.route)
                },
                onServerTestClick = {
                    navController.navigate(Screen.ServerTest.route)
                },
                onCreateGroupClick = {
                    navController.navigate(Screen.CreateGroup.route)
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
                },
                onPlayerClick = { playerName ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerName))
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
                },
                onCreateServerGroupClick = {
                    navController.navigate(Screen.CreateGroup.route)
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
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.GroupHistory.createRoute(groupId))
                },
                onPlayerClick = { playerName ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerName))
                },
                onNavigateToRequests = { id, name ->
                    navController.navigate(Screen.Requests.createRoute(id, name))
                }
            )
        }

        composable(
            route = Screen.GroupHistory.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val viewModel: GroupHistoryViewModel = viewModel(
                factory = GroupHistoryViewModelFactory(repository, groupId)
            )
            GroupHistoryScreen(
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
                },
                onPlayerClick = { playerName ->
                    navController.navigate(Screen.PlayerProfile.createRoute(playerName))
                }
            )
        }


        composable(
            route = Screen.PlayerProfile.route,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPlayerName = backStackEntry.arguments?.getString("playerName") ?: return@composable
            val playerName = try {
                URLDecoder.decode(rawPlayerName, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                rawPlayerName
            }
            val viewModel: PlayerProfileViewModel = viewModel(
                factory = PlayerProfileViewModelFactory(repository, playerName)
            )
            PlayerProfileScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ServerTest.route) {
            ServerTestScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Requests.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupName") {
                    type = NavType.StringType
                    defaultValue = "Online Group"
                }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val rawGroupName = backStackEntry.arguments?.getString("groupName") ?: "Online Group"
            val groupName = try {
                URLDecoder.decode(rawGroupName, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                rawGroupName
            }
            RequestsScreen(
                groupId = groupId,
                groupName = groupName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

