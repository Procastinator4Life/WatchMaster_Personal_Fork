package com.pranshulgg.watchmaster.data.repository

data class TraktEp(
    val s: Int,
    val n: Int,
    val d: String? = null
)

data class TraktDateEntry(
    val t: String,
    val id: Long,
    val s: Int? = null,
    val n: String? = null,
    val d: String,
    val eps: List<TraktEp>? = null
)

data class DateImportSummary(
    val movies: Int,
    val seasons: Int,
    val episodes: Int,
    val noEpisodes: Int,
    val notInApp: Int
)

data class SeasonPlan(
    val number: Int,
    val status: com.pranshulgg.watchmaster.core.model.WatchStatus
)

data class ProgressPlan(
    val seasons: List<SeasonPlan> = emptyList(),
    val skipReason: String? = null
)

// A real watch date, or null when it's missing or from before 1990
fun TraktDateEntry.realDate(): java.time.Instant? =
    runCatching { java.time.Instant.parse(d) }.getOrNull()
        ?.takeIf { it.isAfter(java.time.Instant.parse("1990-01-01T00:00:00Z")) }