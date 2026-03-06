package com.osint.situationroom.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osint.situationroom.data.model.*
import com.osint.situationroom.ui.components.*
import com.osint.situationroom.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onViewAllEvents: () -> Unit = {},
    onViewMap: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        if (state.isLoading && state.assessment == null) {
            LoadingScreen()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── Header ──────────────────────────────────────────────────
                item {
                    DashboardHeader(
                        isRefreshing = state.isRefreshing,
                        lastRefresh = state.lastRefresh,
                        onRefresh = { viewModel.refresh() }
                    )
                }

                // ── Scan Line ───────────────────────────────────────────────
                item { ScanLine(modifier = Modifier.padding(vertical = 2.dp)) }

                // ── Error Banner ────────────────────────────────────────────
                if (state.error != null) {
                    item {
                        ErrorBanner(message = state.error!!)
                    }
                }

                // ── Global Threat Assessment ─────────────────────────────────
                item {
                    ThreatAssessmentBlock(
                        assessment = state.assessment,
                        onViewMap = onViewMap
                    )
                }

                // ── Quick Metrics ────────────────────────────────────────────
                item {
                    state.assessment?.let { QuickMetricsRow(it) }
                }

                // ── Regional Alerts ──────────────────────────────────────────
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader("Regional Alert Status") {
                            TextButton(onClick = onViewMap) {
                                Text("MAP →", color = CyanPrimary, fontSize = 11.sp, fontFamily = MonoFamily)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(state.regionalAlerts) { alert ->
                    RegionalAlertRow(
                        alert = alert,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                    )
                }

                // ── Top Conflict Zones ────────────────────────────────────────
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    SectionHeader(
                        title = "Active Conflict Zones",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(state.conflictZones.take(5)) { zone ->
                    ConflictZoneCard(
                        zone = zone,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ── Latest Intel Events ───────────────────────────────────────
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    SectionHeader(
                        title = "Latest Intel Feed",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        TextButton(onClick = onViewAllEvents) {
                            Text("ALL →", color = CyanPrimary, fontSize = 11.sp, fontFamily = MonoFamily)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (state.topEvents.isEmpty() && !state.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No live events — check network", color = TextMuted, fontFamily = MonoFamily, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(state.topEvents) { event ->
                        OsintEventCard(
                            event = event,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // ── Methodology Note ──────────────────────────────────────────
                item {
                    Spacer(Modifier.height(16.dp))
                    MethodologyCard(state.assessment)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(isRefreshing: Boolean, lastRefresh: Long, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(BgDeep, BgPrimary))
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GLOBAL SITUATION ROOM",
                        color = CyanPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFamily,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "OSINT MONITORING SYSTEM v1.0",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = MonoFamily,
                        letterSpacing = 1.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveIndicator()
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanPrimary, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecond, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (lastRefresh > 0L) {
                val fmt = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                Text(
                    text = "LAST SYNC: ${fmt.format(Date(lastRefresh))}  |  SRC: GDELT v2",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = MonoFamily,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ThreatAssessmentBlock(assessment: GlobalThreatAssessment?, onViewMap: () -> Unit) {
    val score = assessment?.overallScore ?: 7.2f
    val trajectory = assessment?.trajectory ?: Trajectory.VOLATILE
    val scoreColor = threatColor(score)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Central gauge
            ThreatGauge(score = score, size = 200.dp)
        }

        Spacer(Modifier.height(12.dp))

        // Trajectory + nuclear alert
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {}

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "TRAJECTORY", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                TrajectoryBadge(trajectory)
            }

            assessment?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "NUCLEAR ALERT", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    NuclearAlertBar(level = it.nuclearAlertLevel)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "CONFIDENCE", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(it.confidence * 100).toInt()}%",
                        color = if (it.confidence > 0.6f) GreenSafe else AmberWarning,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFamily
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Top threats
        assessment?.topThreats?.let { threats ->
            Text(text = "TOP MONITORED SITUATIONS:", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(threats) { threat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(scoreColor.copy(alpha = 0.12f))
                            .border(1.dp, scoreColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = threat, color = scoreColor, fontSize = 10.sp, fontFamily = MonoFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMetricsRow(assessment: GlobalThreatAssessment) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            MetricTile(
                label = "CONFLICTS",
                value = "${assessment.activeConflicts}",
                icon = Icons.Default.Warning,
                color = ThreatHigh,
                modifier = Modifier.width(100.dp),
                subLabel = "ACTIVE (score≥7)"
            )
        }
        item {
            MetricTile(
                label = "24H EVENTS",
                value = if (assessment.criticalEvents24h >= 0) "${assessment.criticalEvents24h}" else "N/A",
                icon = Icons.Default.Notifications,
                color = ThreatCritical,
                modifier = Modifier.width(100.dp),
                subLabel = "CRITICAL"
            )
        }
        item {
            MetricTile(
                label = "MILITARY",
                value = if (assessment.militaryIncidents >= 0) "${assessment.militaryIncidents}" else "N/A",
                icon = Icons.Default.Security,
                color = ThreatElevated,
                modifier = Modifier.width(100.dp),
                subLabel = "INCIDENTS"
            )
        }
        item {
            MetricTile(
                label = "DIPLOMATIC",
                value = if (assessment.diplomaticActivity >= 0) "${assessment.diplomaticActivity}" else "N/A",
                icon = Icons.Default.Forum,
                color = TrajDeEscalating,
                modifier = Modifier.width(100.dp),
                subLabel = "SIGNALS"
            )
        }
    }
}

@Composable
private fun NuclearAlertBar(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= level) PurpleNuclear else BgBorder)
            )
        }
        Text(text = " ☢", color = if (level >= 3) PurpleNuclear else TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun MethodologyCard(assessment: GlobalThreatAssessment?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BgBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                Text(text = "ASSESSMENT BASIS", color = CyanPrimary, fontSize = 10.sp, fontFamily = MonoFamily, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            }
            assessment?.assessmentBasis?.forEach { note ->
                Text(text = "· $note", color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily, lineHeight = 15.sp)
            } ?: Text(text = "· Loading assessment data...", color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "ZERO EDITORIAL BIAS · ALGORITHMIC ASSESSMENT · ALL SOURCES CITED",
                color = TextMuted.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontFamily = MonoFamily,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AmberWarning.copy(alpha = 0.1f))
            .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.WifiOff, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
        Text(text = message, color = AmberWarning, fontSize = 11.sp, fontFamily = MonoFamily)
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = CyanPrimary, strokeWidth = 2.dp, modifier = Modifier.size(40.dp))
            Text("INITIALIZING OSINT FEEDS...", color = CyanPrimary, fontSize = 11.sp, fontFamily = MonoFamily, letterSpacing = 2.sp)
            Text("GDELT · REUTERS · BBC · AL JAZEERA · AP", color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily, textAlign = TextAlign.Center)
        }
    }
}
