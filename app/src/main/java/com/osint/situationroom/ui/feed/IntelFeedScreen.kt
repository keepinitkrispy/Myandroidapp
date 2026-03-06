package com.osint.situationroom.ui.feed

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.osint.situationroom.data.model.EventType
import com.osint.situationroom.data.model.OsintEvent
import com.osint.situationroom.ui.components.*
import com.osint.situationroom.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IntelFeedScreen(viewModel: FeedViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

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
                    Text("INTELLIGENCE FEED", color = CyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily, letterSpacing = 2.sp)
                    Text(
                        "${state.filteredEvents.size} EVENTS · 10+ SOURCES · ZERO SPIN",
                        color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveIndicator()
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanPrimary, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.refresh() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecond, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        ScanLine()

        // Search
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.setSearch(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("SEARCH EVENTS...", color = TextMuted, fontSize = 12.sp, fontFamily = MonoFamily) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearch("") }) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = BgBorder,
                cursorColor = CyanPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            shape = RoundedCornerShape(6.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = MonoFamily, fontSize = 12.sp)
        )

        // Event type filters
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { viewModel.filterByType(null) },
                    label = "ALL"
                )
            }
            items(EventType.values().filter { it != EventType.UNKNOWN }) { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { viewModel.filterByType(if (state.selectedType == type) null else type) },
                    label = type.label.uppercase(),
                    color = eventTypeColor(type)
                )
            }
        }

        // Error
        if (state.error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmberWarning.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.error!!, color = AmberWarning, fontSize = 11.sp, fontFamily = MonoFamily)
            }
        }

        // Events list
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("FETCHING INTEL FEEDS...", color = TextMuted, fontFamily = MonoFamily, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (state.filteredEvents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No events match filter", color = TextMuted, fontFamily = MonoFamily, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(state.filteredEvents, key = { it.id }) { event ->
                        ExpandableEventCard(event = event, onOpenLink = {
                            try { uriHandler.openUri(event.url) } catch (e: Exception) {}
                        })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun ExpandableEventCard(event: OsintEvent, onOpenLink: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val eventColor = eventTypeColor(event.eventType)
    val severityColor = threatColor(event.severity.toFloat())
    val tsFormat = SimpleDateFormat("MMM dd HH:mm 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = BgSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (expanded) eventColor.copy(alpha = 0.5f) else BgBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Type + severity
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(eventColor.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(event.eventType.label.uppercase(), color = eventColor, fontSize = 9.sp, fontFamily = MonoFamily, letterSpacing = 1.sp)
                        }
                        Text(
                            tsFormat.format(Date(event.timestamp)),
                            color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        event.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MonoFamily,
                        lineHeight = 19.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Severity
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(severityColor.copy(alpha = 0.15f))
                            .border(1.dp, severityColor, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${event.severity}", color = severityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MonoFamily)
                    }
                    Text("SEV", color = TextMuted, fontSize = 8.sp, fontFamily = MonoFamily)
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(event.source.take(20), color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily)
                    if (event.sourceCountry != null) {
                        Text("· ${event.sourceCountry}", color = TextMuted, fontSize = 10.sp, fontFamily = MonoFamily)
                    }
                }
                Text(event.region, color = TextSecond, fontSize = 10.sp, fontFamily = MonoFamily)
            }

            // Expanded detail
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Divider(color = BgBorder, thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                if (event.keywords.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("KEYWORDS:", color = TextMuted, fontSize = 9.sp, fontFamily = MonoFamily)
                        event.keywords.forEach { kw ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(BgElevated)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(kw, color = TextSecond, fontSize = 9.sp, fontFamily = MonoFamily)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                TextButton(
                    onClick = onOpenLink,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = CyanPrimary)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("OPEN SOURCE: ${event.source}", fontSize = 10.sp, fontFamily = MonoFamily)
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    color: androidx.compose.ui.graphics.Color = CyanPrimary
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else BgSurface)
            .border(1.dp, if (selected) color else BgBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) color else TextSecond,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = MonoFamily,
            letterSpacing = 0.5.sp
        )
    }
}
