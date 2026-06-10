package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a SIM Card.
 */
@Entity(tableName = "sim_cards")
data class SimCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val operator: String, // e.g., "Giffgaff UK", "CTExcel UK", "China Mobile", etc.
    val cardType: String, // e.g., "Physical SIM", "eSIM"
    val activationDate: Long, // timestamp
    val planDetails: String, // plan description
    val balance: Double = 0.0, // current credit/balance reference
    val currency: String = "GBP", // Currency code: GBP, USD, CNY, etc.
    val notes: String = "",
    val isActive: Boolean = true
)

/**
 * Entity representing Active Keeping-alive (保号) Rules.
 * Each SIM Card can have multiple rules assigned, or a custom one.
 */
@Entity(tableName = "keep_alive_rules")
data class KeepAliveRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val simId: Int, // Associated SIM card id
    val ruleName: String, // e.g., "180-Day Active Action", "CTExcel £1 Plan Auto-renew"
    val isEnabled: Boolean = true,
    val periodDays: Int = 180, // cycle of active behavior needed (e.g. 180 days)
    val warningDaysAdvancePrimary: Int = 30, // primary warning threshold (e.g. 30 days before risk)
    val warningDaysAdvanceSecondary: Int = 5, // critical threshold (e.g. 5 days before risk)
    val minimumSpend: Double = 0.0,
    val requiredAction: String = "Any Activity", // "Call", "SMS", "Data", "Top-up", "Balance Change", "Any Activity"
    val lastActiveDate: Long, // Timestamp of last confirmed keeper action
    val isPredefined: Boolean = false, // True if using Giffgaff/CTExcel system default templates
    val notes: String = ""
)

/**
 * Entity representing consumption history, activity history, and notification events.
 */
@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val simId: Int,
    val type: String, // "SMS", "Call", "Data", "Top-up", "Rule Checked", "Alert Sent", "Service Fee"
    val amount: Double = 0.0, // financial spend or recharge limit
    val timestamp: Long = System.currentTimeMillis(),
    val description: String
)
