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

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5

        fun isPermanentClientError(message: String?): Boolean {
            if (message.isNullOrBlank()) return false
            val m = message.lowercase()
            return m.contains("400") ||
                m.contains("403") ||
                m.contains("404") ||
                m.contains("bad request") ||
                m.contains("forbidden") ||
                m.contains("not found") ||
                m.contains("permission denied") ||
                m.contains("table is closed")
        }
    }

    suspend fun flush(): Int = withContext(Dispatchers.IO) {
        val pending = repository.getPendingOutbox()
        if (pending.isEmpty()) return@withContext 0

        Log.d("SyncOutbox", "Flushing ${pending.size} outbox operations")
        var successCount = 0

        for (op in pending) {
            if (op.attempts >= MAX_RETRY_ATTEMPTS) {
                Log.w("SyncOutbox", "Retiring outbox op ${op.id} (${op.operationType}) after ${op.attempts} failed attempts")
                repository.updateOutboxOperation(op.copy(status = "FAILED"))
                continue
            }
            try {
                val opResult = processOperation(op)
                if (opResult.isSuccess) {
                    repository.removeOutboxOperation(op.id)
                    successCount++
                } else {
                    val errMsg = opResult.exceptionOrNull()?.message ?: "Server rejected operation"
                    val nextAttempts = op.attempts + 1
                    val permanent = isPermanentClientError(errMsg)
                    if (permanent || nextAttempts >= MAX_RETRY_ATTEMPTS) {
                        Log.w("SyncOutbox", "Stopping retries for op ${op.id} (${op.operationType}): permanent=$permanent, attempts=$nextAttempts, err=$errMsg")
                        repository.updateOutboxOperation(
                            op.copy(attempts = nextAttempts, status = "FAILED", lastError = errMsg)
                        )
                    } else {
                        repository.updateOutboxOperation(
                            op.copy(attempts = nextAttempts, lastError = errMsg)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SyncOutbox", "Failed to process op: ${op.id} (${op.operationType})", e)
                val nextAttempts = op.attempts + 1
                val permanent = isPermanentClientError(e.message)
                repository.updateOutboxOperation(
                    op.copy(
                        attempts = nextAttempts,
                        status = if (permanent || nextAttempts >= MAX_RETRY_ATTEMPTS) "FAILED" else op.status,
                        lastError = e.message
                    )
                )
                if (!permanent) {
                    // Stop processing on transient network failure so FIFO order is preserved
                    break
                }
            }
        }
        return@withContext successCount
    }

    private suspend fun processOperation(op: OutboxRecord): Result<Unit> {
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
                    Result.success(Unit)
                } else Result.failure(result.exceptionOrNull() ?: Exception("createQuickTable failed"))
            }
            "PUBLISH_TABLE" -> {
                val result = remoteRepository.publishTable(op.targetId)
                if (result.isSuccess) {
                    val code = result.getOrNull()?.get("code")?.asString
                    val pubAt = result.getOrNull()?.get("published_at")?.asLong ?: System.currentTimeMillis()
                    if (!code.isNullOrBlank()) {
                        repository.updateTableCode(op.targetId, code, pubAt)
                    }
                    Result.success(Unit)
                } else Result.failure(result.exceptionOrNull() ?: Exception("publishTable failed"))
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
                result.map { Unit }
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
                result.map { Unit }
            }
            "DELETE_PLAYER" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val playerId = payload.get("playerId")?.asString ?: ""
                val result = remoteRepository.deleteTablePlayer(op.targetId, playerId)
                result.map { Unit }
            }
            "EDIT_EXIT" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val tableId = if (payload.has("tableId") && !payload.get("tableId").isJsonNull) payload.get("tableId").asString else ""
                val amount = payload.get("amount")?.asLong ?: 0L
                val note = if (payload.has("note") && !payload.get("note").isJsonNull) payload.get("note").asString else null
                val result = remoteRepository.updateExit(tableId, op.targetId, amount, note)
                result.map { Unit }
            }
            "EDIT_BUY_IN" -> {
                val payload = gson.fromJson(op.payloadJson, JsonObject::class.java)
                val tableId = if (payload.has("tableId") && !payload.get("tableId").isJsonNull) payload.get("tableId").asString else ""
                val amount = payload.get("amount")?.asLong ?: 0L
                val note = if (payload.has("note") && !payload.get("note").isJsonNull) payload.get("note").asString else null
                val result = remoteRepository.updateBuyIn(tableId, op.targetId, amount, note)
                result.map { Unit }
            }
            "CLOSE_TABLE" -> {
                val result = remoteRepository.closeTable(op.targetId)
                result.map { Unit }
            }
            else -> {
                Log.w("SyncOutbox", "Unknown operation type: ${op.operationType}")
                Result.success(Unit)
            }
        }
    }
}
