package com.osint.situationroom.domain

import com.osint.situationroom.data.model.ConflictZone
import com.osint.situationroom.data.model.RegionalAlert
import com.osint.situationroom.data.model.Trajectory

/**
 * Baseline conflict zone data — sourced from publicly available
 * geopolitical reporting, UN reports, and defense think-tanks.
 * These serve as static anchors updated by live GDELT data.
 * NO editorial framing — facts only.
 */
object ConflictZoneData {

    val zones: List<ConflictZone> = listOf(

        ConflictZone(
            id = "ukraine_russia",
            name = "Ukraine–Russia",
            lat = 49.0, lon = 32.0,
            threatScore = 8.5f,
            description = "Full-scale land war. 2022–present. Largest ground conflict in Europe since 1945.",
            countries = listOf("Ukraine", "Russia"),
            activeParties = listOf("Ukrainian Armed Forces", "Russian Armed Forces", "Wagner/Africa Corps"),
            trajectory = Trajectory.VOLATILE,
            nuclearRisk = true,
            natoInvolvement = true,
            keyFacts = listOf(
                "Active frontline ~1,000 km",
                "NATO allies supplying weapons/intelligence",
                "Russian tactical nuclear weapons in Belarus",
                "~500,000+ total casualties (both sides, est.)",
                "Zaporizhzhia NPP under Russian control"
            )
        ),

        ConflictZone(
            id = "middle_east_israel",
            name = "Middle East (Gaza / Lebanon)",
            lat = 31.5, lon = 35.0,
            threatScore = 8.0f,
            description = "Multi-front conflict: Gaza, West Bank, Lebanon border, Houthi Red Sea campaign.",
            countries = listOf("Israel", "Gaza", "Lebanon", "Yemen", "Iran"),
            activeParties = listOf("IDF", "Hamas", "Hezbollah", "Houthis", "IRGC proxies"),
            trajectory = Trajectory.VOLATILE,
            nuclearRisk = false,
            natoInvolvement = false,
            keyFacts = listOf(
                "Gaza conflict since Oct 7 2023",
                "Houthi Red Sea shipping disruptions",
                "Iran-Israel direct missile exchanges",
                "Lebanon ceasefire fragile",
                "Regional escalation risk remains elevated"
            )
        ),

        ConflictZone(
            id = "taiwan_strait",
            name = "Taiwan Strait",
            lat = 24.0, lon = 121.0,
            threatScore = 7.5f,
            description = "Persistent PLA military pressure on Taiwan. US security commitments contested.",
            countries = listOf("China (PRC)", "Taiwan (ROC)", "United States"),
            activeParties = listOf("PLA (Air/Naval)", "ROC Military", "US Pacific Fleet"),
            trajectory = Trajectory.ESCALATING,
            nuclearRisk = true,
            natoInvolvement = false,
            keyFacts = listOf(
                "PLA conducts regular encirclement exercises",
                "US arms sales to Taiwan ongoing",
                "Xi Jinping 'reunification' declared non-negotiable",
                "Taiwan Strait remains international waterway",
                "US–China direct military communication channels limited"
            )
        ),

        ConflictZone(
            id = "korean_peninsula",
            name = "Korean Peninsula",
            lat = 38.5, lon = 127.5,
            threatScore = 7.0f,
            description = "North Korea nuclear/ICBM tests continue. DPRK–Russia military cooperation expanding.",
            countries = listOf("North Korea (DPRK)", "South Korea (ROK)", "United States", "Russia"),
            activeParties = listOf("KPA", "ROK Military", "US Forces Korea"),
            trajectory = Trajectory.ESCALATING,
            nuclearRisk = true,
            natoInvolvement = false,
            keyFacts = listOf(
                "DPRK has est. 40–50 nuclear warheads",
                "ICBM range covers continental US",
                "DPRK troops reportedly deployed to Russia",
                "Six-party talks defunct since 2009",
                "US–ROK–Japan trilateral drills intensifying"
            )
        ),

        ConflictZone(
            id = "iran_nuclear",
            name = "Iran Nuclear Standoff",
            lat = 32.0, lon = 53.0,
            threatScore = 6.5f,
            description = "Iran at ~60% U-235 enrichment. JCPOA collapsed. Regional proxy network active.",
            countries = listOf("Iran", "United States", "Israel", "Saudi Arabia"),
            activeParties = listOf("IRGC", "IDF", "US CENTCOM", "Saudi Military"),
            trajectory = Trajectory.ESCALATING,
            nuclearRisk = true,
            natoInvolvement = false,
            keyFacts = listOf(
                "Iran enriching uranium to ~60% (weapons-grade: 90%)",
                "Breakout time estimate: ~1–2 weeks to weapons-grade material",
                "IAEA monitoring severely limited",
                "Iran supplies drones to Russia and Houthis",
                "US carrier strike group deployments to Gulf elevated"
            )
        ),

        ConflictZone(
            id = "south_china_sea",
            name = "South China Sea",
            lat = 12.0, lon = 114.0,
            threatScore = 6.0f,
            description = "China vs Philippines/Vietnam/Malaysia over islands, EEZ, resources. US FONOPs ongoing.",
            countries = listOf("China", "Philippines", "Vietnam", "Malaysia", "United States"),
            activeParties = listOf("PLA Navy/CCG", "Philippine Coast Guard", "US 7th Fleet"),
            trajectory = Trajectory.ESCALATING,
            nuclearRisk = false,
            natoInvolvement = false,
            keyFacts = listOf(
                "China claims ~90% of SCS via 'Nine-Dash Line'",
                "UN UNCLOS tribunal ruled against China (ignored)",
                "China water cannon attacks on PH resupply missions",
                "US–Philippines Enhanced Defense Cooperation active",
                "Multiple artificial islands militarized by China"
            )
        ),

        ConflictZone(
            id = "sahel_africa",
            name = "Sahel / Sub-Saharan Africa",
            lat = 14.0, lon = 5.0,
            threatScore = 5.5f,
            description = "Wave of coups, jihadist insurgencies, Russia/Wagner influence expansion.",
            countries = listOf("Mali", "Niger", "Burkina Faso", "Sudan", "Ethiopia", "Somalia"),
            activeParties = listOf("JNIM", "ISWAP", "Wagner/Africa Corps", "AU Forces", "French Forces (withdrawing)"),
            trajectory = Trajectory.VOLATILE,
            nuclearRisk = false,
            natoInvolvement = false,
            keyFacts = listOf(
                "8 coups in 3 years (Mali, Niger, Burkina Faso, Guinea, etc.)",
                "Wagner/Africa Corps replaced French presence",
                "Sudan civil war: RSF vs SAF ongoing",
                "Somalia: Al-Shabaab remains capable insurgency",
                "Refugee flows destabilizing neighboring states"
            )
        ),

        ConflictZone(
            id = "india_pakistan",
            name = "India–Pakistan (Kashmir)",
            lat = 34.0, lon = 74.5,
            threatScore = 5.0f,
            description = "Persistent LoC tensions. Both states nuclear-armed. Cross-border raids occasional.",
            countries = listOf("India", "Pakistan"),
            activeParties = listOf("Indian Army", "Pakistani Army", "LeT/JeM (proxy groups)"),
            trajectory = Trajectory.STABLE,
            nuclearRisk = true,
            natoInvolvement = false,
            keyFacts = listOf(
                "Both nations have ~150–160 nuclear warheads each",
                "No nuclear 'no first use' doctrine for Pakistan",
                "2019 Balakot airstrikes set new escalation precedent",
                "Article 370 revocation increased tensions in 2019",
                "Indus Waters Treaty dispute escalating"
            )
        ),

        ConflictZone(
            id = "arctic",
            name = "Arctic Militarization",
            lat = 78.0, lon = 20.0,
            threatScore = 4.5f,
            description = "Russia, China, US compete for Arctic resources and strategic routes as ice retreats.",
            countries = listOf("Russia", "United States", "China", "Norway", "Canada"),
            activeParties = listOf("Russian Northern Fleet", "US 2nd Fleet", "PLA Navy (observer)"),
            trajectory = Trajectory.ESCALATING,
            nuclearRisk = true,
            natoInvolvement = true,
            keyFacts = listOf(
                "Arctic warming 4x faster than global average",
                "Northwest Passage commercially viable",
                "Russia has largest Arctic military infrastructure",
                "Svalbard: Norwegian territory, Russian presence contested",
                "Undersea cable vulnerability in High North"
            )
        )
    )

    val regionalAlerts: List<RegionalAlert> = listOf(
        RegionalAlert("Eastern Europe", "🇺🇦", 9, "Active war — frontline advances/retreats daily", +0.3f, Trajectory.VOLATILE),
        RegionalAlert("Middle East", "🌍", 8, "Multi-front escalation risk, Iran nuclear timeline", +0.5f, Trajectory.ESCALATING),
        RegionalAlert("Indo-Pacific", "🌏", 8, "PLA Taiwan exercises, SCS confrontations", +0.4f, Trajectory.ESCALATING),
        RegionalAlert("Korean Peninsula", "🇰🇷", 7, "DPRK ICBM tests, Russia–DPRK axis forming", +0.6f, Trajectory.ESCALATING),
        RegionalAlert("Persian Gulf", "🛢️", 7, "Iran nuclear breakout timeline shortening", +0.4f, Trajectory.ESCALATING),
        RegionalAlert("Sub-Saharan Africa", "🌍", 6, "Multi-state instability, Wagner expansion", +0.1f, Trajectory.VOLATILE),
        RegionalAlert("South Asia", "🇮🇳", 5, "Stable but both nuclear powers at LoC", 0.0f, Trajectory.STABLE),
        RegionalAlert("Arctic", "❄️", 5, "Quiet but strategic competition intensifying", +0.2f, Trajectory.ESCALATING)
    )
}
