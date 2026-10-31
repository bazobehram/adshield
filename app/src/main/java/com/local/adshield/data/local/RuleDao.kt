package com.local.adshield.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.local.adshield.data.model.RuleEntity
import com.local.adshield.data.model.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM custom_rules WHERE type = :type ORDER BY createdAt DESC")
    fun getRulesByType(type: RuleType): Flow<List<RuleEntity>>

    @Query("SELECT * FROM custom_rules WHERE enabled = 1")
    suspend fun getAllEnabledRules(): List<RuleEntity>

    @Query("SELECT * FROM custom_rules WHERE domain = :domain LIMIT 1")
    suspend fun findByDomain(domain: String): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity): Long

    @Update
    suspend fun update(rule: RuleEntity)

    @Delete
    suspend fun delete(rule: RuleEntity)

    @Query("DELETE FROM custom_rules WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("SELECT COUNT(*) FROM custom_rules WHERE enabled = 1")
    fun getEnabledCount(): Flow<Int>
}
