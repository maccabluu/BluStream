package com.blustream.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val ADDON_PREFS = "blustream_addons"
private const val INSTALLED_ADDONS = "installed_manifest_urls"
private const val DIRECTORY_URL = "https://stremio-addons.net/api/v0/addons?limit=100&nsfw=exclude&sort_by=stars&order=desc"

data class BluAddon(
    val name: String,
    val description: String,
    val manifestUrl: String,
    val stars: Int = 0
)

enum class BluSourceKind { DIRECT, TORRENT, EXTERNAL, YOUTUBE, NZB, ARCHIVE, UNKNOWN }

data class BluStreamSource(
    val name: String,
    val description: String,
    val kind: BluSourceKind,
    val url: String? = null,
    val externalUrl: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val sources: List<String> = emptyList(),
    val nzbUrl: String? = null,
    val rawJson: String = ""
) {
    val stableKey: String
        get() = listOf(kind.name, url, externalUrl, ytId, infoHash, fileIdx?.toString(), nzbUrl)
            .joinToString("|")

    val playableTarget: String?
        get() = when (kind) {
            BluSourceKind.DIRECT -> url
            BluSourceKind.EXTERNAL -> externalUrl
            BluSourceKind.YOUTUBE -> ytId?.let { "https://www.youtube.com/watch?v=$it" }
            BluSourceKind.TORRENT -> buildMagnetUri()
            BluSourceKind.NZB -> nzbUrl
            else -> url ?: externalUrl
        }

    private fun buildMagnetUri(): String? {
        val hash = infoHash?.takeIf { it.isNotBlank() } ?: return url?.takeIf { it.startsWith("magnet:", true) }
        val trackerParams = sources
            .mapNotNull { source ->
                val tracker = source.removePrefix("tracker:")
                if (tracker.startsWith("http://") || tracker.startsWith("https://") || tracker.startsWith("udp://")) {
                    "tr=" + URLEncoder.encode(tracker, "UTF-8")
                } else null
            }
        val fileParam = fileIdx?.let { "so=$it" }
        return buildList {
            add("magnet:?xt=urn:btih:$hash")
            if (fileParam != null) add(fileParam)
            addAll(trackerParams)
        }.joinToString("&")
    }
}

private object AddonPrefs {
    fun load(context: Context): Set<String> =
        context.getSharedPreferences(ADDON_PREFS, Context.MODE_PRIVATE)
            .getStringSet(INSTALLED_ADDONS, emptySet())?.toSet() ?: emptySet()

    fun save(context: Context, urls: Set<String>) {
        context.getSharedPreferences(ADDON_PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(INSTALLED_ADDONS, urls).apply()
    }
}

private object StremioAddonClient {
    suspend fun loadDirectory(): List<BluAddon> = withContext(Dispatchers.IO) {
        val root = JSONObject(getJson(DIRECTORY_URL))
        val array = root.optJSONArray("addons") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val manifest = item.optJSONObject("manifest")
                val manifestUrl = item.optString("manifestUrl", "")
                if (manifestUrl.isBlank()) continue
                add(
                    BluAddon(
                        name = manifest?.optString("name")?.takeIf { it.isNotBlank() }
                            ?: item.optString("slug", "Unnamed add-on"),
                        description = manifest?.optString("description", "") ?: "",
                        manifestUrl = manifestUrl,
                        stars = item.optInt("stars", 0)
                    )
                )
            }
        }
    }

    suspend fun loadManifest(manifestUrl: String): BluAddon = withContext(Dispatchers.IO) {
        require(manifestUrl.startsWith("https://") || manifestUrl.startsWith("http://")) {
            "Manifest URL must use HTTP or HTTPS."
        }
        val manifest = JSONObject(getJson(manifestUrl))
        BluAddon(
            name = manifest.optString("name", "Stremio add-on"),
            description = manifest.optString("description", ""),
            manifestUrl = manifestUrl
        )
    }

    suspend fun resolveStreams(manifestUrl: String, type: String, id: String): List<BluStreamSource> = withContext(Dispatchers.IO) {
        val base = manifestUrl.substringBeforeLast("/manifest.json")
        val safeType = if (type == "series") "series" else "movie"
        val safeId = URLEncoder.encode(id.trim(), "UTF-8")
        val endpoint = "$base/stream/$safeType/$safeId.json"
        val root = JSONObject(getJson(endpoint))
        val streams = root.optJSONArray("streams") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until streams.length()) {
                val stream = streams.optJSONObject(i) ?: continue
                val url = stream.optString("url", "").takeIf { it.isNotBlank() }
                val external = stream.optString("externalUrl", "").takeIf { it.isNotBlank() }
                val ytId = (stream.optString("ytId", "").ifBlank { stream.optString("yt_id", "") }).takeIf { it.isNotBlank() }
                val infoHash = stream.optString("infoHash", "").takeIf { it.isNotBlank() }
                val nzbUrl = stream.optString("nzbUrl", "").takeIf { it.isNotBlank() }
                val fileIdx = when {
                    stream.has("fileIdx") -> stream.optInt("fileIdx")
                    stream.has("mapIdx") -> stream.optInt("mapIdx")
                    else -> null
                }
                val sourceArray = stream.optJSONArray("sources")
                val sourceHints = buildList {
                    if (sourceArray != null) {
                        for (j in 0 until sourceArray.length()) {
                            sourceArray.optString(j).takeIf { it.isNotBlank() }?.let { add(it) }
                        }
                    }
                }
                val archivePresent = listOf("rarUrls", "zipUrls", "7zipUrls", "tgzUrls", "tarUrls").any { stream.has(it) }
                val kind = when {
                    url?.startsWith("magnet:", true) == true -> BluSourceKind.TORRENT
                    infoHash != null -> BluSourceKind.TORRENT
                    url != null -> BluSourceKind.DIRECT
                    external != null -> BluSourceKind.EXTERNAL
                    ytId != null -> BluSourceKind.YOUTUBE
                    nzbUrl != null -> BluSourceKind.NZB
                    archivePresent -> BluSourceKind.ARCHIVE
                    else -> BluSourceKind.UNKNOWN
                }
                add(
                    BluStreamSource(
                        name = stream.optString("name", "Stream ${i + 1}"),
                        description = stream.optString("description", stream.optString("title", "")),
                        kind = kind,
                        url = url,
                        externalUrl = external,
                        ytId = ytId,
                        infoHash = infoHash,
                        fileIdx = fileIdx,
                        sources = sourceHints,
                        nzbUrl = nzbUrl,
                        rawJson = stream.toString()
                    )
                )
            }
        }
    }

    private fun getJson(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BluStream/0.5.0-alpha")
            val code = connection.responseCode
            if (code !in 200..299) error("Server returned HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun AddonsScreen(
    profile: String,
    onProfile: () -> Unit,
    onPlaySource: (BluStreamSource) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember { mutableStateOf(AddonPrefs.load(context)) }
    var directory by remember { mutableStateOf<List<BluAddon>>(emptyList()) }
    var manualUrl by remember { mutableStateOf("") }
    var mediaId by remember { mutableStateOf("") }
    var contentType by remember { mutableStateOf("movie") }
    var selectedManifest by remember { mutableStateOf<String?>(installed.firstOrNull()) }
    var sources by remember { mutableStateOf<List<BluStreamSource>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Browse Stremio-compatible add-ons or install a manifest URL.") }

    fun install(url: String) {
        scope.launch {
            loading = true
            runCatching { StremioAddonClient.loadManifest(url.trim()) }
                .onSuccess { addon ->
                    installed = installed + addon.manifestUrl
                    AddonPrefs.save(context, installed)
                    selectedManifest = addon.manifestUrl
                    message = "Installed ${addon.name}."
                }
                .onFailure { message = it.message ?: "Could not install add-on." }
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header(profile, onProfile) }
        item {
            Text("Add-ons", color = Color.White, fontSize = 26.sp)
            Text("Directory data: stremio-addons.net", color = Color(0xFF8FA9C3), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Color(0xFFB8C9DC))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !loading,
                    onClick = {
                        scope.launch {
                            loading = true
                            runCatching { StremioAddonClient.loadDirectory() }
                                .onSuccess {
                                    directory = it
                                    message = "Loaded ${it.size} directory add-ons."
                                }
                                .onFailure { message = it.message ?: "Directory could not be loaded." }
                            loading = false
                        }
                    }
                ) { Text("Browse directory") }
                if (loading) CircularProgressIndicator(Modifier.size(28.dp))
            }
        }
        item {
            OutlinedTextField(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Manifest URL") },
                placeholder = { Text("https://example.com/manifest.json") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = (manualUrl.startsWith("https://") || manualUrl.startsWith("http://")) && !loading,
                onClick = { install(manualUrl) }
            ) { Text("Install add-on") }
        }
        if (installed.isNotEmpty()) {
            item { Text("Installed", color = Color.White, fontSize = 20.sp) }
            items(installed.toList(), key = { it }) { url ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedManifest = url }.focusable(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedManifest == url) Color(0xFF173C67) else Color(0xFF10243D)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(url, color = Color.White, maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Remove",
                            modifier = Modifier.clickable {
                                installed = installed - url
                                AddonPrefs.save(context, installed)
                                if (selectedManifest == url) selectedManifest = installed.firstOrNull()
                            }.focusable(),
                            color = Color(0xFF8EC5FF)
                        )
                    }
                }
            }
        }
        if (directory.isNotEmpty()) {
            item { Text("Directory", color = Color.White, fontSize = 20.sp) }
            items(directory, key = { it.manifestUrl }) { addon ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(addon.name, color = Color.White, fontSize = 17.sp)
                        if (addon.description.isNotBlank()) Text(addon.description, color = Color(0xFFB8C9DC), maxLines = 3)
                        Text("★ ${addon.stars}", color = Color(0xFF8FA9C3))
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { install(addon.manifestUrl) }) { Text("Install") }
                    }
                }
            }
        }
        item {
            Divider()
            Text("Find streams", color = Color.White, fontSize = 20.sp)
            Text("Enter a media ID supported by your selected add-on.", color = Color(0xFF8FA9C3), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = contentType == "movie", onClick = { contentType = "movie" }, label = { Text("Movie") })
                FilterChip(selected = contentType == "series", onClick = { contentType = "series" }, label = { Text("Series") })
            }
            OutlinedTextField(
                value = mediaId,
                onValueChange = { mediaId = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Media ID") },
                placeholder = { Text("tt1234567 or tt1234567:1:1") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = selectedManifest != null && mediaId.isNotBlank() && !loading,
                onClick = {
                    val manifest = selectedManifest ?: return@Button
                    scope.launch {
                        loading = true
                        runCatching { StremioAddonClient.resolveStreams(manifest, contentType, mediaId) }
                            .onSuccess {
                                sources = it
                                message = if (it.isEmpty()) "No streams were returned." else "Found ${it.size} stream source(s)."
                            }
                            .onFailure { message = it.message ?: "Stream lookup failed." }
                        loading = false
                    }
                }
            ) { Text("Find streams") }
        }
        if (sources.isNotEmpty()) {
            items(sources, key = { it.stableKey }) { source ->
                Card(
                    Modifier.fillMaxWidth().clickable { onPlaySource(source) }.focusable(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10243D))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(source.name, color = Color.White)
                        Text(source.kind.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color(0xFF8EC5FF), fontSize = 12.sp)
                        if (source.description.isNotBlank()) Text(source.description, color = Color(0xFFB8C9DC), maxLines = 3)
                        if (source.infoHash != null) Text("Torrent ${source.infoHash.take(12)}…", color = Color(0xFF8FA9C3), fontSize = 12.sp)
                        Text(if (source.kind == BluSourceKind.TORRENT) "Stream P2P" else "Open", color = Color(0xFF8EC5FF))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}
