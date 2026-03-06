package com.osint.situationroom.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Situation Room Dark Palette ─────────────────────────────────────────────
// Military ops-center aesthetic: dark navy, cyan accents, threat-coded colors

val BgDeep       = Color(0xFF060B16)   // Deepest background
val BgPrimary    = Color(0xFF0A0E1A)   // Main background
val BgSurface    = Color(0xFF111827)   // Cards / panels
val BgElevated   = Color(0xFF1C2333)   // Elevated elements
val BgBorder     = Color(0xFF2A3548)   // Borders / dividers

val TextPrimary  = Color(0xFFE0E6F0)   // Main text
val TextSecond   = Color(0xFF8A9BBE)   // Secondary text
val TextMuted    = Color(0xFF4A5568)   // Muted / disabled
val TextAccent   = Color(0xFF00D4FF)   // Cyan accent

// ─── Threat Level Colors (0–10 scale) ────────────────────────────────────────
val ThreatNone     = Color(0xFF00E676)  // 0–2    MINIMAL (green)
val ThreatLow      = Color(0xFF69F0AE)  // 2–4    LOW     (light green)
val ThreatElevated = Color(0xFFFFD740)  // 4–6    ELEVATED (amber)
val ThreatHigh     = Color(0xFFFF6D00)  // 6–8    HIGH    (orange)
val ThreatCritical = Color(0xFFFF1744)  // 8–9    CRITICAL (red)
val ThreatExtreme  = Color(0xFFD50000)  // 9–10   EXTREME  (deep red)

// ─── Trajectory Colors ────────────────────────────────────────────────────────
val TrajEscalating   = Color(0xFFFF4444)  // Red arrow up
val TrajDeEscalating = Color(0xFF00E676)  // Green arrow down
val TrajStable       = Color(0xFF40C4FF)  // Blue arrow right
val TrajVolatile     = Color(0xFFFFAB40)  // Orange wave

// ─── Event Type Colors ────────────────────────────────────────────────────────
val EvMilitary  = Color(0xFFFF5252)
val EvDiplomat  = Color(0xFF40C4FF)
val EvEconomic  = Color(0xFFFFD740)
val EvNuclear   = Color(0xFFD500F9)
val EvCyber     = Color(0xFF00E5FF)
val EvHumanitar = Color(0xFF69F0AE)
val EvInfoOps   = Color(0xFFFF6E40)

// ─── System UI ────────────────────────────────────────────────────────────────
val CyanPrimary   = Color(0xFF00D4FF)
val CyanDark      = Color(0xFF0097A7)
val RedAlert      = Color(0xFFFF1744)
val GreenSafe     = Color(0xFF00E676)
val AmberWarning  = Color(0xFFFFD740)
val PurpleNuclear = Color(0xFFD500F9)
