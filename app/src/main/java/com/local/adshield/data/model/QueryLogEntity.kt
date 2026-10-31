package com.local.adshield.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QueryStatus {
    ALLOWED,
    BLOCKED
}

@Entity(
    tableName = "query_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["domain"]),
        Index(value = ["status"])
    ]
)
data class QueryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: QueryStatus,
    val queryType: String = "A",
    val uid: Int? = null,
    val latencyMs: Long = 0,
    val clientIp: String = "10.0.0.2",
    val ruleMatched: String? = null
)
