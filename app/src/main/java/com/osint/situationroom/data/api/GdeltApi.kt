package com.osint.situationroom.data.api

import com.osint.situationroom.data.model.GdeltResponse
import com.osint.situationroom.data.model.GdeltTimelineResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * GDELT Project API v2 — free, no API key required.
 * https://blog.gdeltproject.org/gdelt-2-0-our-global-research-platform/
 */
interface GdeltApi {

    /**
     * Article search — returns recent articles matching a conflict query.
     * mode=artlist  → article list with titles, domains, dates
     */
    @GET("api/v2/doc/doc")
    suspend fun searchArticles(
        @Query("query") query: String,
        @Query("mode") mode: String = "artlist",
        @Query("format") format: String = "json",
        @Query("maxrecords") maxRecords: Int = 75,
        @Query("timespan") timespan: String = "24h",
        @Query("sourcelang") language: String = "english"
    ): GdeltResponse

    /**
     * Timeline — returns event frequency over time for trend analysis.
     * mode=timelinevol → volume-normalized timeline
     */
    @GET("api/v2/timeline/timeline")
    suspend fun getTimeline(
        @Query("query") query: String,
        @Query("format") format: String = "json",
        @Query("timespan") timespan: String = "30d",
        @Query("smoothing") smoothing: Int = 3
    ): GdeltTimelineResponse
}
