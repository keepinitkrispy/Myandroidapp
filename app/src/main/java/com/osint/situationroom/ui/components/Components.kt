package com.osint.situationroom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.osint.situationroom.data.model.*
import com.osint.situationroom.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─── Threat Gauge ─────────────────────────────────────────────────────────────

@Composable
fun ThreatGauge(
    score: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val animScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "threat_score"
    )

    val color = threatColor(score)
    val pulsate = rememberInfiniteTransition(label = "pulse")
    val alpha by pulsate.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = size.toPx() / 10f
            val radius = (size.toPx() - stroke) / 2f
            val cx = size.toPx() / 2f
            val cy = size.toPx() / 2f

            // Background arc (full 270°)
            drawArc(
                color = BgElevated,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Colored arc
            val sweep = (animScore / 10f) * 270f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ThreatNone, ThreatElevated, ThreatHigh, ThreatCritical),
                    center = Offset(cx, cy)
                ),
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )

            // Needle dot at current position
            val needleAngle = (135f + sweep) * (PI / 180f)
            val dotX = cx + radius * cos(needleAngle).toFloat()
            val dotY = cy + radius * sin(needleAngle).toFloat()
            drawCircle(color = color.copy(alpha = alpha), radius = stroke / 2f, center = Offset(dotX, dotY))

            // Tick marks
            for (i in 0..10) {
                val tickAngle = (135f + (i / 10f) * 270f) * (PI / 180f)
                val innerR = radius - stroke
                val outerR = radius + stroke * 0.3f
                val x1 = cx + innerR * cos(tickAngle).toFloat()
                val y1 = cy + innerR * sin(tickAngle).toFloat()
                val x2 = cx + outerR * cos(tickAngle).toFloat()
                val y2 = cy + outerR * sin(tickAngle).toFloat()
                drawLine(
                    color = if (i % 5 == 0) TextSecond else BgBorder,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (i % 5 == 0) 2.5f else 1.5f
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", score),
                fontSize = (size.value / 4.5f).sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = MonoFamily
            )
            Text(
                text = "/ 10",
                fontSize = (size.value / 11f).sp,
                color = TextMuted,
                fontFamily = MonoFamily
            )
            Text(
                text = threatLabel(score),
                fontSize = (size.value / 12f).sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = MonoFamily,
                letterSpacing = 1.sp
            )
        }
    }
}

// ─── Trajectory Badge ─────────────────────────────────────────────────────────

@Composable
fun TrajectoryBadge(trajectory: Trajectory, modifier: Modifier = Modifier) {
    val (color, icon) = when (trajectory) {
        Trajectory.ESCALATING    -> TrajEscalating to "↑"
        Trajectory.DE_ESCALATING -> TrajDeEscalating to "↓"
        Trajectory.STABLE        -> TrajStable to "→"
        Trajectory.VOLATILE      -> TrajVolatile to "~"
    }

    val pulse = rememberInfiniteTransition(label = "traj_pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = alpha), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, color = color.copy(alpha = alpha), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            text = trajectory.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MonoFamily,
            letterSpacing = 1.sp
        )
    }
}

// ─── Event Card ───────────────────────────────────────────────────────────────

@Composable
fun OsintEventCard(
    event: OsintEvent,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val eventColor = eventTypeColor(event.eventType)
    val severityColor = threatColor(event.severity.toFloat())

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BgBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Event type chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(eventColor.copy(alpha = 0.15f))
                        .border(1.dp, eventColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.eventType.label.uppercase(),
                        color = eventColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MonoFamily,
                        letterSpacing = 1.sp
                    )
                }
                // Severity indicator
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "SEV", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily)
                    SeverityBar(severity = event.severity, color = severityColor)
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = event.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MonoFamily,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(11.dp), tint = TextMuted)
                    Text(text = event.source.take(25), color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(BgElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(text = event.region, color = TextSecond, fontSize = 10.sp, fontFamily = MonoFamily)
                    }
                }
            }
        }
    }
}

// ─── Conflict Zone Card ───────────────────────────────────────────────────────

@Composable
fun ConflictZoneCard(zone: ConflictZone, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val zoneColor = threatColor(zone.threatScore)

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, zoneColor.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Threat score circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(zoneColor.copy(alpha = 0.15f))
                    .border(2.dp, zoneColor, CircleShape)
            ) {
                Text(
                    text = String.format("%.0f", zone.threatScore),
                    color = zoneColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFamily
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = zone.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily)
                Text(text = zone.countries.joinToString(" · "), color = TextSecond, fontSize = 11.sp, fontFamily = MonoFamily, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(text = zone.description, color = TextMuted, fontSize = 11.sp, fontFamily = MonoFamily, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TrajectoryBadge(zone.trajectory)
                if (zone.nuclearRisk) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(PurpleNuclear.copy(alpha = 0.15f))
                            .border(1.dp, PurpleNuclear.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(text = "☢ NUCLEAR", color = PurpleNuclear, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 0.5.sp)
                    }
                }
            }
        }
    }
}

// ─── Metric Tile ──────────────────────────────────────────────────────────────

@Composable
fun MetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color = TextAccent,
    modifier: Modifier = Modifier,
    subLabel: String = ""
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BgBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(text = label, color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
            }
            Text(text = value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily)
            if (subLabel.isNotEmpty()) {
                Text(text = subLabel, color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily)
            }
        }
    }
}

// ─── Regional Alert Row ───────────────────────────────────────────────────────

@Composable
fun RegionalAlertRow(alert: RegionalAlert, modifier: Modifier = Modifier) {
    val color = threatColor(alert.alertLevel.toFloat())
    val changeText = if (alert.change24h >= 0) "+${String.format("%.1f", alert.change24h)}" else String.format("%.1f", alert.change24h)
    val changeColor = if (alert.change24h > 0) TrajEscalating else if (alert.change24h < 0) TrajDeEscalating else TextMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(BgSurface)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = alert.flag, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = alert.region, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily)
            Text(text = alert.summary, color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(color)
                )
                Text(text = "${alert.alertLevel}/10", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily)
            }
            Text(text = changeText, color = changeColor, fontSize = 10.sp, fontFamily = MonoFamily)
        }
    }
}

// ─── Scan Line Decoration ─────────────────────────────────────────────────────

@Composable
fun ScanLine(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "scan_offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, CyanPrimary.copy(alpha = 0.6f), Color.Transparent),
                        startX = size.width * (offset - 0.2f),
                        endX = size.width * (offset + 0.2f)
                    ),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 1.dp.toPx()
                )
            }
    )
}

// ─── Status Indicator ─────────────────────────────────────────────────────────

@Composable
fun LiveIndicator(label: String = "LIVE", modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "live_pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "live_alpha"
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(RedAlert.copy(alpha = alpha)))
        Text(text = label, color = RedAlert, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily, letterSpacing = 1.5.sp)
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(3.dp).height(16.dp).background(CyanPrimary))
            Text(
                text = title.uppercase(),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MonoFamily,
                letterSpacing = 1.5.sp
            )
        }
        action()
    }
}

// ─── Severity Bar ─────────────────────────────────────────────────────────────

@Composable
fun SeverityBar(severity: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..10) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 10.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= severity) color else BgBorder)
            )
        }
    }
}

// ─── Helper Functions ─────────────────────────────────────────────────────────

fun threatColor(score: Float): Color = when {
    score >= 9f -> ThreatExtreme
    score >= 8f -> ThreatCritical
    score >= 6f -> ThreatHigh
    score >= 4f -> ThreatElevated
    score >= 2f -> ThreatLow
    else        -> ThreatNone
}

fun threatLabel(score: Float): String = when {
    score >= 9f -> "EXTREME"
    score >= 8f -> "CRITICAL"
    score >= 6f -> "HIGH"
    score >= 4f -> "ELEVATED"
    score >= 2f -> "LOW"
    else        -> "MINIMAL"
}

fun eventTypeColor(type: EventType): Color = when (type) {
    EventType.MILITARY_ACTION  -> EvMilitary
    EventType.DIPLOMATIC       -> EvDiplomat
    EventType.ECONOMIC         -> EvEconomic
    EventType.NUCLEAR          -> EvNuclear
    EventType.CYBER            -> EvCyber
    EventType.HUMANITARIAN     -> EvHumanitar
    EventType.INFORMATION_OPS  -> EvInfoOps
    else                       -> TextSecond
}
