package com.bankpoker.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NotificationPayload(
    val title: String? = null,
    val message: String? = null,
    val targetId: String? = null,
    val targetType: String? = null,
    val groupId: String? = null,
    val tableId: String? = null,
    val requestId: String? = null
)

data class NotificationItemDto(
    val id: String,
    @SerializedName("user_id")
    val userId: String? = null,
    val type: String,
    val payload: NotificationPayload? = null,
    val count: Int? = 1,
    val read: Boolean = false,
    @SerializedName("created_at")
    val createdAt: Long? = null,
    @SerializedName("updated_at")
    val updatedAt: Long? = null
)

data class NotificationListResponse(
    val notifications: List<NotificationItemDto> = emptyList(),
    val unreadCount: Int = 0
)

data class NotificationSettingsResponse(
    val settings: Map<String, Boolean> = emptyMap()
)
