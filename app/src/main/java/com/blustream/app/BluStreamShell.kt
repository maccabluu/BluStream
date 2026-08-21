package com.blustream.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class BluStreamV07Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateManager.check(this, manual = false)
        setContent { BluStreamV07App() }
    }
}

private enum class BluPage(val label: String, val icon: String) {
    HOME("Home", "⌂"), MOVIES("Movies", "▣"), SHOWS("Shows", "▤"), GENRES("Genres", "◫"),
    SEARCH("Search", "⌕"), MY_STUFF("My Stuff", "♡"), ADDONS("Add-ons", "+"), SETTINGS("Settings", "⚙")
}

private data class BluProfile(
    val id: String,
    val name: String,
    val avatar: String,
    val theme: Int,
    val kids: Boolean
)

private object ProfileStore {
    private const val PREFS = "blustream_profiles_v2"
    private const val KEY = "profiles"

    fun load(context: Context): List<BluProfile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return defaults()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(
                        BluProfile(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            name = item.optString("name").ifBlank { "Profile" },
                            avatar = item.optString("avatar").ifBlank { "●" },
                            theme = item.optInt("theme", 0),
                            kids = item.optBoolean("kids", false)
                        )
                    )
                }
            }.ifEmpty { defaults() }
        }.getOrDefault(defaults())
    }

    fun save(context: Context, profiles: List<BluProfile>) {
        val array = JSONArray()
        profiles.take(5).forEach { p ->
            array.put(JSONObject().put("id", p.id).put("name", p.name).put("avatar", p.avatar).put("theme", p.theme).put("kids", p.kids))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    private fun defaults() = listOf(
        BluProfile(UUID.randomUUID().toString(), "Macca", "M", 0, false),
        BluProfile(UUID.randomUUID().toString(), "Guest", "G", 1, false)
    )
}

private object MyStuffStore {
    private const val PREFS = "blustream_my_stuff"
    fun ids(context: Context, profileId: String): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(profileId, emptySet())?.toSet() ?: emptySet()
    fun toggle(context: Context, profileId: String, id: String): Boolean {
        val set = ids(context, profileId).toMutableSet()
        val added = if (id in set) { set.remove(id); false } else { set.add(id); true }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(profileId, set).apply()
        return added
    }
}

private object SearchHistoryStore {
    private const val PREFS = "blustream_search_history"
    fun load(context: Context, profileId: String): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(profileId, "")
            .orEmpty().split("\n").map { it.trim() }.filter { it.isNotBlank() }.take(8)
    fun add(context: Context, profileId: String, query: String) {
        val next = (listOf(query.trim()) + load(context, profileId)).filter { it.isNotBlank() }.distinct().take(8)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(profileId, next.joinToString("\n")).apply()
    }
    fun clear(context: Context, profileId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(profileId).apply()
    }
}

private fun profileColor(theme: Int): Color = when (theme % 6) {
    1 -> Color(0xFF7057D9)
    2 -> Color(0xFFB23A66)
    3 -> Color(0xFF2F8A65)
    4 -> Color(0xFFD17721)
    5 -> Color(0xFF556579)
    else -> Color(0xFF0B6EB3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluStreamV07App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf(ProfileStore.load(context)) }
    var profile by remember { mutableStateOf(profiles.first()) }
    var showProfiles by remember { mutableStateOf(true) }
    var manageProfiles by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(BluPage.HOME) }
    var selected by remember { mutableStateOf<RealMedia?>(null) }
    var playing by remember { mutableStateOf<Pair<String, String>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF1597FF), background = Color(0xFF020A12), surface = Color(0xFF071624))) {
        when {
            showProfiles -> ProfilePickerV07(profiles, onSelect = { profile = it; showProfiles = false }, onManage = { manageProfiles = true; showProfiles = false })
            manageProfiles -> ManageProfilesV07(profiles, onDone = {
                profiles = it
                ProfileStore.save(context, it)
                profile = it.firstOrNull { p -> p.id == profile.id } ?: it.first()
                manageProfiles = false
                showProfiles = true
            })
            playing != null -> LocalPlayerV07(playing!!.first, playing!!.second) { playing = null }
            selected != null -> RealDetailV07(
                profile = profile,
                media = selected!!,
                onBack = { selected = null },
                onFindSources = { page = BluPage.ADDONS; selected = null }
            )
            else -> ModalNavigationDrawer(
                drawerState = drawer,
                drawerContent = {
                    ModalDrawerSheet(drawerContainerColor = Color(0xFF06111F), modifier = Modifier.width(280.dp)) {
                        Spacer(Modifier.height(24.dp))
                        Text("BLUSTREAM", Modifier.padding(horizontal = 20.dp), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().clickable { showProfiles = true }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(42.dp), shape = CircleShape, color = profileColor(profile.theme)) {
                                Box(contentAlignment = Alignment.Center) { Text(profile.avatar.take(2), color = Color.White, fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column { Text(profile.name, color = Color.White); Text(if (profile.kids) "Kids profile" else "Switch profile", color = Color(0xFF75BEFF), fontSize = 12.sp) }
                        }
                        HorizontalDivider(color = Color(0xFF17304A))
                        BluPage.entries.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) }, icon = { Text(item.icon, fontSize = 20.sp) }, selected = page == item,
                                onClick = { page = item; scope.launch { drawer.close() } }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFF17304A), modifier = Modifier.padding(vertical = 8.dp))
                        NavigationDrawerItem(label = { Text("Manage profiles") }, icon = { Text("✎") }, selected = false,
                            onClick = { manageProfiles = true; scope.launch { drawer.close() } }, modifier = Modifier.padding(horizontal = 10.dp))
                    }
                }
            ) {
                Box(Modifier.fillMaxSize().background(Color(0xFF020A12))) {
                    when (page) {
                        BluPage.HOME -> RealHomeV07(profile, { scope.launch { drawer.open() } }, { showProfiles = true }, { selected = it })
                        BluPage.MOVIES -> RealCatalogPageV07("Movies", profile, { scope.launch { drawer.open() } }, "movie", { selected = it })
                        BluPage.SHOWS -> RealCatalogPageV07("Shows", profile, { scope.launch { drawer.open() } }, "series", { selected = it })
                        BluPage.GENRES -> GenrePageV07(profile, { scope.launch { drawer.open() } }, { selected = it })
                        BluPage.SEARCH -> NativeSearchV07(profile, { scope.launch { drawer.open() } }, { selected = it })
                        BluPage.MY_STUFF -> MyStuffPageV07(profile, { scope.launch { drawer.open() } }, { selected = it })
                        BluPage.ADDONS -> AddonsScreen(
                            profile = profile.name,
                            onProfile = { showProfiles = true },
                            onPlaySource = { source ->
                                when (source.kind) {
                                    BluSourceKind.DIRECT -> source.url?.let { playing = source.name to it }
                                    BluSourceKind.TORRENT -> scope.launch {
                                        message = "Connecting to P2P peers…"
                                        runCatching { P2pEngine.prepare(context.applicationContext, source) }
                                            .onSuccess { prepared -> playing = prepared.title to prepared.url; message = null }
                                            .onFailure { message = it.message ?: "P2P playback failed" }
                                    }
                                    BluSourceKind.EXTERNAL, BluSourceKind.YOUTUBE -> source.playableTarget?.let {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                                    }
                                    else -> message = "This source type has no player yet."
                                }
                            }
                        )
                        BluPage.SETTINGS -> SettingsV07(profile, { scope.launch { drawer.open() } }, { manageProfiles = true })
                    }
                    message?.let { Surface(Modifier.align(Alignment.TopCenter).padding(12.dp), color = Color(0xFF173C67), shape = RoundedCornerShape(12.dp)) { Text(it, Modifier.padding(12.dp), color = Color.White) } }
                }
            }
        }
    }
}

@Composable
private fun TopBarV07(profile: BluProfile, onMenu: () -> Unit, onProfile: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(76.dp).background(Color(0xFF020A12)).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("☰", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { onMenu() })
        Text("BLUSTREAM", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f).padding(start = 26.dp))
        Surface(Modifier.size(44.dp).clickable { onProfile() }, shape = CircleShape, color = profileColor(profile.theme)) {
            Box(contentAlignment = Alignment.Center) { Text(profile.avatar.take(2), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun RealHomeV07(profile: BluProfile, onMenu: () -> Unit, onProfile: () -> Unit, onSelect: (RealMedia) -> Unit) {
    var movies by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var series by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    LaunchedEffect(profile.id) { movies = RealCatalog.topMovies(); series = RealCatalog.topSeries() }
    val hero = movies.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu, onProfile)
        if (hero != null) {
            Box(Modifier.fillMaxWidth().height(370.dp).clickable { onSelect(hero) }) {
                AsyncImage(model = hero.background.ifBlank { hero.poster }, contentDescription = hero.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x22020A12), Color(0xFF020A12)))))
                Column(Modifier.align(Alignment.BottomStart).padding(24.dp).fillMaxWidth(.82f)) {
                    Text("TRENDING", color = Color(0xFF1597FF), fontWeight = FontWeight.Bold)
                    Text(hero.name, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
                    if (hero.releaseInfo.isNotBlank()) Text(hero.releaseInfo, color = Color(0xFFB7C8D8))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSelect(hero) }, shape = RoundedCornerShape(8.dp)) { Text("▶  View title") }
                }
            }
        } else Box(Modifier.fillMaxWidth().height(250.dp).background(Color(0xFF081A2A)), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        MediaRailV07("Popular Movies", movies, onSelect)
        MediaRailV07("Popular Shows", series, onSelect)
        MediaRailV07("More to Explore", (movies.drop(8) + series.drop(8)), onSelect)
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun MediaRailV07(title: String, items: List<RealMedia>, onSelect: (RealMedia) -> Unit) {
    if (items.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("See All", color = Color(0xFF1597FF))
    }
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.take(15).forEach { item ->
            Card(Modifier.width(150.dp).clickable { onSelect(item) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF071624)), shape = RoundedCornerShape(10.dp)) {
                AsyncImage(model = item.poster, contentDescription = item.name, modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
                Column(Modifier.padding(9.dp)) {
                    Text(item.name, color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    Text(item.releaseInfo.ifBlank { item.type.replaceFirstChar { c -> c.uppercase() } }, color = Color(0xFFA8B6C4), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RealCatalogPageV07(title: String, profile: BluProfile, onMenu: () -> Unit, type: String, onSelect: (RealMedia) -> Unit) {
    var items by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    LaunchedEffect(type) { items = if (type == "series") RealCatalog.topSeries() else RealCatalog.topMovies() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu)
        Text(title, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        PosterGridV07(items, onSelect)
    }
}

@Composable
private fun NativeSearchV07(profile: BluProfile, onMenu: () -> Unit, onSelect: (RealMedia) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(SearchHistoryStore.load(context, profile.id)) }

    fun runSearch(value: String) {
        query = value
        if (value.isBlank()) { results = emptyList(); return }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu)
        Text("Search", Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = query, onValueChange = ::runSearch, singleLine = true, label = { Text("Movies and shows") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = query.isNotBlank() && !loading, onClick = { loading = true }) { Text("Search") }
            if (loading) CircularProgressIndicator(Modifier.size(28.dp))
        }
        LaunchedEffect(loading) {
            if (loading) {
                results = RealCatalog.search(query)
                SearchHistoryStore.add(context, profile.id, query)
                history = SearchHistoryStore.load(context, profile.id)
                loading = false
            }
        }
        if (history.isNotEmpty() && results.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent searches", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Clear", color = Color(0xFF1597FF), modifier = Modifier.clickable { SearchHistoryStore.clear(context, profile.id); history = emptyList() })
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                history.forEach { item -> AssistChip(onClick = { query = item; loading = true }, label = { Text(item) }) }
            }
        }
        if (results.isNotEmpty()) {
            Text("Results", Modifier.padding(horizontal = 18.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            PosterGridV07(results, onSelect)
        }
    }
}

@Composable
private fun GenrePageV07(profile: BluProfile, onMenu: () -> Unit, onSelect: (RealMedia) -> Unit) {
    val genres = listOf("Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi", "Thriller")
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var type by remember { mutableStateOf("movie") }
    LaunchedEffect(selectedGenre, type) { selectedGenre?.let { items = RealCatalog.genre(type, it) } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu)
        Text("Genres", Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = type == "movie", onClick = { type = "movie" }, label = { Text("Movies") })
            FilterChip(selected = type == "series", onClick = { type = "series" }, label = { Text("Shows") })
        }
        if (selectedGenre == null) {
            genres.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { genre ->
                        Card(Modifier.weight(1f).height(90.dp).clickable { selectedGenre = genre }, colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2740))) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(genre, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        } else {
            Text("‹ All genres", Modifier.padding(18.dp).clickable { selectedGenre = null; items = emptyList() }, color = Color(0xFF1597FF))
            Text(selectedGenre!!, Modifier.padding(horizontal = 18.dp), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            PosterGridV07(items, onSelect)
        }
    }
}

@Composable
private fun MyStuffPageV07(profile: BluProfile, onMenu: () -> Unit, onSelect: (RealMedia) -> Unit) {
    val context = LocalContext.current
    var movies by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var series by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    val ids = MyStuffStore.ids(context, profile.id)
    LaunchedEffect(profile.id, ids.size) { movies = RealCatalog.topMovies(); series = RealCatalog.topSeries() }
    val items = (movies + series).filter { it.id in ids }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu)
        Text("My Stuff", Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        if (items.isEmpty()) Text("Titles you save will appear here for this profile.", Modifier.padding(18.dp), color = Color(0xFFA8B6C4))
        PosterGridV07(items, onSelect)
    }
}

@Composable
private fun PosterGridV07(items: List<RealMedia>, onSelect: (RealMedia) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    items.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { item ->
                Card(Modifier.weight(1f).clickable { onSelect(item) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF071624))) {
                    AsyncImage(model = item.poster, contentDescription = item.name, modifier = Modifier.fillMaxWidth().aspectRatio(.68f), contentScale = ContentScale.Crop)
                    Text(item.name, Modifier.padding(8.dp), color = Color.White, maxLines = 1, fontSize = 12.sp)
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun RealDetailV07(profile: BluProfile, media: RealMedia, onBack: () -> Unit, onFindSources: () -> Unit) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<RealMediaDetails?>(null) }
    var inList by remember { mutableStateOf(media.id in MyStuffStore.ids(context, profile.id)) }
    var season by remember { mutableIntStateOf(1) }
    LaunchedEffect(media.id) { details = RealCatalog.details(media.type, media.id) }
    val resolved = details?.media ?: media
    val episodes = details?.episodes.orEmpty()
    val seasons = episodes.map { it.season }.filter { it > 0 }.distinct().sorted()
    val shownEpisodes = episodes.filter { it.season == season }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFF020A12))) {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            AsyncImage(model = resolved.background.ifBlank { resolved.poster }, contentDescription = resolved.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF020A12)))))
            Text("‹", color = Color.White, fontSize = 48.sp, modifier = Modifier.padding(18.dp).clickable { onBack() })
        }
        Column(Modifier.padding(20.dp)) {
            Text(resolved.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (resolved.releaseInfo.isNotBlank()) Text(resolved.releaseInfo, color = Color(0xFF91A9BE))
                details?.runtime?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color(0xFF91A9BE)) }
            }
            if (resolved.genres.isNotEmpty()) Text(resolved.genres.joinToString(" • "), color = Color(0xFF1597FF), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            if (resolved.description.isNotBlank()) Text(resolved.description, color = Color(0xFFD5DFE8), lineHeight = 21.sp)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onFindSources) { Text("Find sources") }
                OutlinedButton(onClick = { inList = MyStuffStore.toggle(context, profile.id, resolved.id) }) { Text(if (inList) "✓ My Stuff" else "+ My Stuff") }
            }
            if (details?.cast?.isNotEmpty() == true) {
                Spacer(Modifier.height(18.dp)); Text("Cast", color = Color.White, fontWeight = FontWeight.Bold); Text(details!!.cast.take(8).joinToString(", "), color = Color(0xFFB8C7D5))
            }
            if (seasons.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Episodes", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    seasons.forEach { s -> FilterChip(selected = season == s, onClick = { season = s }, label = { Text("Season $s") }) }
                }
                shownEpisodes.forEach { ep ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071624))) {
                        Row(Modifier.padding(10.dp)) {
                            AsyncImage(model = ep.thumbnail, contentDescription = ep.title, modifier = Modifier.width(120.dp).height(72.dp), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column { Text("${ep.episode}. ${ep.title}", color = Color.White, fontWeight = FontWeight.Bold); if (ep.overview.isNotBlank()) Text(ep.overview, color = Color(0xFFA8B6C4), fontSize = 12.sp, maxLines = 3) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePickerV07(profiles: List<BluProfile>, onSelect: (BluProfile) -> Unit, onManage: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020A12), Color(0xFF08223A)))).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("BLUSTREAM", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp)); Text("Who's watching?", color = Color.White, fontSize = 24.sp); Spacer(Modifier.height(26.dp))
        profiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 9.dp)) {
                row.forEach { p ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(105.dp).clickable { onSelect(p) }) {
                        Surface(Modifier.size(78.dp), shape = RoundedCornerShape(14.dp), color = profileColor(p.theme)) { Box(contentAlignment = Alignment.Center) { Text(p.avatar.take(2), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) } }
                        Spacer(Modifier.height(8.dp)); Text(p.name, color = Color.White, maxLines = 1); if (p.kids) Text("Kids", color = Color(0xFF75BEFF), fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp)); OutlinedButton(onClick = onManage) { Text("Manage Profiles") }
    }
}

@Composable
private fun ManageProfilesV07(initial: List<BluProfile>, onDone: (List<BluProfile>) -> Unit) {
    var profiles by remember { mutableStateOf(initial.ifEmpty { listOf(BluProfile(UUID.randomUUID().toString(), "Profile", "P", 0, false)) }) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFF020A12)).padding(22.dp)) {
        Text("Manage Profiles", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Edit names, avatar letters, theme and Kids mode. Up to 5 profiles.", color = Color(0xFF9CB2C7)); Spacer(Modifier.height(18.dp))
        profiles.forEachIndexed { index, p ->
            Card(Modifier.fillMaxWidth().padding(vertical = 7.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF071624))) {
                Column(Modifier.padding(14.dp)) {
                    OutlinedTextField(value = p.name, onValueChange = { value -> profiles = profiles.toMutableList().also { it[index] = p.copy(name = value.take(24)) } }, label = { Text("Profile name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = p.avatar, onValueChange = { value -> profiles = profiles.toMutableList().also { it[index] = p.copy(avatar = value.take(2)) } }, label = { Text("Avatar") }, modifier = Modifier.width(130.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Theme", color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Surface(Modifier.size(38.dp).clickable { profiles = profiles.toMutableList().also { it[index] = p.copy(theme = (p.theme + 1) % 6) } }, shape = CircleShape, color = profileColor(p.theme)) {}
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = p.kids, onCheckedChange = { checked -> profiles = profiles.toMutableList().also { it[index] = p.copy(kids = checked) } }); Spacer(Modifier.width(8.dp)); Text("Kids profile", color = Color.White) }
                    if (profiles.size > 1) TextButton(onClick = { profiles = profiles.toMutableList().also { it.removeAt(index) } }) { Text("Remove profile") }
                }
            }
        }
        if (profiles.size < 5) Button(onClick = { profiles = profiles + BluProfile(UUID.randomUUID().toString(), "New Profile", "N", profiles.size, false) }) { Text("+ Add Profile") }
        Spacer(Modifier.height(22.dp)); Button(onClick = { onDone(profiles.map { it.copy(name = it.name.trim().ifBlank { "Profile" }, avatar = it.avatar.trim().ifBlank { it.name.take(1).uppercase() }) }) }) { Text("Done") }
    }
}

@Composable
private fun SettingsV07(profile: BluProfile, onMenu: () -> Unit, onProfiles: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color(0xFF020A12))) {
        TopBarV07(profile, onMenu)
        Column(Modifier.padding(20.dp)) {
            Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(18.dp))
            SettingsButtonV07("Profiles", "Names, avatars, themes and Kids mode", onProfiles)
            SettingsButtonV07("Check for updates", "Official BluStream GitHub Releases") { UpdateManager.check(context as ComponentActivity, manual = true) }
            SettingsButtonV07("Playback", "Audio, subtitles and autoplay settings are next") {}
            SettingsButtonV07("About", "BluStream 0.7 Alpha") {}
        }
    }
}

@Composable
private fun SettingsButtonV07(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF071624))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFFA8B6C4), fontSize = 12.sp) }; Text("›", color = Color(0xFF1597FF), fontSize = 24.sp) }
    }
}

@Composable
private fun LocalPlayerV07(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
        Text("‹  $title", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(18.dp).clickable { onBack() })
    }
}
