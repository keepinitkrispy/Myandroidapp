package com.osint.situationroom.data.repository

import android.util.Log
import com.osint.situationroom.data.api.NetworkModule
import com.osint.situationroom.data.model.*
import com.osint.situationroom.domain.ConflictZoneData
import com.osint.situationroom.domain.ThreatCalculator
import com.osint.situationroom.domain.TrajectoryAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class OsintRepository {

    private val TAG = "OsintRepository"
    private val gdeltApi = NetworkModule.gdeltApi

    // Cached state
    private var cachedAssessment: GlobalThreatAssessment? = null
    private var cachedEvents: List<OsintEvent> = emptyList()
    private var cachedTimeline: List<TrajectoryPoint> = emptyList()
    private var lastFetch: Long = 0L
    private val cacheTtlMs = 5 * 60 * 1000L // 5 min

    // GDELT conflict search terms — factual, not editorialized
    private val conflictQueries = listOf(
        "war military offensive attack",
        "nuclear missile launch test",
        "NATO troops deploy",
        "ceasefire peace negotiations",
        "sanctions economic warfare",
        "cyber attack infrastructure",
        "naval fleet warship",
        "ballistic missile intercept"
    )

    suspend fun getGlobalAssessment(): Result<GlobalThreatAssessment> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            if (cachedAssessment != null && (now - lastFetch) < cacheTtlMs) {
                return@withContext Result.success(cachedAssessment!!)
            }

            coroutineScope {
                val gdeltJob = async { fetchGdeltEvents("war conflict military attack ceasefire") }
                val timelineJob = async { fetchTimeline("war military conflict") }

                val gdeltResult = gdeltJob.await()
                val timelineResult = timelineJob.await()

                val events = gdeltResult.getOrElse { emptyList() }
                val timeline = timelineResult.getOrElse { emptyList() }

                val assessment = ThreatCalculator.compute(
                    articles = events,
                    timeline = timeline,
                    conflictZones = ConflictZoneData.zones
                )

                cachedAssessment = assessment
                cachedTimeline = TrajectoryAnalyzer.buildTrajectory(timeline)
                lastFetch = now
                Result.success(assessment)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Assessment fetch failed: ${e.message}")
            // Return offline assessment based on static conflict zone data
            Result.success(ThreatCalculator.offlineAssessment(ConflictZoneData.zones))
        }
    }

    suspend fun getOsintEvents(query: String = "war military nuclear", timespan: String = "24h"): Result<List<OsintEvent>> =
        withContext(Dispatchers.IO) {
            try {
                val gdeltResult = fetchGdeltEvents(query, timespan)
                val articles = gdeltResult.getOrElse { emptyList() }

                val events = articles.mapIndexed { idx, article ->
                    val fmt = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US)
                    val ts = try { fmt.parse(article.seendate)?.time ?: System.currentTimeMillis() }
                            catch (e: Exception) { System.currentTimeMillis() - idx * 60_000L }

                    OsintEvent(
                        id = "gdelt_$idx",
                        title = article.title,
                        source = article.domain,
                        sourceCountry = article.sourcecountry,
                        url = article.url,
                        timestamp = ts,
                        imageUrl = article.socialimage,
                        region = inferRegion(article.title + " " + article.sourcecountry),
                        eventType = inferEventType(article.title),
                        severity = inferSeverity(article.title),
                        keywords = extractKeywords(article.title)
                    )
                }.sortedByDescending { it.timestamp }

                cachedEvents = events
                Result.success(events)
            } catch (e: Exception) {
                Log.e(TAG, "Events fetch failed: ${e.message}")
                if (cachedEvents.isNotEmpty()) Result.success(cachedEvents)
                else Result.failure(e)
            }
        }

    suspend fun getTrajectoryTimeline(): Result<List<TrajectoryPoint>> = withContext(Dispatchers.IO) {
        try {
            if (cachedTimeline.isNotEmpty()) return@withContext Result.success(cachedTimeline)
            val timeline = fetchTimeline("war military nuclear conflict").getOrElse { emptyList() }
            val points = TrajectoryAnalyzer.buildTrajectory(timeline)
            cachedTimeline = points
            Result.success(points)
        } catch (e: Exception) {
            Result.success(TrajectoryAnalyzer.syntheticBaseline())
        }
    }

    fun getConflictZones(): List<ConflictZone> = ConflictZoneData.zones

    fun getRegionalAlerts(): List<RegionalAlert> = ConflictZoneData.regionalAlerts

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private suspend fun fetchGdeltEvents(
        query: String,
        timespan: String = "24h"
    ): Result<List<com.osint.situationroom.data.model.GdeltArticle>> = try {
        val response = gdeltApi.searchArticles(query = query, timespan = timespan)
        Result.success(response.articles)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun fetchTimeline(query: String): Result<List<GdeltTimelineDataPoint>> = try {
        val response = gdeltApi.getTimeline(query = query)
        val points = response.timeline.firstOrNull()?.data ?: emptyList()
        Result.success(points)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun inferRegion(text: String): String {
        val t = text.lowercase()
        return when {
            t.containsAny("ukraine", "russia", "kyiv", "moscow", "donbas", "crimea") -> "Eastern Europe"
            t.containsAny("gaza", "israel", "hamas", "hezbollah", "lebanon", "west bank") -> "Middle East"
            t.containsAny("taiwan", "china", "beijing", "pla", "strait") -> "Indo-Pacific"
            t.containsAny("north korea", "dprk", "pyongyang", "kim") -> "Korean Peninsula"
            t.containsAny("iran", "tehran", "nuclear deal", "iaea") -> "Persian Gulf"
            t.containsAny("sudan", "ethiopia", "sahel", "mali", "niger", "somalia") -> "Sub-Saharan Africa"
            t.containsAny("nato", "alliance", "article 5") -> "NATO Region"
            t.containsAny("south china sea", "philippines", "spratly") -> "South China Sea"
            t.containsAny("pakistan", "india", "kashmir", "loc") -> "South Asia"
            t.containsAny("myanmar", "burma", "junta") -> "Southeast Asia"
            t.containsAny("venezuela", "colombia", "cartel") -> "Latin America"
            t.containsAny("arctic", "svalbard", "greenland") -> "Arctic"
            else -> "Global"
        }
    }

    private fun inferEventType(title: String): EventType {
        val t = title.lowercase()
        return when {
            t.containsAny("nuclear", "nuke", "icbm", "warhead", "wmd", "radiation") -> EventType.NUCLEAR
            t.containsAny("cyber", "hack", "ransomware", "malware", "infrastructure attack") -> EventType.CYBER
            t.containsAny("talks", "negotiation", "summit", "treaty", "diplomat", "ceasefire", "peace") -> EventType.DIPLOMATIC
            t.containsAny("sanction", "trade war", "embargo", "tariff", "economic war") -> EventType.ECONOMIC
            t.containsAny("airstrike", "missile", "attack", "offensive", "troops", "military", "navy", "tank", "drone") -> EventType.MILITARY_ACTION
            t.containsAny("refugee", "civilian", "hospital", "humanitarian", "aid") -> EventType.HUMANITARIAN
            t.containsAny("intel", "spy", "cia", "fsb", "mossad", "intercept", "surveillance") -> EventType.INTELLIGENCE
            t.containsAny("propaganda", "disinformation", "psyop", "information") -> EventType.INFORMATION_OPS
            else -> EventType.UNKNOWN
        }
    }

    private fun inferSeverity(title: String): Int {
        val t = title.lowercase()
        var score = 3
        if (t.containsAny("nuclear", "nuke", "wmd")) score += 4
        if (t.containsAny("war", "invasion", "offensive")) score += 2
        if (t.containsAny("missile", "airstrike", "attack")) score += 2
        if (t.containsAny("ceasefire", "peace", "withdraw")) score -= 1
        if (t.containsAny("kills", "dead", "casualties", "death")) score += 2
        return score.coerceIn(1, 10)
    }

    private fun extractKeywords(title: String): List<String> {
        val keywords = mutableListOf<String>()
        val militaryKw = listOf("NATO","Russia","Ukraine","China","Taiwan","Iran","North Korea",
            "nuclear","missile","troops","ceasefire","offensive","sanctions","drone","navy")
        militaryKw.filter { title.contains(it, ignoreCase = true) }.forEach { keywords.add(it) }
        return keywords.distinct().take(5)
    }

    private fun String.containsAny(vararg terms: String) = terms.any { this.contains(it) }
}
