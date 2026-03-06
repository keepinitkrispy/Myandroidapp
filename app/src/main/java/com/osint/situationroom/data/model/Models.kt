package com.osint.situationroom.data.model

// ─── GDELT API Response Models ───────────────────────────────────────────────

data class GdeltArticle(
    val url: String = "",
    val title: String = "",
    val seendate: String = "",
    val domain: String = "",
    val language: String = "English",
    val sourcecountry: String? = null,
    val socialimage: String? = null
)

data class GdeltResponse(
    val articles: List<GdeltArticle> = emptyList()
)

data class GdeltTimelineDataPoint(
    val date: String = "",
    val value: Double = 0.0
)

data class GdeltTimelineData(
    val data: List<GdeltTimelineDataPoint> = emptyList()
)

data class GdeltTimelineResponse(
    val timeline: List<GdeltTimelineData> = emptyList()
)

// ─── Core Domain Models ───────────────────────────────────────────────────────

enum class Trajectory(val label: String, val symbol: String, val upward: Boolean?) {
    ESCALATING("ESCALATING", "↑", true),
    DE_ESCALATING("DE-ESCALATING", "↓", false),
    STABLE("STABLE", "→", null),
    VOLATILE("VOLATILE", "~", null)
}

enum class EventType(val label: String) {
    MILITARY_ACTION("Military"),
    DIPLOMATIC("Diplomatic"),
    ECONOMIC("Economic"),
    HUMANITARIAN("Humanitarian"),
    INTELLIGENCE("Intelligence"),
    NUCLEAR("Nuclear / WMD"),
    CYBER("Cyber"),
    INFORMATION_OPS("Info Ops"),
    UNKNOWN("Unknown")
}

data class ConflictZone(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val threatScore: Float,          // 0.0 – 10.0
    val description: String,
    val countries: List<String>,
    val activeParties: List<String>,
    val trajectory: Trajectory,
    val nuclearRisk: Boolean = false,
    val natoInvolvement: Boolean = false,
    val keyFacts: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class OsintEvent(
    val id: String,
    val title: String,
    val source: String,
    val sourceCountry: String?,
    val url: String,
    val timestamp: Long,
    val imageUrl: String?,
    val region: String,
    val eventType: EventType,
    val severity: Int,               // 1–10
    val keywords: List<String> = emptyList()
)

data class GlobalThreatAssessment(
    val overallScore: Float,         // 0.0 – 10.0
    val trajectory: Trajectory,
    val activeConflicts: Int,
    val criticalEvents24h: Int,
    val diplomaticActivity: Int,
    val militaryIncidents: Int,
    val nuclearAlertLevel: Int,      // 0–5 (Doomsday Clock analogy)
    val lastUpdated: Long,
    val confidence: Float,           // 0.0 – 1.0 (data confidence)
    val topThreats: List<String>,
    val assessmentBasis: List<String> // transparent methodology notes
)

data class TrajectoryPoint(
    val timestamp: Long,
    val score: Float,
    val eventCount: Int,
    val label: String
)

data class RegionalAlert(
    val region: String,
    val flag: String,
    val alertLevel: Int,             // 0–10
    val summary: String,
    val change24h: Float,            // delta
    val trajectory: Trajectory
)

data class RssItem(
    val title: String,
    val link: String,
    val pubDate: String,
    val description: String,
    val source: String
)

data class ThreatMetrics(
    val score: Float,
    val eventFrequencyScore: Float,
    val toneScore: Float,
    val geographicSpreadScore: Float,
    val eventTypeScore: Float,
    val trendScore: Float,
    val methodology: String
)
