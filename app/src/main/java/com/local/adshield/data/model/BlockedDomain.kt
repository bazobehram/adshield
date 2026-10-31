package com.local.adshield.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_domains",
    indices = [
        Index(value = ["sourceId"])
    ]
)
data class BlockedDomain(
    @PrimaryKey
    val domain: String,
    val sourceId: Long
)
