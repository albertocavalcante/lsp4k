package io.lsp4k.protocol

/** An allow-listed violation [key], the gap that explains it and why it is accepted for now. */
internal data class AllowListEntry(
    val key: String,
    val gap: String,
    val reason: String,
)

/** Violations missing from the allow-list ([unexpected]) and allow-list entries no violation needs ([stale]). */
internal data class Ratchet(
    val unexpected: Set<String>,
    val stale: Set<String>,
)

internal fun ratchet(
    violations: Set<String>,
    allowed: Set<String>,
): Ratchet = Ratchet(unexpected = violations - allowed, stale = allowed - violations)

/**
 * Parser for the ratcheting allow-list of the meta-model conformance test.
 *
 * Entries are grouped under a gap header `[LP-<n>] <title>`, one violation key per line followed by its reason.
 * Blank lines and lines starting with `#` are ignored. A key may appear once, a gap header once, and every header
 * needs at least one entry, so the file cannot keep empty or scattered groups.
 */
internal object ConformanceAllowList {
    private val gapHeader = Regex("""^\[(LP-\d+)]\s+\S.*$""")
    private val entry = Regex("""^(\S+)\s+(\S.*)$""")

    fun parse(text: String): List<AllowListEntry> {
        val entries = mutableListOf<AllowListEntry>()
        val gaps = mutableListOf<String>()
        text.lines().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val header = gapHeader.matchEntire(line)
            if (header != null) {
                val gap = header.groupValues[1]
                require(gap !in gaps) { "Line ${index + 1}: gap $gap has a second header; keep its entries in one group" }
                gaps += gap
                return@forEachIndexed
            }
            val match = requireNotNull(entry.matchEntire(line)) { "Line ${index + 1}: expected '<violation key> <reason>'" }
            val gap = requireNotNull(gaps.lastOrNull()) { "Line ${index + 1}: entry before any [LP-<n>] gap header" }
            val key = match.groupValues[1]
            require(entries.none { it.key == key }) { "Line ${index + 1}: duplicate key $key" }
            entries += AllowListEntry(key, gap, match.groupValues[2])
        }
        val empty = gaps - entries.map { it.gap }.toSet()
        require(empty.isEmpty()) { "Gap headers without entries: $empty" }
        return entries
    }
}
