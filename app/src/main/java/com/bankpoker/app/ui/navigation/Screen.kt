package com.bankpoker.app.ui.navigation

sealed class Screen(val route: String) {
    object Tables : Screen("tables")
    object TableDetail : Screen("table_detail/{tableId}") {
        fun createRoute(tableId: String) = "table_detail/$tableId"
    }
}
