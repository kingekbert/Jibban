package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Isolated balance reconciliation adjustment record for an account.
 *
 * Unlike [TransactionEntity], rows in this table only affect the account's calculated balance
 * (current balance and balance-at-date snapshots) and are strictly excluded from categorized
 * expense/income reports, charts, budgets, and transaction totals.
 *
 * [amount] is a signed delta in Rials:
 * - Positive (> 0) increases app balance toward a higher bank balance.
 * - Negative (< 0) decreases app balance toward a lower bank balance.
 */
@Entity(
    tableName = "account_balance_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "date"])
    ]
)
data class AccountBalanceAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val amount: Long,
    val date: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
