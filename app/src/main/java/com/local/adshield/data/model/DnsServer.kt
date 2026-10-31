package com.local.adshield.data.model

data class DnsServer(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int = 53,
    val description: String
) {
    companion object {
        val CLOUDFLARE = DnsServer(
            id = "cloudflare",
            name = "Cloudflare",
            ip = "1.1.1.1",
            port = 53,
            description = "Fast, privacy-first DNS (1.1.1.1)"
        )
        val QUAD9 = DnsServer(
            id = "quad9",
            name = "Quad9",
            ip = "9.9.9.9",
            port = 53,
            description = "Security & threat blocking DNS (9.9.9.9)"
        )
        val GOOGLE = DnsServer(
            id = "google",
            name = "Google",
            ip = "8.8.8.8",
            port = 53,
            description = "High reliability global DNS (8.8.8.8)"
        )
        val ADGUARD = DnsServer(
            id = "adguard",
            name = "AdGuard DNS",
            ip = "94.140.14.14",
            port = 53,
            description = "Ad & tracker blocking default (94.140.14.14)"
        )

        val PRESETS = listOf(CLOUDFLARE, QUAD9, GOOGLE, ADGUARD)

        fun findOrDefault(ip: String): DnsServer {
            return PRESETS.find { it.ip == ip } ?: DnsServer(
                id = "custom",
                name = "Custom DNS",
                ip = ip,
                port = 53,
                description = "User-defined upstream ($ip)"
            )
        }
    }
}

data class VpnStats(
    val totalQueriesToday: Int = 0,
    val totalBlockedToday: Int = 0,
    val activeRulesCount: Int = 0
) {
    val blockPercentage: Float
        get() = if (totalQueriesToday > 0) {
            (totalBlockedToday.toFloat() / totalQueriesToday.toFloat()) * 100f
        } else {
            0f
        }
}
