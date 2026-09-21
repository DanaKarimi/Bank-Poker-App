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

data class CreateQuickTablePayload(
    val name: String? = null,
    val chipValue: Long? = null,
    val entryFee: Long? = null
)

data class BuyInExitPayload(
    val playerId: String? = null,
    val username: String? = null,
    val amount: Long? = null,
    val note: String? = null
)

data class DeletePlayerPayload(
    val playerId: String? = null
)

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

    private inline fun <reified T> safeParsePayload(op: OutboxRecord): Pair<T?, JsonObject?> {
        var rawJson: JsonObject? = null
        try {
            rawJson = gson.fromJson(op.payloadJson, JsonObject::class.java)
        } catch (e: Exception) {
            Log.e("SyncOutbox", "ERROR: Outbox payload invalid for op ${op.id}, type ${op.operationType}. Processing anyway.")
        }

        var typed: T? = null
        try {
            typed = gson.fromJson(op.payloadJson, T::class.java)
        } catch (e: Exception) {
            Log.e("SyncOutbox", "ERROR: Outbox payload invalid for op ${op.id}, type ${op.operationType}. Processing anyway.")
        }
        return Pair(typed, rawJson)
    }

    private suspend fun processOperation(op: OutboxRecord): Boolean {
        return when (op.operationType) {
            "CREATE_QUICK_TABLE" -> {
                val (typed, raw) = safeParsePayload<CreateQuickTablePayload>(op)
                val name = if (typed != null) {
                    if (typed.name.isNullOrBlank()) {
                        Log.w("SyncOutbox", "Warning: missing or empty 'name' for op ${op.id}, filling default")
                    }
                    typed.name ?: "Quick Table"
                } else {
                    raw?.get("name")?.asString ?: "Quick Table"
                }
                val chipValue = if (typed != null) {
                    typed.chipValue
                } else {
                    if (raw?.has("chipValue") == true && !raw.get("chipValue").isJsonNull) raw.get("chipValue").asLong else null
                }
                val entryFee = if (typed != null) {
                    typed.entryFee
                } else {
                    if (raw?.has("entryFee") == true && !raw.get("entryFee").isJsonNull) raw.get("entryFee").asLong else null
                }

                val result = remoteRepository.createQuickTable(
                    name = name,
                    chipValue = chipValue,
                    entryFee = entryFee,
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
                val (typed, raw) = safeParsePayload<BuyInExitPayload>(op)
                val playerId = if (typed != null) {
                    typed.playerId
                } else {
                    if (raw?.has("playerId") == true && !raw.get("playerId").isJsonNull) raw.get("playerId").asString else null
                }
                val username = if (typed != null) {
                    typed.username
                } else {
                    if (raw?.has("username") == true && !raw.get("username").isJsonNull) raw.get("username").asString else null
                }
                val amount = if (typed != null) {
                    if (typed.amount == null) {
                        Log.w("SyncOutbox", "Warning: null 'amount' for BUY_IN op ${op.id}, filling default 0")
                    }
                    typed.amount ?: 0L
                } else {
                    raw?.get("amount")?.asLong ?: 0L
                }
                val note = if (typed != null) {
                    typed.note
                } else {
                    if (raw?.has("note") == true && !raw.get("note").isJsonNull) raw.get("note").asString else null
                }
                if (playerId == null && username == null) {
                    Log.w("SyncOutbox", "Warning: both playerId and username missing for BUY_IN op ${op.id}")
                }

                val result = remoteRepository.directBuyIn(
                    tableId = op.targetId,
                    playerId = playerId,
                    username = username,
                    amount = amount,
                    note = note
                )
                result.isSuccess
            }
            "EXIT" -> {
                val (typed, raw) = safeParsePayload<BuyInExitPayload>(op)
                val playerId = if (typed != null) {
                    typed.playerId
                } else {
                    if (raw?.has("playerId") == true && !raw.get("playerId").isJsonNull) raw.get("playerId").asString else null
                }
                val username = if (typed != null) {
                    typed.username
                } else {
                    if (raw?.has("username") == true && !raw.get("username").isJsonNull) raw.get("username").asString else null
                }
                val amount = if (typed != null) {
                    if (typed.amount == null) {
                        Log.w("SyncOutbox", "Warning: null 'amount' for EXIT op ${op.id}, filling default 0")
                    }
                    typed.amount ?: 0L
                } else {
                    raw?.get("amount")?.asLong ?: 0L
                }
                val note = if (typed != null) {
                    typed.note
                } else {
                    if (raw?.has("note") == true && !raw.get("note").isJsonNull) raw.get("note").asString else null
                }
                if (playerId == null && username == null) {
                    Log.w("SyncOutbox", "Warning: both playerId and username missing for EXIT op ${op.id}")
                }

                val result = remoteRepository.directExit(
                    tableId = op.targetId,
                    playerId = playerId,
                    username = username,
                    amount = amount,
                    note = note
                )
                result.isSuccess
            }
            "DELETE_PLAYER" -> {
                val (typed, raw) = safeParsePayload<DeletePlayerPayload>(op)
                val playerId = if (typed != null) {
                    if (typed.playerId.isNullOrBlank()) {
                        Log.w("SyncOutbox", "Warning: missing playerId for DELETE_PLAYER op ${op.id}")
                    }
                    typed.playerId ?: ""
                } else {
                    raw?.get("playerId")?.asString ?: ""
                }
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
