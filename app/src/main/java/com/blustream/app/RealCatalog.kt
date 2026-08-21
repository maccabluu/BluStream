package com.blustream.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class RealMedia(
    val id: String,
    val type: String,
    val name: String,
    val poster: String,
    val background: String,
    val description: String,
    val releaseInfo: String
)

internal object RealCatalog {
    private const val BASE = "https://v3-cinemeta.strem.io"

    suspend fun topMovies(): List<RealMedia> = load("$BASE/catalog/movie/top.json", "movie")
    suspend fun topSeries(): List<RealMedia> = load("$BASE/catalog/series/top.json", "series")

    private suspend fun load(address: String, fallbackType: String): List<RealMedia> = withContext(Dispatchers.IO) {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BluStream-Android")
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val metas = root.optJSONArray("metas") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until metas.length()) {
                    val item = metas.optJSONObject(i) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                    add(
                        RealMedia(
                            id = id,
                            type = item.optString("type", fallbackType).ifBlank { fallbackType },
                            name = name,
                            poster = item.optString("poster", ""),
                            background = item.optString("background", item.optString("poster", "")),
                            description = item.optString("description", ""),
                            releaseInfo = item.optString("releaseInfo", item.optString("year", ""))
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
