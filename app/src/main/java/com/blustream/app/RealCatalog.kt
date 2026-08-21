package com.blustream.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class RealMedia(
    val id: String,
    val type: String,
    val name: String,
    val poster: String,
    val background: String,
    val description: String,
    val releaseInfo: String,
    val genres: List<String> = emptyList()
)

internal data class RealEpisode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val thumbnail: String,
    val overview: String,
    val released: String
)

internal data class RealMediaDetails(
    val media: RealMedia,
    val runtime: String,
    val cast: List<String>,
    val director: List<String>,
    val episodes: List<RealEpisode>
)

internal object RealCatalog {
    private const val BASE = "https://v3-cinemeta.strem.io"

    suspend fun topMovies(): List<RealMedia> = loadCatalog("movie", "top")
    suspend fun topSeries(): List<RealMedia> = loadCatalog("series", "top")

    suspend fun search(query: String): List<RealMedia> {
        val text = query.trim()
        if (text.isBlank()) return emptyList()
        val extra = "search=" + encodeExtra(text)
        val movies = loadCatalog("movie", "top", extra)
        val series = loadCatalog("series", "top", extra)
        return (movies + series).distinctBy { it.id }.take(60)
    }

    suspend fun genre(type: String, genre: String): List<RealMedia> {
        val safeType = if (type == "series") "series" else "movie"
        return loadCatalog(safeType, "top", "genre=" + encodeExtra(genre))
    }

    suspend fun details(type: String, id: String): RealMediaDetails? = withContext(Dispatchers.IO) {
        val safeType = if (type == "series") "series" else "movie"
        val root = getJson("$BASE/meta/$safeType/${encodeExtra(id)}.json") ?: return@withContext null
        val meta = root.optJSONObject("meta") ?: return@withContext null
        val media = parseMedia(meta, safeType) ?: return@withContext null
        val videos = meta.optJSONArray("videos")
        val episodes = buildList {
            if (videos != null) {
                for (i in 0 until videos.length()) {
                    val v = videos.optJSONObject(i) ?: continue
                    add(
                        RealEpisode(
                            id = v.optString("id"),
                            title = v.optString("title").ifBlank { "Episode ${v.optInt("episode", i + 1)}" },
                            season = v.optInt("season", 0),
                            episode = v.optInt("episode", i + 1),
                            thumbnail = v.optString("thumbnail", media.background),
                            overview = v.optString("overview", ""),
                            released = v.optString("released", "")
                        )
                    )
                }
            }
        }
        RealMediaDetails(
            media = media,
            runtime = meta.optString("runtime", ""),
            cast = jsonStrings(meta.optJSONArray("cast")),
            director = jsonStrings(meta.optJSONArray("director")),
            episodes = episodes
        )
    }

    private suspend fun loadCatalog(type: String, id: String, extra: String? = null): List<RealMedia> = withContext(Dispatchers.IO) {
        val suffix = if (extra.isNullOrBlank()) "" else "/$extra"
        val root = getJson("$BASE/catalog/$type/$id$suffix.json") ?: return@withContext emptyList()
        val metas = root.optJSONArray("metas") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until metas.length()) {
                parseMedia(metas.optJSONObject(i), type)?.let(::add)
            }
        }
    }

    private fun parseMedia(item: JSONObject?, fallbackType: String): RealMedia? {
        item ?: return null
        val id = item.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = item.optString("name").takeIf { it.isNotBlank() } ?: return null
        return RealMedia(
            id = id,
            type = item.optString("type", fallbackType).ifBlank { fallbackType },
            name = name,
            poster = item.optString("poster", ""),
            background = item.optString("background", item.optString("poster", "")),
            description = item.optString("description", ""),
            releaseInfo = item.optString("releaseInfo", item.optString("year", "")),
            genres = jsonStrings(item.optJSONArray("genres"))
        )
    }

    private fun getJson(address: String): JSONObject? {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BluStream-Android")
            if (connection.responseCode !in 200..299) null
            else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (_: Throwable) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun jsonStrings(array: org.json.JSONArray?): List<String> = buildList {
        if (array != null) for (i in 0 until array.length()) array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
    }

    private fun encodeExtra(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
