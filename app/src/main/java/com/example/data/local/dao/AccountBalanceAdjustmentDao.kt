package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AccountBalanceAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountBalanceAdjustmentDao {

    @Query("SELECT * FROM account_balance_adjustments ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<AccountBalanceAdjustmentEntity>>

    @Query("SELECT * FROM account_balance_adjustments ORDER BY date DESC, id DESC")
    suspend fun getAllList(): List<AccountBalanceAdjustmentEntity>

    @Query("SELECT * FROM account_balance_adjustments WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    fun getByAccountId(accountId: Long): Flow<List<AccountBalanceAdjustmentEntity>>

    @Query("SELECT * FROM account_balance_adjustments WHERE accountId = :accountId ORDER BY date DESC, id DESC")
    suspend fun getListByAccountId(accountId: Long): List<AccountBalanceAdjustmentEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM account_balance_adjustments WHERE accountId = :accountId")
    suspend fun getTotalAdjustmentForAccount(accountId: Long): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM account_balance_adjustments WHERE accountId = :accountId AND date <= :date")
    suspend fun getTotalAdjustmentUpToDate(accountId: Long, date: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(adjustment: AccountBalanceAdjustmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(adjustments: List<AccountBalanceAdjustmentEntity>)

    @Delete
    suspend fun delete(adjustment: AccountBalanceAdjustmentEntity)

    @Query("DELETE FROM account_balance_adjustments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM account_balance_adjustments WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: Long)

    @Query("DELETE FROM account_balance_adjustments")
    suspend fun deleteAll()
}
