package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FinTrackDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountType
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryType
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReconciliationIsolatedAdjustmentTest {

    private lateinit var database: FinTrackDatabase
    private lateinit var repository: ReconciliationRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ReconciliationRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `isolated balance reconciliation syncs account balance without creating fake expense or polluting reports`() = runTest {
        val accountId = database.accountDao().insert(
            AccountEntity(
                name = "بانک مهر ایران",
                type = AccountType.BANK,
                initialBalance = 10_000_000L, // 10,000,000 Rials
                color = 0xFF1B5E20.toInt(),
                icon = "account_balance"
            )
        )
        val expenseCategoryId = database.categoryDao().insert(
            CategoryEntity(
                name = "خوراک",
                type = CategoryType.EXPENSE,
                color = 0xFFE53935.toInt(),
                icon = "restaurant"
            )
        )
        val incomeCategoryId = database.categoryDao().insert(
            CategoryEntity(
                name = "حقوق",
                type = CategoryType.INCOME,
                color = 0xFF43A047.toInt(),
                icon = "payments"
            )
        )

        val t1 = 1_700_000_000_000L
        val t2 = t1 + 86_400_000L

        // Real income (+5,000,000 Rials) and real expense (-2,000,000 Rials) => app balance = 13,000,000 Rials
        database.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 5_000_000L,
                accountId = accountId,
                categoryId = incomeCategoryId,
                date = t1,
                note = "واریز حقوق"
            )
        )
        database.transactionDao().insert(
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 2_000_000L,
                accountId = accountId,
                categoryId = expenseCategoryId,
                date = t2,
                note = "خرید سوپرمارکت"
            )
        )

        // Bank SMS says balance is 12,350,000 Rials (650,000 Rials lower than app's 13,000,000 Rials)
        repository.recordSnapshot(
            accountId = accountId,
            smsDate = t2,
            balance = 12_350_000L,
            smsHash = "snap_hash_1"
        )

        val diagBefore = repository.getReconciliationDiagnosis(accountId, emptyList())
        assertNotNull(diagBefore)
        assertEquals(13_000_000L, diagBefore!!.appBalance)
        assertEquals(12_350_000L, diagBefore.bankBalance)
        assertEquals(-650_000L, diagBefore.diff)
        assertFalse(diagBefore.isSynced)

        // Perform isolated reconciliation for exact discrepancy (-650,000 Rials)
        val ok = repository.reconcileBalanceIsolated(
            accountId = accountId,
            signedDelta = diagBefore.diff,
            date = t2,
            note = "تراز با پیامک بانک"
        )
        assertTrue(ok)

        // 1. Discrepancy is now 0 and diagnosis is synced
        val diagAfter = repository.getReconciliationDiagnosis(accountId, emptyList())
        assertNotNull(diagAfter)
        assertEquals(12_350_000L, diagAfter!!.appBalance)
        assertEquals(12_350_000L, diagAfter.bankBalance)
        assertEquals(0L, diagAfter.diff)
        assertTrue(diagAfter.isSynced)
        assertEquals(1, diagAfter.adjustments.size)
        assertEquals(-650_000L, diagAfter.adjustments.first().amount)

        // 2. Account currentBalance reflects the reconciled balance in Rials
        val accountWithBalance = database.accountDao().getAccountWithBalance(accountId).first()
        assertNotNull(accountWithBalance)
        assertEquals(12_350_000L, accountWithBalance!!.currentBalance)
        assertEquals(2, accountWithBalance.transactionCount)

        // 3. CRITICAL: Categorized reports & total expense/income MUST NOT contain any fake expense/income!
        val totalExpense = database.transactionDao().getTotalExpense(0L, Long.MAX_VALUE).first() ?: 0L
        val totalIncome = database.transactionDao().getTotalIncome(0L, Long.MAX_VALUE).first() ?: 0L
        val expenseCategories = database.transactionDao()
            .getCategorySummaryRaw(0L, Long.MAX_VALUE, TransactionType.EXPENSE).first()
        val incomeCategories = database.transactionDao()
            .getCategorySummaryRaw(0L, Long.MAX_VALUE, TransactionType.INCOME).first()
        val allTransactions = database.transactionDao().getAll().first()

        assertEquals(2_000_000L, totalExpense)
        assertEquals(5_000_000L, totalIncome)
        assertEquals(1, expenseCategories.size)
        assertEquals(2_000_000L, expenseCategories.first().totalAmount)
        assertEquals(1, incomeCategories.size)
        assertEquals(5_000_000L, incomeCategories.first().totalAmount)
        assertEquals(2, allTransactions.size)
    }

    @Test
    fun `reconcileToTargetBankBalance handles positive adjustment and deleting adjustment reverts balance cleanly`() = runTest {
        val accountId = database.accountDao().insert(
            AccountEntity(
                name = "بانک ملت",
                type = AccountType.BANK,
                initialBalance = 4_000_000L,
                color = 0xFF1565C0.toInt(),
                icon = "account_balance"
            )
        )
        val t1 = 1_700_000_000_000L
        val t2 = t1 + 86_400_000L

        // Earlier snapshot at t1 was synced (4,000,000 Rials)
        repository.recordSnapshot(accountId, t1, 4_000_000L, "snap_t1")
        // Later snapshot at t2 shows bank has 4,850,000 Rials (+850,000 Rials discrepancy)
        repository.recordSnapshot(accountId, t2, 4_850_000L, "snap_t2")

        val reconciled = repository.reconcileToTargetBankBalance(
            accountId = accountId,
            targetBankBalance = 4_850_000L,
            date = t2,
            note = "تراز مستقیم"
        )
        assertTrue(reconciled)

        val diag = repository.getReconciliationDiagnosis(accountId, emptyList())!!
        assertTrue(diag.isSynced)
        assertEquals(4_850_000L, diag.appBalance)
        // Historical snapshot at t1 remains untouched at 4,000,000 because adjustment is dated t2
        val t1History = diag.history.first { it.snapshot.smsDate == t1 }
        assertEquals(4_000_000L, t1History.appBalance)
        assertEquals(0L, t1History.diff)

        // No income transaction was faked
        val totalIncome = database.transactionDao().getTotalIncome(0L, Long.MAX_VALUE).first() ?: 0L
        assertEquals(0L, totalIncome)

        // Deleting the adjustment restores pre-reconciliation balance cleanly
        val adjId = diag.adjustments.first().id
        repository.deleteBalanceAdjustment(adjId)
        val revertedBalance = database.accountDao().getAccountWithBalance(accountId).first()!!.currentBalance
        assertEquals(4_000_000L, revertedBalance)
    }

    @Test
    fun `migration 13 to 14 creates account_balance_adjustments table without losing existing accounts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_13_14.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `accounts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `initialBalance` INTEGER NOT NULL,
                            `color` INTEGER NOT NULL,
                            `icon` TEXT NOT NULL,
                            `cardNumber` TEXT,
                            `shabaNumber` TEXT,
                            `cardExpiry` TEXT,
                            `logoResName` TEXT,
                            `logoImage` TEXT,
                            `sortOrder` INTEGER NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `updatedAt` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO `accounts` (`id`, `name`, `type`, `initialBalance`, `color`, `icon`, `sortOrder`, `createdAt`, `updatedAt`)
                        VALUES (1, 'بانک تست', 'BANK', 5000000, 0, 'bank', 0, 1000, 1000)
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = helper.writableDatabase
        try {
            FinTrackDatabase.MIGRATION_13_14.migrate(sqliteDb)

            // Existing account is intact
            sqliteDb.query("SELECT name, initialBalance FROM accounts WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("بانک تست", c.getString(0))
                assertEquals(5_000_000L, c.getLong(1))
            }

            // New account_balance_adjustments table accepts records cleanly
            sqliteDb.execSQL("""
                INSERT INTO `account_balance_adjustments` (`accountId`, `amount`, `date`, `note`, `createdAt`)
                VALUES (1, -250000, 2000, 'تراز آزمایشی', 2000)
            """.trimIndent())
            sqliteDb.query("SELECT amount, note FROM account_balance_adjustments WHERE accountId = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(-250_000L, c.getLong(0))
                assertEquals("تراز آزمایشی", c.getString(1))
            }
        } finally {
            sqliteDb.close()
            helper.close()
            context.deleteDatabase(dbName)
        }
    }
}
