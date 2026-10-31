package com.local.adshield.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RuleType {
    WHITELIST,
    BLACKLIST
}

@Entity(
    tableName = "custom_rules",
    indices = [
        Index(value = ["domain"], unique = true),
        Index(value = ["type"])
    ]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val type: RuleType,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
