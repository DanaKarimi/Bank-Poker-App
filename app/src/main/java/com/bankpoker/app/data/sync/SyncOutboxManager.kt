package com.bankpoker.app.data.sync

import android.content.Context
import android.util.Log
import com.bankpoker.app.data.local.entity.OutboxRecord
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncOutboxManager(
    private val context: Context,
    private val repository: PokerRepository,
    private val remoteRepository: RemoteRepository
) {
    private val gson = Gson()

    suspend fun flush(): Int = withContext(Dispatchers.IO) {
        val pending = repository.getPendingOutbox()
        if (pending.isEmpty()) return@withContext 0

        Log.d("SyncOutbox", "Flushing ${pending.size} outbox operations")
        var successCount = 0

        for (op in pending) {
            try {
                val success = processOperation(op)
                if (success) {
                    repository.removeOutboxOperation(op.id)
                    successCount++
                } else {
                    repository.updateOutboxOperation(
                        op.copy(attempts = op.attempts + 1, lastError = "Server rejected operation")
                    )
                }
            } catch (e: Exception) {
                Log.e("SyncOutbox", "Failed to process op: ${op.id} (${op.operationType})", e)
                repository.updateOutboxOperation(
                    op.copy(attempts = op.attempts + 1, lastError = e.message)
                )
                // Stop processing on network failure so FIFO order is preserved
                break
            }
        }
        return@withContext successCount
    }

    private suspend fun processOperation(op: OutboxRecord): Boolean {
        return when (op.operationType) {
            "CREATE_QUICK_TABLE" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val result = remoteRepository.createQuickTable(
                    name = payload.get("name")?.asString ?: "Quick Table",
                    chipValue = if (payload.has("chipValue") && !payload.get("chipValue").isJsonNull) payload.get("chipValue").asLong else null,
                    entryFee = if (payload.has("entryFee") && !payload.get("entryFee").isJsonNull) payload.get("entryFee").asLong else null,
                    playerNames = emptyList()
                )
                if (result.isSuccess) {
                    val resObj = result.getOrNull()
                    val code = resObj?.getAsJsonObject("table")?.get("code")?.asString
                    if (!code.isNullOrBlank()) {
                        repository.updateTableCode(op.targetId, code, System.currentTimeMillis())
                    }
                    true
                } else false
            }
            "PUBLISH_TABLE" -> {
                val result = remoteRepository.publishTable(op.targetId)
                if (result.isSuccess) {
                    val code = result.getOrNull()?.get("code")?.asString
                    val pubAt = result.getOrNull()?.get("published_at")?.asLong ?: System.currentTimeMillis()
                    if (!code.isNullOrBlank()) {
                        repository.updateTableCode(op.targetId, code, pubAt)
                    }
                    true
                } else false
            }
            "BUY_IN" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val result = remoteRepository.directBuyIn(
                    tableId = op.targetId,
                    playerId = if (payload.has("playerId") && !payload.get("playerId").isJsonNull) payload.get("playerId").asString else null,
                    username = if (payload.has("username") && !payload.get("username").isJsonNull) payload.get("username").asString else null,
                    amount = payload.get("amount")?.asLong ?: 0L,
                    note = if (payload.has("note") && !payload.get("note").isJsonNull) payload.get("note").asString else null
                )
                result.isSuccess
            }
            "EXIT" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val result = remoteRepository.directExit(
                    tableId = op.targetId,
                    playerId = if (payload.has("playerId") && !payload.get("playerId").isJsonNull) payload.get("playerId").asString else null,
                    username = if (payload.has("username") && !payload.get("username").isJsonNull) payload.get("username").asString else null,
                    amount = payload.get("amount")?.asLong ?: 0L,
                    note = if (payload.has("note") && !payload.get("note").isJsonNull) payload.get("note").asString else null
                )
                result.isSuccess
            }
            "DELETE_PLAYER" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val playerId = payload.get("playerId")?.asString ?: ""
                val result = remoteRepository.deleteTablePlayer(op.targetId, playerId)
                result.isSuccess
            }
            "CLOSE_TABLE" -> {
                val result = remoteRepository.closeTable(op.targetId)
                result.isSuccess
            }
            else -> {
                Log.w("SyncOutbox", "Unknown operation type: ${op.operationType}")
                true
            }
        }
    }
}
