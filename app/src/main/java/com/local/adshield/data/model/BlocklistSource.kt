package com.local.adshield.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocklist_sources")
data class BlocklistSource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val last_synced: Long? = null,
    val ruleCount: Int = 0
)
