package org.ensodai.avalonmediacard.database

import org.jetbrains.exposed.v1.datetime.timestamp

object MediaDiscoverCacheTable : BaseUuidTable("media_discover_cache") {
    val cacheKey = varchar("cache_key", 255).uniqueIndex("media_discover_cache_cache_key_unique")
    val paramsHash = varchar("params_hash", 64).index("media_discover_cache_params_hash")
    val targetType = varchar("target_type", 20)
    val language = varchar("language", 10)
    val page = integer("page")
    val resultsJson = text("results_json")
    val expiresAt = timestamp("expires_at").index("media_discover_cache_expires_at")
}
