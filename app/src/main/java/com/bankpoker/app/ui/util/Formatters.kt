package com.bankpoker.app.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    /**
     * Format balance as +$X, -$X, or $0 with thousands separators
     */
    fun formatGroupBalance(balance: Long): String {
        val absFormatted = NumberFormat.getNumberInstance(Locale.US).format(abs(balance))
        return when {
            balance > 0 -> "+$$absFormatted"
            balance < 0 -> "-$$absFormatted"
            else -> "$0"
        }
    }

    /**
     * Format timestamp as "MMM dd, yyyy HH:mm"
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Format chips amount with optional dollar value
     */
    fun formatAmount(chips: Long, chipValue: Long?): String {
        return if (chipValue != null) {
            "$chips ($${chips * chipValue})"
        } else {
            "$chips"
        }
    }
}
