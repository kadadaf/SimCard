package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SimDao {
    @Query("SELECT * FROM sim_cards ORDER BY id DESC")
    fun getAllSimCards(): Flow<List<SimCard>>

    @Query("SELECT * FROM sim_cards WHERE id = :id")
    suspend fun getSimCardById(id: Int): SimCard?

    @Query("SELECT * FROM sim_cards WHERE id = :id")
    fun getSimCardByIdFlow(id: Int): Flow<SimCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimCard(simCard: SimCard): Long

    @Update
    suspend fun updateSimCard(simCard: SimCard)

    @Delete
    suspend fun deleteSimCard(simCard: SimCard)
}

@Dao
interface KeepAliveRuleDao {
    @Query("SELECT * FROM keep_alive_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<KeepAliveRule>>

    @Query("SELECT * FROM keep_alive_rules WHERE simId = :simId")
    fun getRulesForSim(simId: Int): Flow<List<KeepAliveRule>>

    @Query("SELECT * FROM keep_alive_rules WHERE simId = :simId")
    suspend fun getRulesForSimList(simId: Int): List<KeepAliveRule>

    @Query("SELECT * FROM keep_alive_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<KeepAliveRule>

    @Query("SELECT * FROM keep_alive_rules WHERE id = :id")
    suspend fun getRuleById(id: Int): KeepAliveRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: KeepAliveRule): Long

    @Update
    suspend fun updateRule(rule: KeepAliveRule)

    @Delete
    suspend fun deleteRule(rule: KeepAliveRule)

    @Query("DELETE FROM keep_alive_rules WHERE simId = :simId")
    suspend fun deleteRulesBySimId(simId: Int)
}

@Dao
interface HistoryRecordDao {
    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE simId = :simId ORDER BY timestamp DESC")
    fun getHistoryForSim(simId: Int): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records WHERE simId = :simId ORDER BY timestamp DESC")
    suspend fun getHistoryForSimList(simId: Int): List<HistoryRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HistoryRecord): Long

    @Delete
    suspend fun deleteRecord(record: HistoryRecord)

    @Query("DELETE FROM history_records WHERE simId = :simId")
    suspend fun deleteHistoryBySimId(simId: Int)
}
