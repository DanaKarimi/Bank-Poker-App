package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AdminOverviewResponse(
    val stats: AdminStatsDto
)

data class AdminStatsDto(
    val totalUsers: Int = 0,
    val guestUsers: Int = 0,
    val fullUsers: Int = 0,
    val totalGroups: Int = 0,
    val totalTables: Int = 0,
    val activeTables: Int = 0,
    val quickTables: Int = 0,
    val activePlayers: Int = 0
)

data class AdminUsersResponse(
    val users: List<AdminUserDto> = emptyList()
)

data class AdminUserDto(
    val id: String,
    val username: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("avatar_id") val avatarId: String? = null,
    val role: String = "USER",
    @SerializedName("is_guest") val isGuest: Boolean = false,
    @SerializedName("group_count") val groupCount: Int = 0,
    @SerializedName("created_at") val createdAt: Long? = null
)

data class AdminGroupsResponse(
    val groups: List<AdminGroupDto> = emptyList()
)

data class AdminGroupDto(
    val id: String,
    val name: String,
    @SerializedName("invite_code") val inviteCode: String? = null,
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("table_count") val tableCount: Int = 0,
    @SerializedName("created_at") val createdAt: Long? = null,
    val owner: AdminOwnerDto? = null
)

data class AdminOwnerDto(
    val id: String? = null,
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null
)

data class AdminTablesResponse(
    val tables: List<AdminTableDto> = emptyList()
)

data class AdminTableDto(
    val id: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val isQuickTable: Boolean = false,
    val name: String,
    val code: String? = null,
    val status: String = "ACTIVE",
    @SerializedName("player_count") val playerCount: Int = 0,
    @SerializedName("created_at") val createdAt: Long? = null
)

data class AdminTablePlayersResponse(
    val players: List<AdminTablePlayerDto> = emptyList()
)

data class AdminTablePlayerDto(
    val id: String,
    val name: String,
    @SerializedName("user_id") val userId: String? = null,
    val totalBuyIns: Long = 0L,
    val totalExits: Long = 0L,
    val balance: Long = 0L,
    @SerializedName("linked_user") val linkedUser: AdminLinkedUserDto? = null
)

data class AdminLinkedUserDto(
    val id: String? = null,
    val username: String? = null,
    @SerializedName("avatar_id") val avatarId: String? = null
)
