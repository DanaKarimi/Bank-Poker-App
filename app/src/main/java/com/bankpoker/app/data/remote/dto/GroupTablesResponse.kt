package com.bankpoker.app.data.remote.dto

import com.bankpoker.app.data.local.entity.PokerTable
import com.google.gson.annotations.SerializedName

data class GroupTablesResponse(
    @SerializedName("tables")
    val tables: List<GroupTableItemDto> = emptyList()
)

data class GroupTableItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("groupId") val groupId: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("chipValue") val chipValue: Long? = null,
    @SerializedName("status") val status: String = "ACTIVE",
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("hasEntryFee") val hasEntryFee: Boolean = false,
    @SerializedName("entryFee") val entryFee: Long? = null,
    @SerializedName("myEntryFeePaid") val myEntryFeePaid: Boolean? = null,
    @SerializedName("paidCount") val paidCount: Int? = null,
    @SerializedName("seatedCount") val seatedCount: Int? = null,
    @SerializedName("isHostOrAdmin") val isHostOrAdmin: Boolean = false,
    @SerializedName("canManage") val canManage: Boolean = false,
    @SerializedName("hasJoinedTable") val hasJoinedTable: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("closedAt") val closedAt: Long? = null,
    @SerializedName("playerCount") val playerCount: Int = 0
)

data class GroupTableItem(
    val id: String,
    val name: String,
    val chipValue: Long? = null,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val groupId: String? = null,
    val hasEntryFee: Boolean = false,
    val entryFee: Long? = null,
    val myEntryFeePaid: Boolean? = null,
    val paidCount: Int? = null,
    val seatedCount: Int? = null,
    val isHostOrAdmin: Boolean = false,
    val canManage: Boolean = false,
    val playerCount: Int = 0
)

fun GroupTableItemDto.toGroupTableItem(): GroupTableItem {
    return GroupTableItem(
        id = id,
        name = name,
        chipValue = chipValue,
        status = status,
        createdAt = createdAt ?: System.currentTimeMillis(),
        closedAt = closedAt,
        groupId = groupId,
        hasEntryFee = hasEntryFee || (entryFee != null && entryFee > 0),
        entryFee = entryFee,
        myEntryFeePaid = myEntryFeePaid,
        paidCount = paidCount,
        seatedCount = seatedCount ?: playerCount,
        isHostOrAdmin = isHostOrAdmin || canManage,
        canManage = canManage || isHostOrAdmin,
        playerCount = playerCount
    )
}

fun GroupTableItem.toPokerTable(): PokerTable {
    return PokerTable(
        id = id,
        name = name,
        chipValue = chipValue,
        status = status,
        createdAt = createdAt,
        closedAt = closedAt,
        groupId = groupId,
        hasEntryFee = hasEntryFee,
        entryFee = entryFee
    )
}

fun PokerTable.toGroupTableItem(
    myEntryFeePaid: Boolean? = null,
    paidCount: Int? = null,
    seatedCount: Int? = null,
    isHostOrAdmin: Boolean = false,
    canManage: Boolean = false,
    playerCount: Int = 0
): GroupTableItem {
    return GroupTableItem(
        id = id,
        name = name,
        chipValue = chipValue,
        status = status,
        createdAt = createdAt,
        closedAt = closedAt,
        groupId = groupId,
        hasEntryFee = hasEntryFee,
        entryFee = entryFee,
        myEntryFeePaid = myEntryFeePaid,
        paidCount = paidCount,
        seatedCount = seatedCount ?: playerCount,
        isHostOrAdmin = isHostOrAdmin,
        canManage = canManage,
        playerCount = playerCount
    )
}
