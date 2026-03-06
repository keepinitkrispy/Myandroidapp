package com.osint.situationroom.data.repository

import android.util.Log
import com.osint.situationroom.data.api.NetworkModule
import com.osint.situationroom.data.model.RssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object RssFeedParser {

    private val TAG = "RssFeedParser"

    // Zero-spin OSINT sources — diverse geopolitical perspectives
    val FEED_SOURCES = mapOf(
        "Reuters World"     to "https://feeds.reuters.com/reuters/worldNews",
        "BBC World"         to "https://feeds.bbci.co.uk/news/world/rss.xml",
        "Al Jazeera"        to "https://www.aljazeera.com/xml/rss/all.xml",
        "AP Top News"       to "https://apnews.com/rss",
        "Defense One"       to "https://www.defenseone.com/rss/all/",
        "The War Zone"      to "https://www.thedrive.com/the-war-zone/rss",
        "SIPRI"             to "https://www.sipri.org/rss.xml",
        "UN News"           to "https://news.un.org/feed/subscribe/en/news/all/rss.xml",
        "Kyiv Independent" to "https://kyivindependent.com/rss",
        "Global Voices"     to "https://globalvoices.org/feed/"
    )

    suspend fun fetchFeed(url: String, sourceName: String): List<RssItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<RssItem>()
        try {
            val request = Request.Builder().url(url)
                .header("User-Agent", "OSINT-SituationRoom/1.0")
                .build()
            val response = NetworkModule.rawHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(body.reader())

            var eventType = parser.eventType
            var inItem = false
            var title = ""
            var link = ""
            var pubDate = ""
            var description = ""
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name ?: ""
                        if (currentTag == "item" || currentTag == "entry") inItem = true
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name ?: ""
                        if ((tag == "item" || tag == "entry") && inItem) {
                            if (title.isNotBlank()) {
                                items.add(RssItem(title.trim(), link.trim(), pubDate.trim(), description.trim(), sourceName))
                            }
                            title = ""; link = ""; pubDate = ""; description = ""; inItem = false
                        }
                        currentTag = ""
                    }
                    XmlPullParser.TEXT -> {
                        if (inItem) {
                            val text = parser.text ?: ""
                            when (currentTag) {
                                "title"       -> title += text
                                "link"        -> link += text
                                "pubDate", "published", "updated" -> pubDate += text
                                "description", "summary", "content" -> if (description.length < 500) description += text
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse feed $sourceName: ${e.message}")
        }
        items
    }

    suspend fun fetchAllFeeds(): List<RssItem> = withContext(Dispatchers.IO) {
        val all = mutableListOf<RssItem>()
        FEED_SOURCES.entries.forEach { (name, url) ->
            try {
                all.addAll(fetchFeed(url, name))
            } catch (e: Exception) {
                Log.w(TAG, "Skipping $name: ${e.message}")
            }
        }
        all.sortedByDescending { it.pubDate }
    }
}
