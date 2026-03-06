package com.osint.situationroom.domain

import com.osint.situationroom.data.model.*
import kotlin.math.min

/**
 * ThreatCalculator — transparent, algorithmic threat assessment.
 *
 * METHODOLOGY (fully disclosed, no hidden weighting):
 *
 *  Score 0–10 composed of:
 *   [A] Event Frequency Score (0–3.0)  — how many conflict events in 24h vs baseline
 *   [B] Tone Score            (0–2.0)  — GDELT article negativity
 *   [C] Active Conflicts      (0–2.0)  — number of hot wars vs historical norm
 *   [D] Nuclear Risk Flag     (0–2.0)  — any nuclear-armed party involved
 *   [E] Trajectory Trend      (0–1.0)  — 7-day momentum (escalating +1, de-escal -0.5)
 *
 * All inputs sourced from: GDELT Project (gdeltproject.org), public conflict databases.
 * No subjective editorial judgment applied.
 */
object ThreatCalculator {

    // Baseline "normal" event count (estimated from GDELT historical data)
    private const val BASELINE_EVENTS_24H = 40
    private const val MAX_CONFLICTS_BASELINE = 3

    fun compute(
        articles: List<GdeltArticle>,
        timeline: List<GdeltTimelineDataPoint>,
        conflictZones: List<ConflictZone>
    ): GlobalThreatAssessment {

        // [A] Event Frequency Score
        val eventCount = articles.size
        val freqScore = min(3.0f, (eventCount.toFloat() / BASELINE_EVENTS_24H) * 1.5f)

        // [B] Tone Score — proxy: count negative/escalatory keywords in titles
        val negativeKeywords = listOf("attack", "strike", "kill", "war", "nuclear", "missile",
            "invasion", "offensive", "escalat", "threat", "launch", "bomb", "casualties", "dead")
        val positiveKeywords = listOf("ceasefire", "peace", "withdrawal", "diplomacy", "talks",
            "agreement", "truce", "negotiat", "de-escalat")

        var negCount = 0
        var posCount = 0
        articles.forEach { a ->
            val t = a.title.lowercase()
            negCount += negativeKeywords.count { t.contains(it) }
            posCount += positiveKeywords.count { t.contains(it) }
        }
        val toneRatio = if (negCount + posCount > 0) negCount.toFloat() / (negCount + posCount) else 0.5f
        val toneScore = toneRatio * 2.0f

        // [C] Active High-Intensity Conflicts
        val hotConflicts = conflictZones.count { it.threatScore >= 7.0f }
        val conflictScore = min(2.0f, hotConflicts.toFloat() / MAX_CONFLICTS_BASELINE * 2.0f)

        // [D] Nuclear Risk
        val nuclearZones = conflictZones.count { it.nuclearRisk && it.threatScore >= 5.0f }
        val nuclearScore = min(2.0f, nuclearZones * 0.5f)

        // [E] Trajectory Trend (from timeline)
        val trendScore = computeTrendScore(timeline)

        val rawScore = freqScore + toneScore + conflictScore + nuclearScore + trendScore
        val finalScore = rawScore.coerceIn(0f, 10f)

        val trajectory = computeTrajectory(timeline, conflictZones)

        // Military incidents = events typed as military
        val militaryCount = articles.count { a ->
            val t = a.title.lowercase()
            listOf("attack","strike","troops","military","offensive","missile","drone","navy").any { t.contains(it) }
        }

        val diplomaticCount = articles.count { a ->
            val t = a.title.lowercase()
            listOf("talks","ceasefire","diplomat","peace","negotiat","summit","treaty").any { t.contains(it) }
        }

        val nuclearAlertLevel = when {
            nuclearZones >= 3 && finalScore >= 8 -> 5
            nuclearZones >= 2 && finalScore >= 7 -> 4
            nuclearZones >= 2 && finalScore >= 5 -> 3
            nuclearZones >= 1 && finalScore >= 4 -> 2
            nuclearZones >= 1 -> 1
            else -> 0
        }

        val topThreats = conflictZones.sortedByDescending { it.threatScore }
            .take(3).map { it.name }

        val confidence = min(1.0f, articles.size.toFloat() / 50f)

        return GlobalThreatAssessment(
            overallScore = finalScore,
            trajectory = trajectory,
            activeConflicts = hotConflicts,
            criticalEvents24h = articles.count { a -> a.title.lowercase().containsAny("nuclear","invasion","war","attack") },
            diplomaticActivity = diplomaticCount,
            militaryIncidents = militaryCount,
            nuclearAlertLevel = nuclearAlertLevel,
            lastUpdated = System.currentTimeMillis(),
            confidence = confidence,
            topThreats = topThreats,
            assessmentBasis = listOf(
                "Event frequency: ${eventCount} articles (24h window)",
                "Tone analysis: $negCount negative / $posCount positive signals",
                "Active hot conflicts (score ≥7): $hotConflicts",
                "Nuclear-risk zones (score ≥5): $nuclearZones",
                "Source: GDELT Project v2 (gdeltproject.org)",
                "Algorithm: Open-source, reproducible — see ThreatCalculator.kt"
            )
        )
    }

    fun offlineAssessment(conflictZones: List<ConflictZone>): GlobalThreatAssessment {
        val avgScore = conflictZones.map { it.threatScore }.average().toFloat()
        val hotConflicts = conflictZones.count { it.threatScore >= 7.0f }
        val nuclearZones = conflictZones.count { it.nuclearRisk }

        return GlobalThreatAssessment(
            overallScore = avgScore,
            trajectory = Trajectory.VOLATILE,
            activeConflicts = hotConflicts,
            criticalEvents24h = -1,  // -1 = unknown (offline)
            diplomaticActivity = -1,
            militaryIncidents = -1,
            nuclearAlertLevel = if (nuclearZones >= 2) 3 else 1,
            lastUpdated = System.currentTimeMillis(),
            confidence = 0.4f,
            topThreats = conflictZones.sortedByDescending { it.threatScore }.take(3).map { it.name },
            assessmentBasis = listOf(
                "OFFLINE MODE — live GDELT data unavailable",
                "Assessment based on static conflict zone database only",
                "Confidence reduced to 40%"
            )
        )
    }

    private fun computeTrendScore(timeline: List<GdeltTimelineDataPoint>): Float {
        if (timeline.size < 7) return 0.5f
        val recent = timeline.takeLast(7).map { it.value }
        val earlier = timeline.takeLast(14).take(7).map { it.value }
        if (earlier.isEmpty() || recent.isEmpty()) return 0.5f
        val recentAvg = recent.average()
        val earlierAvg = earlier.average()
        if (earlierAvg == 0.0) return 0.5f
        val change = (recentAvg - earlierAvg) / earlierAvg
        return when {
            change > 0.2  -> 1.0f   // escalating
            change < -0.2 -> 0.0f   // de-escalating
            else          -> 0.5f   // stable
        }
    }

    private fun computeTrajectory(
        timeline: List<GdeltTimelineDataPoint>,
        zones: List<ConflictZone>
    ): Trajectory {
        val trendScore = computeTrendScore(timeline)
        val escalatingZones = zones.count { it.trajectory == Trajectory.ESCALATING }
        val deEscalatingZones = zones.count { it.trajectory == Trajectory.DE_ESCALATING }

        return when {
            trendScore >= 0.8f && escalatingZones > deEscalatingZones -> Trajectory.ESCALATING
            trendScore <= 0.2f && deEscalatingZones > escalatingZones -> Trajectory.DE_ESCALATING
            zones.count { it.trajectory == Trajectory.VOLATILE } >= 2  -> Trajectory.VOLATILE
            else -> Trajectory.STABLE
        }
    }

    private fun String.containsAny(vararg terms: String) = terms.any { this.contains(it) }
}
