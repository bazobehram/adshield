package com.local.adshield.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.QueryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(queryLog: QueryLogEntity): Long

    @Query("SELECT * FROM query_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 150): Flow<List<QueryLogEntity>>

    @Query(
        """
        SELECT * FROM query_logs 
        WHERE (:domainQuery IS NULL OR domain LIKE '%' || :domainQuery || '%')
          AND (:statusFilter IS NULL OR status = :statusFilter)
        ORDER BY timestamp DESC 
        LIMIT :limit
        """
    )
    fun getFilteredLogs(
        domainQuery: String?,
        statusFilter: QueryStatus?,
        limit: Int = 150
    ): Flow<List<QueryLogEntity>>

    @Query("SELECT COUNT(*) FROM query_logs WHERE timestamp >= :sinceTimestamp")
    fun getTotalCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM query_logs WHERE status = 'BLOCKED' AND timestamp >= :sinceTimestamp")
    fun getBlockedCountSince(sinceTimestamp: Long): Flow<Int>

    @Query("DELETE FROM query_logs")
    suspend fun clearAll()

    @Query("DELETE FROM query_logs WHERE id NOT IN (SELECT id FROM query_logs ORDER BY timestamp DESC LIMIT :retainCount)")
    suspend fun trimOldLogs(retainCount: Int = 5000)
}
