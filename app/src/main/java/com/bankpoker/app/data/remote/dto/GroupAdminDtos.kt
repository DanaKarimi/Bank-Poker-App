package com.bankpoker.app.data.remote.dto

data class GroupMemberDto(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarId: String? = null,
    val role: String = "MEMBER",
    val isOwner: Boolean = false,
    val balance: Long = 0L,
    val joinedAt: Long = 0L
)

data class GroupMembersResponse(
    val groupId: String? = null,
    val groupName: String? = null,
    val members: List<GroupMemberDto> = emptyList()
)

data class GroupAdminTableSummaryDto(
    val id: String = "",
    val name: String = "",
    val status: String = "ACTIVE",
    val playerCount: Int = 0,
    val totalBuyIns: Long = 0L,
    val totalExits: Long = 0L,
    val createdAt: Long = 0L,
    val closedAt: Long? = null
)

data class GroupAdminPaymentDto(
    val id: String = "",
    val fromPlayer: String = "",
    val toPlayer: String = "",
    val amount: Long = 0L,
    val createdAt: Long = 0L
)

data class GroupAdminOverviewResponse(
    val groupId: String? = null,
    val groupName: String? = null,
    val members: List<GroupMemberDto> = emptyList(),
    val tables: List<GroupAdminTableSummaryDto> = emptyList(),
    val payments: List<GroupAdminPaymentDto> = emptyList(),
    val passwordResets: List<PasswordResetRequestDto> = emptyList()
)

data class GroupAdminTablePlayerDto(
    val id: String = "",
    val userId: String? = null,
    val name: String = "",
    val username: String? = null,
    val avatarId: String? = null,
    val status: String = "ACTIVE"
)

data class GroupAdminTableTxDto(
    val id: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val amount: Long = 0L,
    val note: String? = null,
    val createdAt: Long = 0L
)

data class GroupAdminTableDetailResponse(
    val table: GroupAdminTableSummaryDto? = null,
    val players: List<GroupAdminTablePlayerDto> = emptyList(),
    val buyIns: List<GroupAdminTableTxDto> = emptyList(),
    val exits: List<GroupAdminTableTxDto> = emptyList()
)

data class UpdateMemberRoleRequest(val role: String)
data class AdminTableStatusRequest(val status: String? = null, val isDeleted: Boolean? = null)
data class AdminTablePlayerRequest(val name: String, val userId: String? = null)
data class AdminTableTxRequest(val playerId: String? = null, val amount: Long, val note: String? = null)
