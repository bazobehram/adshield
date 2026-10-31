package com.local.adshield.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.local.adshield.data.model.BlocklistSource
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistSourceDao {
    @Query("SELECT * FROM blocklist_sources ORDER BY id ASC")
    fun getAllSources(): Flow<List<BlocklistSource>>

    @Query("SELECT * FROM blocklist_sources WHERE enabled = 1")
    suspend fun getEnabledSources(): List<BlocklistSource>

    @Query("SELECT * FROM blocklist_sources WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BlocklistSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: BlocklistSource): Long

    @Update
    suspend fun update(source: BlocklistSource)

    @Delete
    suspend fun delete(source: BlocklistSource)

    @Query("SELECT COUNT(*) FROM blocklist_sources")
    suspend fun count(): Int
}
