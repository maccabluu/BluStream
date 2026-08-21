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

private const val ADDON_PREFS = "blustream_addons_v2"
private const val LEGACY_ADDON_PREFS = "blustream_addons"
private const val LEGACY_INSTALLED_ADDONS = "installed_manifest_urls"
private const val DIRECTORY_URL = "https://stremio-addons.net/api/v0/addons?limit=100&nsfw=exclude&sort_by=stars&order=desc"

private val DEFAULT_TRACKERS = listOf(
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://open.stealth.si:80/announce",
    "udp://tracker.torrent.eu.org:451/announce"
)

data class BluAddon(val name: String, val description: String, val manifestUrl: String, val stars: Int = 0)

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
    val stableKey: String get() = listOf(kind.name, url, externalUrl, ytId, infoHash, fileIdx?.toString(), nzbUrl).joinToString("|")
    val playableTarget: String? get() = when (kind) {
        BluSourceKind.DIRECT -> url
        BluSourceKind.EXTERNAL -> externalUrl
        BluSourceKind.YOUTUBE -> ytId?.let { "https://www.youtube.com/watch?v=$it" }
        BluSourceKind.TORRENT -> buildMagnetUri()
        BluSourceKind.NZB -> nzbUrl
        else -> url ?: externalUrl
    }
    private fun buildMagnetUri(): String? {
        url?.takeIf { it.startsWith("magnet:", true) }?.let { return it }
        val hash = infoHash?.takeIf { it.isNotBlank() } ?: return null
        val parsedTrackers = sources.mapNotNull { source ->
            val tracker = source.removePrefix("tracker:").trim()
            tracker.takeIf { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("udp://") }
        }
        val trackers = (parsedTrackers + DEFAULT_TRACKERS).distinct()
        val params = mutableListOf("xt=urn:btih:$hash")
        fileIdx?.let { params += "so=$it" }
        trackers.forEach { params += "tr=" + URLEncoder.encode(it, "UTF-8") }
        return "magnet:?" + params.joinToString("&")
    }
}

private fun BluStreamSource.fastRank(): Int = when (kind) {
    BluSourceKind.DIRECT -> if (url?.startsWith("https://", true) == true) 0 else 1
    BluSourceKind.EXTERNAL -> 2
    BluSourceKind.YOUTUBE -> 3
    BluSourceKind.TORRENT -> 10
    else -> 20
}

private fun preferFastStreams(streams: List<BluStreamSource>): List<BluStreamSource> =
    streams.distinctBy { it.stableKey }.sortedBy { it.fastRank() }

private fun BluStreamSource.speedLabel(): String = when (kind) {
    BluSourceKind.DIRECT -> if (url?.startsWith("https://", true) == true) "FAST HTTPS DIRECT" else "DIRECT HTTP"
    BluSourceKind.EXTERNAL -> "HOSTED / EXTERNAL"
    BluSourceKind.YOUTUBE -> "YOUTUBE"
    BluSourceKind.TORRENT -> "P2P FALLBACK"
    else -> kind.name
}

internal object AddonPrefs {
    private fun key(profileId: String) = "installed_manifest_urls_${profileId.ifBlank { "default" }}"
    fun load(context: Context, profileId: String = "default"): Set<String> {
        val prefs = context.getSharedPreferences(ADDON_PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(key(profileId), null)?.toSet()
        if (saved != null) return saved
        val legacy = context.getSharedPreferences(LEGACY_ADDON_PREFS, Context.MODE_PRIVATE)
            .getStringSet(LEGACY_INSTALLED_ADDONS, emptySet())?.toSet().orEmpty()
        if (legacy.isNotEmpty()) save(context, profileId, legacy)
        return legacy
    }
    fun save(context: Context, profileId: String = "default", urls: Set<String>) {
        context.getSharedPreferences(ADDON_PREFS, Context.MODE_PRIVATE).edit().putStringSet(key(profileId), urls).apply()
    }
}

internal object StremioAddonClient {
    suspend fun loadDirectory(): List<BluAddon> = withContext(Dispatchers.IO) {
        val root = JSONObject(getJson(DIRECTORY_URL))
        val array = root.optJSONArray("addons") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val manifest = item.optJSONObject("manifest")
                val manifestUrl = item.optString("manifestUrl", "").trim()
                if (manifestUrl.isBlank()) continue
                add(BluAddon(manifest?.optString("name")?.takeIf { it.isNotBlank() } ?: item.optString("slug", "Unnamed add-on"), manifest?.optString("description", "") ?: "", manifestUrl, item.optInt("stars", 0)))
            }
        }.distinctBy { it.manifestUrl }
    }

    suspend fun loadManifest(manifestUrl: String): BluAddon = withContext(Dispatchers.IO) {
        val safe = manifestUrl.trim()
        require(safe.startsWith("https://") || safe.startsWith("http://"))
        val manifest = JSONObject(getJson(safe))
        val name = manifest.optString("name", "").trim()
        require(name.isNotBlank())
        BluAddon(name, manifest.optString("description", ""), safe)
    }

    suspend fun resolveStreams(manifestUrl: String, type: String, id: String): List<BluStreamSource> = withContext(Dispatchers.IO) {
        val base = manifestUrl.substringBeforeLast("/manifest.json")
        val endpoint = "$base/stream/${if (type == "series") "series" else "movie"}/${URLEncoder.encode(id.trim(), "UTF-8")}.json"
        val streams = JSONObject(getJson(endpoint)).optJSONArray("streams") ?: return@withContext emptyList()
        preferFastStreams(buildList {
            for (i in 0 until streams.length()) {
                val s = streams.optJSONObject(i) ?: continue
                val url = s.optString("url", "").takeIf { it.isNotBlank() }
                val external = s.optString("externalUrl", "").takeIf { it.isNotBlank() }
                val yt = s.optString("ytId", "").takeIf { it.isNotBlank() }
                val hash = s.optString("infoHash", "").takeIf { it.isNotBlank() }
                val fileIdx = if (s.has("fileIdx")) s.optInt("fileIdx") else null
                val sourceArray = s.optJSONArray("sources")
                val sourceHints = buildList {
                    if (sourceArray != null) for (j in 0 until sourceArray.length()) sourceArray.optString(j).takeIf { it.isNotBlank() }?.let(::add)
                }
                val kind = when {
                    url?.startsWith("magnet:", true) == true || hash != null -> BluSourceKind.TORRENT
                    url != null -> BluSourceKind.DIRECT
                    external != null -> BluSourceKind.EXTERNAL
                    yt != null -> BluSourceKind.YOUTUBE
                    else -> BluSourceKind.UNKNOWN
                }
                add(BluStreamSource(s.optString("name", "Stream ${i + 1}"), s.optString("description", s.optString("title", "")), kind, url, external, yt, hash, fileIdx, sourceHints, rawJson = s.toString()))
            }
        })
    }

    private fun getJson(address: String): String {
        val c = URL(address).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("User-Agent", "BluStream/3.x")
            val code = c.responseCode
            if (code !in 200..299) error("Server returned HTTP $code")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}

suspend fun resolveInstalledAddonStreams(context: Context, profileId: String, type: String, id: String): List<BluStreamSource> {
    val found = mutableListOf<BluStreamSource>()
    AddonPrefs.load(context, profileId).forEach { manifest ->
        runCatching { StremioAddonClient.resolveStreams(manifest, type, id) }.getOrNull()?.let(found::addAll)
    }
    return preferFastStreams(found)
}

@Composable
fun AddonsScreen(profileId: String, profileName: String, onProfile: () -> Unit, onPlaySource: (BluStreamSource) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installed by remember(profileId) { mutableStateOf(AddonPrefs.load(context, profileId)) }
    var directory by remember { mutableStateOf<List<BluAddon>>(emptyList()) }
    var manualUrl by remember { mutableStateOf("") }
    var mediaId by remember { mutableStateOf("") }
    var contentType by remember { mutableStateOf("movie") }
    var selectedManifest by remember(profileId) { mutableStateOf<String?>(installed.firstOrNull()) }
    var sources by remember { mutableStateOf<List<BluStreamSource>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Browse Stremio-compatible add-ons or install a manifest URL.") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Header(profileName, onProfile); Text("Add-ons", color = Color.White, fontSize = 26.sp); Text(message, color = Color(0xFFB8C9DC)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !loading, onClick = { scope.launch { loading = true; runCatching { StremioAddonClient.loadDirectory() }.onSuccess { directory = it; message = "Loaded ${it.size} add-ons." }.onFailure { message = it.message ?: "Directory failed" }; loading = false } }) { Text("Browse directory") }
                OutlinedButton(enabled = !loading && installed.isNotEmpty(), onClick = { message = "Checked ${installed.size} installed add-ons." }) { Text("Refresh") }
            }
        }
        item {
            OutlinedTextField(manualUrl, { manualUrl = it.take(2048) }, Modifier.fillMaxWidth(), label = { Text("Manifest URL") })
            Button(enabled = !loading && manualUrl.startsWith("http"), onClick = { scope.launch { loading = true; runCatching { StremioAddonClient.loadManifest(manualUrl.trim()) }.onSuccess { addon -> installed = installed + addon.manifestUrl; AddonPrefs.save(context, profileId, installed); selectedManifest = addon.manifestUrl; message = "Installed ${addon.name}." }.onFailure { message = it.message ?: "Install failed" }; loading = false } }) { Text("Install add-on") }
        }
        if (installed.isNotEmpty()) {
            item { Text("Installed for $profileName", color = Color.White, fontSize = 20.sp) }
            items(installed.toList()) { url -> Card(Modifier.fillMaxWidth().clickable { selectedManifest = url }.focusable()) { Column(Modifier.padding(14.dp)) { Text(url); Text("Remove", Modifier.clickable { installed = installed - url; AddonPrefs.save(context, profileId, installed) }, color = Color(0xFF159CFF)) } } }
        }
        if (directory.isNotEmpty()) {
            item { Text("Directory", color = Color.White, fontSize = 20.sp) }
            items(directory) { addon -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(addon.name); Text(addon.description, maxLines = 2); Button(enabled = addon.manifestUrl !in installed, onClick = { scope.launch { runCatching { StremioAddonClient.loadManifest(addon.manifestUrl) }.onSuccess { installed = installed + addon.manifestUrl; AddonPrefs.save(context, profileId, installed); selectedManifest = addon.manifestUrl } } }) { Text(if (addon.manifestUrl in installed) "Installed" else "Install") } } } }
        }
        item {
            HorizontalDivider(); Text("Find streams", color = Color.White, fontSize = 20.sp)
            Row { FilterChip(contentType == "movie", { contentType = "movie" }, { Text("Movie") }); Spacer(Modifier.width(8.dp)); FilterChip(contentType == "series", { contentType = "series" }, { Text("Series") }) }
            OutlinedTextField(mediaId, { mediaId = it }, Modifier.fillMaxWidth(), label = { Text("Media ID") })
            Button(enabled = selectedManifest != null && mediaId.isNotBlank() && !loading, onClick = { val manifest = selectedManifest ?: return@Button; scope.launch { loading = true; runCatching { StremioAddonClient.resolveStreams(manifest, contentType, mediaId) }.onSuccess { sources = it; message = if (it.isEmpty()) "No sources were returned by this add-on." else "Found ${it.size} source(s). HTTPS direct streams appear first. P2P is fallback only." }.onFailure { message = it.message ?: "Lookup failed" }; loading = false } }) { Text("Find streams") }
        }
        items(sources) { source -> Card(Modifier.fillMaxWidth().clickable { onPlaySource(source) }) { Column(Modifier.padding(14.dp)) { Text(source.name); Text(source.speedLabel()) } } }
    }
}