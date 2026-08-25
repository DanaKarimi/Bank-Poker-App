package com.bankpoker.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tables : Screen("tables")
    object TableDetail : Screen("table_detail/{tableId}") {
        fun createRoute(tableId: String) = "table_detail/$tableId"
    }
    object Groups : Screen("groups")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object Stats : Screen("stats")
}
