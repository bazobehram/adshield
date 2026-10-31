package com.local.adshield.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.local.adshield.data.model.BlockedDomain

@Dao
interface BlockedDomainDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlockedDomain>)

    @Query("DELETE FROM blocked_domains WHERE sourceId = :sourceId")
    suspend fun deleteBySourceId(sourceId: Long)

    @Query("DELETE FROM blocked_domains")
    suspend fun clearAll()

    @Query("SELECT domain FROM blocked_domains")
    suspend fun getAllDomains(): List<String>

    @Query("SELECT COUNT(*) FROM blocked_domains")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM blocked_domains WHERE sourceId = :sourceId")
    suspend fun getCountBySourceId(sourceId: Long): Int
}
