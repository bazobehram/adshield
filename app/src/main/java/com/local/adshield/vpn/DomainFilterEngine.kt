package com.local.adshield.vpn

import java.util.concurrent.atomic.AtomicReference

enum class DomainMatchResult {
    WHITELISTED,
    BLACKLISTED,
    NOT_MATCHED
}

/**
 * Microsecond in-memory domain filtering engine.
 * Employs reverse-label radix trie for O(labels) wildcard and subdomain matching,
 * combined with exact-match fast hash tables and atomic snapshot swaps.
 */
class DomainFilterEngine {

    private class TrieNode {
        val children = HashMap<String, TrieNode>()
        var isExactMatch: Boolean = false
        var isWildcardMatch: Boolean = false
    }

    private data class FilterSnapshot(
        val whitelistTrie: TrieNode = TrieNode(),
        val blacklistTrie: TrieNode = TrieNode(),
        val whitelistExact: Set<String> = emptySet(),
        val blacklistExact: Set<String> = emptySet(),
        val totalBlockedCount: Int = 0,
        val totalWhitelistedCount: Int = 0
    )

    private val snapshotRef = AtomicReference(FilterSnapshot())

    val totalBlockedCount: Int
        get() = snapshotRef.get().totalBlockedCount

    val totalWhitelistedCount: Int
        get() = snapshotRef.get().totalWhitelistedCount

    fun updateRules(
        whitelistDomains: Collection<String>,
        blacklistDomains: Collection<String>
    ) {
        val newWhitelistTrie = TrieNode()
        val newBlacklistTrie = TrieNode()
        val newWhitelistExact = HashSet<String>(whitelistDomains.size)
        val newBlacklistExact = HashSet<String>(blacklistDomains.size)

        for (domain in whitelistDomains) {
            val clean = normalizeDomain(domain)
            if (clean.isNotEmpty()) {
                newWhitelistExact.add(clean)
                insertIntoTrie(newWhitelistTrie, clean, isWildcard = true)
            }
        }

        for (domain in blacklistDomains) {
            val clean = normalizeDomain(domain)
            if (clean.isNotEmpty()) {
                newBlacklistExact.add(clean)
                insertIntoTrie(newBlacklistTrie, clean, isWildcard = true)
            }
        }

        snapshotRef.set(
            FilterSnapshot(
                whitelistTrie = newWhitelistTrie,
                blacklistTrie = newBlacklistTrie,
                whitelistExact = newWhitelistExact,
                blacklistExact = newBlacklistExact,
                totalBlockedCount = newBlacklistExact.size,
                totalWhitelistedCount = newWhitelistExact.size
            )
        )
    }

    fun checkDomain(domain: String): DomainMatchResult {
        val clean = normalizeDomain(domain)
        if (clean.isEmpty()) return DomainMatchResult.NOT_MATCHED

        val snapshot = snapshotRef.get()

        // 1. Whitelist fast exact check
        if (snapshot.whitelistExact.contains(clean)) {
            return DomainMatchResult.WHITELISTED
        }

        // 2. Whitelist trie check (subdomains & wildcard)
        if (matchesTrie(snapshot.whitelistTrie, clean)) {
            return DomainMatchResult.WHITELISTED
        }

        // 3. Blacklist fast exact check
        if (snapshot.blacklistExact.contains(clean)) {
            return DomainMatchResult.BLACKLISTED
        }

        // 4. Blacklist trie check (subdomains & wildcard)
        if (matchesTrie(snapshot.blacklistTrie, clean)) {
            return DomainMatchResult.BLACKLISTED
        }

        return DomainMatchResult.NOT_MATCHED
    }

    private fun insertIntoTrie(root: TrieNode, domain: String, isWildcard: Boolean) {
        val labels = domain.split('.').filter { it.isNotEmpty() }.reversed()
        if (labels.isEmpty()) return

        var curr = root
        for (label in labels) {
            curr = curr.children.getOrPut(label) { TrieNode() }
        }
        curr.isExactMatch = true
        if (isWildcard) {
            curr.isWildcardMatch = true
        }
    }

    private fun matchesTrie(root: TrieNode, domain: String): Boolean {
        val labels = domain.split('.').filter { it.isNotEmpty() }.reversed()
        if (labels.isEmpty()) return false

        var curr: TrieNode? = root
        for (i in labels.indices) {
            val label = labels[i]
            curr = curr?.children?.get(label) ?: return false

            if (curr.isWildcardMatch) {
                return true
            }
        }
        return curr?.isExactMatch == true
    }

    companion object {
        fun normalizeDomain(domain: String): String {
            return domain.trim()
                .lowercase()
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("*.")
                .removeSuffix(".")
        }
    }
}
