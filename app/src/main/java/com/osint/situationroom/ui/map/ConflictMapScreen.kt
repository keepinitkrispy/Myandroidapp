package com.osint.situationroom.ui.map

import android.graphics.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osint.situationroom.data.model.ConflictZone
import com.osint.situationroom.data.model.Trajectory
import com.osint.situationroom.ui.components.*
import com.osint.situationroom.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import android.graphics.Paint as AndroidPaint

@Composable
fun ConflictMapScreen(viewModel: MapViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgDeep)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CONFLICT MAP", color = CyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily, letterSpacing = 2.sp)
                    Text("${state.conflictZones.size} ACTIVE ZONES MONITORED", color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily)
                }
                LiveIndicator()
            }
        }

        ScanLine()

        // Map
        Box(modifier = Modifier.weight(0.55f).fillMaxWidth()) {
            OsmdroidMap(
                conflictZones = state.conflictZones,
                onMarkerClick = { zone -> viewModel.selectZone(zone) },
                modifier = Modifier.fillMaxSize()
            )
            MapLegend(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        }

        // Zone Detail Sheet (bottom half)
        LazyColumn(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxWidth()
                .background(BgPrimary),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val selected = state.selectedZone
            if (selected != null) {
                item { ZoneDetailPanel(zone = selected, onClose = { viewModel.selectZone(null) }) }
            } else {
                item {
                    SectionHeader(
                        title = "All Conflict Zones",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(state.conflictZones.sortedByDescending { it.threatScore }) { zone ->
                    ConflictZoneCard(
                        zone = zone,
                        onClick = { viewModel.selectZone(zone) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmdroidMap(
    conflictZones: List<ConflictZone>,
    onMarkerClick: (ConflictZone) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = "OSINT-SituationRoom/1.0"
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(3.0)
                controller.setCenter(GeoPoint(25.0, 20.0))
                // Dark-style tiles look best; OSM default is OK for functionality
                isHorizontalMapRepetitionEnabled = true
                isVerticalMapRepetitionEnabled = false
                minZoomLevel = 2.0
                maxZoomLevel = 12.0
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            conflictZones.forEach { zone ->
                addConflictMarker(mapView, zone, onMarkerClick)
                addThreatCircle(mapView, zone)
            }
            mapView.invalidate()
        },
        modifier = modifier
    )
}

private fun addConflictMarker(mapView: MapView, zone: ConflictZone, onClick: (ConflictZone) -> Unit) {
    val marker = Marker(mapView).apply {
        position = GeoPoint(zone.lat, zone.lon)
        title = zone.name
        snippet = "Threat: ${zone.threatScore}/10 | ${zone.trajectory.label}"
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Custom icon based on threat level
        val color = when {
            zone.threatScore >= 8f -> android.graphics.Color.parseColor("#FF1744")
            zone.threatScore >= 6f -> android.graphics.Color.parseColor("#FF6D00")
            zone.threatScore >= 4f -> android.graphics.Color.parseColor("#FFD740")
            else -> android.graphics.Color.parseColor("#00E676")
        }

        val bmp = createMarkerBitmap(zone.threatScore, color, zone.nuclearRisk)
        icon = android.graphics.drawable.BitmapDrawable(mapView.resources, bmp)

        setOnMarkerClickListener { _, _ ->
            onClick(zone)
            true
        }
    }
    mapView.overlays.add(marker)
}

private fun addThreatCircle(mapView: MapView, zone: ConflictZone) {
    val radiusDeg = zone.threatScore * 0.3 // rough visual radius
    val polygon = Polygon(mapView).apply {
        points = Polygon.pointsAsCircle(GeoPoint(zone.lat, zone.lon), zone.threatScore * 50_000.0)
        val color = when {
            zone.threatScore >= 8f -> android.graphics.Color.parseColor("#33FF1744")
            zone.threatScore >= 6f -> android.graphics.Color.parseColor("#33FF6D00")
            zone.threatScore >= 4f -> android.graphics.Color.parseColor("#33FFD740")
            else -> android.graphics.Color.parseColor("#2200E676")
        }
        fillPaint.color = color
        outlinePaint.color = color.and(0xFFFFFF) or 0x88000000.toInt()
        outlinePaint.strokeWidth = 2f
    }
    mapView.overlays.add(0, polygon) // add circles below markers
}

private fun createMarkerBitmap(score: Float, color: Int, nuclear: Boolean): Bitmap {
    val size = 60
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)

    // Outer glow
    paint.color = color
    paint.alpha = 60
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    // Main circle
    paint.alpha = 220
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

    // Score text
    paint.color = android.graphics.Color.WHITE
    paint.alpha = 255
    paint.textSize = 16f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    paint.isFakeBoldText = true
    paint.typeface = Typeface.MONOSPACE
    val text = String.format("%.0f", score)
    canvas.drawText(text, size / 2f, size / 2f + 6f, paint)

    // Nuclear icon
    if (nuclear) {
        paint.color = android.graphics.Color.parseColor("#D500F9")
        paint.textSize = 10f
        canvas.drawText("☢", size / 2f, size / 2f - 8f, paint)
    }

    return bmp
}

@Composable
private fun ZoneDetailPanel(zone: ConflictZone, onClose: () -> Unit) {
    val zoneColor = threatColor(zone.threatScore)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, zoneColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(zone.name, color = zoneColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TrajectoryBadge(zone.trajectory)
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(zone.description, color = TextSecond, fontSize = 11.sp, fontFamily = MonoFamily, lineHeight = 16.sp)

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("PARTIES", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
                    zone.activeParties.forEach {
                        Text("· $it", color = TextSecond, fontSize = 10.sp, fontFamily = MonoFamily)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("KEY FACTS:", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
            zone.keyFacts.forEach {
                Text("▸ $it", color = TextPrimary, fontSize = 11.sp, fontFamily = MonoFamily, lineHeight = 15.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (zone.nuclearRisk) {
                    Chip(label = "☢ NUCLEAR RISK", color = PurpleNuclear)
                }
                if (zone.natoInvolvement) {
                    Chip(label = "NATO INVOLVED", color = TrajStable)
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BgDeep.copy(alpha = 0.85f))
            .border(1.dp, BgBorder, RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("THREAT LEVEL", color = TextMuted, fontSize = 8.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
        listOf(
            "8–10" to ThreatCritical,
            "6–8 " to ThreatHigh,
            "4–6 " to ThreatElevated,
            "0–4 " to ThreatLow
        ).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
                Text(label, color = TextSecond, fontSize = 9.sp, fontFamily = MonoFamily)
            }
        }
    }
}
