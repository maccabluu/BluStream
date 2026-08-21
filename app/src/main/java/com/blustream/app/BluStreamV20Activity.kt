package com.blustream.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.UUID

class BluStreamV20Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateManager.check(this, manual = false)
        setContent { BluStreamV20App() }
    }
}

private data class V20Profile(
    val id: String,
    val name: String,
    val avatar: String,
    val kids: Boolean
)

private object V20ProfileStore {
    private const val PREFS = "blustream_v20_profiles"
    private const val KEY = "profiles"
    private const val LAST = "last_profile"

    fun load(context: Context): List<V20Profile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw.isNullOrBlank()) {
            val starter = listOf(V20Profile("macca", "Macca", "😎", false))
            save(context, starter)
            return starter
        }
        return raw.split(";;").mapNotNull { row ->
            val p = row.split("|")
            if (p.size < 4) null else V20Profile(p[0], p[1], p[2], p[3] == "1")
        }.ifEmpty { listOf(V20Profile("macca", "Macca", "😎", false)) }
    }

    fun save(context: Context, profiles: List<V20Profile>) {
        val raw = profiles.joinToString(";;") { "${it.id}|${it.name}|${it.avatar}|${if (it.kids) 1 else 0}" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, raw).apply()
    }

    fun last(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LAST, null)
    fun setLast(context: Context, id: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LAST, id).apply()
}

private enum class V20Screen { HOME, MOVIES, SHOWS, SEARCH, MY_STUFF, ADDONS, PROFILES, SETTINGS }

@Composable
fun BluStreamV20App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf(V20ProfileStore.load(context)) }
    var active by remember { mutableStateOf<V20Profile?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(V20Screen.HOME) }
    var selected by remember { mutableStateOf<RealMedia?>(null) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var playingTitle by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profiles) {
        if (active == null) {
            val last = V20ProfileStore.last(context)
            active = if (profiles.size == 1) profiles.first() else profiles.firstOrNull { it.id == last }
            showPicker = active == null
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF159CFF), background = Color(0xFF020C16), surface = Color(0xFF071827))) {
        when {
            showPicker || active == null -> V20ProfilePicker(
                profiles = profiles,
                onSelect = {
                    active = it
                    V20ProfileStore.setLast(context, it.id)
                    showPicker = false
                    screen = V20Screen.HOME
                },
                onManage = { screen = V20Screen.PROFILES; showPicker = false; active = profiles.firstOrNull() }
            )
            playingUrl != null -> V20Player(playingTitle, playingUrl!!) { playingUrl = null }
            selected != null -> V20Details(
                media = selected!!,
                profile = active!!,
                onBack = { selected = null },
                onPlay = { title, url -> playingTitle = title; playingUrl = url },
                onMessage = { message = it }
            )
            else -> V20Shell(
                profile = active!!,
                screen = screen,
                onScreen = { screen = it },
                onProfile = { showPicker = true },
                content = {
                    when (screen) {
                        V20Screen.HOME -> V20Home(active!!, onSelect = { selected = it }, onSeeAll = { screen = it })
                        V20Screen.MOVIES -> V20Catalog("Movies", active!!, "movie", onSelect = { selected = it })
                        V20Screen.SHOWS -> V20Catalog("Shows", active!!, "series", onSelect = { selected = it })
                        V20Screen.SEARCH -> V20Search(active!!, onSelect = { selected = it })
                        V20Screen.MY_STUFF -> V20MyStuff(active!!, onSelect = { selected = it })
                        V20Screen.ADDONS -> AddonsScreen(
                            profileId = active!!.id,
                            profileName = active!!.name,
                            onProfile = { showPicker = true },
                            onPlaySource = { source ->
                                when (source.kind) {
                                    BluSourceKind.DIRECT -> source.url?.let { playingTitle = source.name; playingUrl = it }
                                    BluSourceKind.TORRENT -> {
                                        message = "Connecting to P2P peers…"
                                        scope.launch {
                                            runCatching { P2pEngine.prepare(context.applicationContext, source) }
                                                .onSuccess { prepared -> playingTitle = source.name.ifBlank { prepared.title }; playingUrl = prepared.url; message = null }
                                                .onFailure { message = it.message ?: "P2P playback failed" }
                                        }
                                    }
                                    BluSourceKind.EXTERNAL, BluSourceKind.YOUTUBE -> source.playableTarget?.let {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                                            .onFailure { message = "No app is available to open this source." }
                                    }
                                    else -> message = "This source type does not have a player yet."
                                }
                            }
                        )
                        V20Screen.PROFILES -> V20ManageProfiles(
                            profiles = profiles,
                            onSave = {
                                profiles = it
                                V20ProfileStore.save(context, it)
                                active = it.firstOrNull { p -> p.id == active?.id } ?: it.firstOrNull()
                            }
                        )
                        V20Screen.SETTINGS -> V20Settings(active!!)
                    }
                }
            )
        }

        message?.let {
            Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
        }
    }
}

@Composable
private fun V20ProfilePicker(profiles: List<V20Profile>, onSelect: (V20Profile) -> Unit, onManage: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020912), Color(0xFF06213A)))).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("BLUSTREAM", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Who's watching?", color = Color(0xFFBCD2E5), fontSize = 22.sp)
        Spacer(Modifier.height(28.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            items(profiles) { p ->
                Column(Modifier.width(110.dp).clickable { onSelect(p) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(Modifier.size(92.dp), shape = CircleShape, color = if (p.kids) Color(0xFF33A56D) else Color(0xFF1168B3)) {
                        Box(contentAlignment = Alignment.Center) { Text(p.avatar, fontSize = 48.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    if (p.kids) Text("Kids", color = Color(0xFF80E0AC), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onManage) { Text("Manage Profiles") }
    }
}

@Composable
private fun V20Shell(profile: V20Profile, screen: V20Screen, onScreen: (V20Screen) -> Unit, onProfile: () -> Unit, content: @Composable () -> Unit) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF071827)) {
                Spacer(Modifier.height(18.dp))
                Text("BLUSTREAM 2.0", Modifier.padding(18.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                val entries = listOf(
                    V20Screen.HOME to "Home", V20Screen.MOVIES to "Movies", V20Screen.SHOWS to "Shows", V20Screen.SEARCH to "Search",
                    V20Screen.MY_STUFF to "My Stuff", V20Screen.ADDONS to "Add-ons", V20Screen.PROFILES to "Profiles", V20Screen.SETTINGS to "Settings"
                )
                entries.forEach { (target, label) ->
                    NavigationDrawerItem(label = { Text(label) }, selected = screen == target, onClick = { onScreen(target); scope.launch { drawer.close() } }, modifier = Modifier.padding(horizontal = 10.dp))
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(Color(0xFF020C16))) {
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("☰", color = Color.White, fontSize = 32.sp, modifier = Modifier.clickable { scope.launch { drawer.open() } })
                Text("BLUSTREAM", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Surface(Modifier.size(42.dp).clickable(onClick = onProfile), shape = CircleShape, color = if (profile.kids) Color(0xFF33A56D) else Color(0xFF1168B3)) {
                    Box(contentAlignment = Alignment.Center) { Text(profile.avatar, fontSize = 24.sp) }
                }
            }
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
private fun V20Home(profile: V20Profile, onSelect: (RealMedia) -> Unit, onSeeAll: (V20Screen) -> Unit) {
    var movies by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var shows by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    LaunchedEffect(profile.id, profile.kids) {
        val m = if (profile.kids) RealCatalog.genre("movie", "Animation") + RealCatalog.genre("movie", "Family") else RealCatalog.topMovies()
        val s = if (profile.kids) RealCatalog.genre("series", "Animation") + RealCatalog.genre("series", "Family") else RealCatalog.topSeries()
        movies = m.distinctBy { it.id }.take(30)
        shows = s.distinctBy { it.id }.take(30)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            val hero = movies.firstOrNull() ?: shows.firstOrNull()
            if (hero != null) V20Hero(hero, onSelect)
        }
        item { V20Rail("Popular Movies", movies, { onSeeAll(V20Screen.MOVIES) }, onSelect) }
        item { V20Rail("Popular Shows", shows, { onSeeAll(V20Screen.SHOWS) }, onSelect) }
        if (profile.kids) item { Text("Kids profile is filtering the catalogue to family and animation titles.", Modifier.padding(18.dp), color = Color(0xFF80E0AC)) }
    }
}

@Composable
private fun V20Hero(media: RealMedia, onSelect: (RealMedia) -> Unit) {
    Box(Modifier.fillMaxWidth().height(330.dp).clickable { onSelect(media) }) {
        AsyncImage(model = media.background, contentDescription = media.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE020C16)))))
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text("TRENDING", color = Color(0xFF159CFF), fontWeight = FontWeight.Bold)
            Text(media.name, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(media.releaseInfo, color = Color(0xFFB5C6D7))
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onSelect(media) }) { Text("View title") }
        }
    }
}

@Composable
private fun V20Rail(title: String, items: List<RealMedia>, onSeeAll: () -> Unit, onSelect: (RealMedia) -> Unit) {
    Column(Modifier.padding(top = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("See All", color = Color(0xFF159CFF), modifier = Modifier.clickable(onClick = onSeeAll))
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { media -> V20Poster(media, onSelect) }
        }
    }
}

@Composable
private fun V20Poster(media: RealMedia, onSelect: (RealMedia) -> Unit) {
    Column(Modifier.width(150.dp).clickable { onSelect(media) }) {
        Card(shape = RoundedCornerShape(12.dp)) {
            AsyncImage(model = media.poster, contentDescription = media.name, modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(6.dp))
        Text(media.name, color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
        Text(media.releaseInfo, color = Color(0xFF93A9BD), fontSize = 12.sp)
    }
}

@Composable
private fun V20Catalog(title: String, profile: V20Profile, type: String, onSelect: (RealMedia) -> Unit) {
    var items by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    LaunchedEffect(profile.id, profile.kids, type) {
        items = if (profile.kids) {
            (RealCatalog.genre(type, "Animation") + RealCatalog.genre(type, "Family")).distinctBy { it.id }.take(60)
        } else if (type == "series") RealCatalog.topSeries() else RealCatalog.topMovies()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        items(items.chunked(3)) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { media -> Box(Modifier.weight(1f)) { V20Poster(media, onSelect) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun V20Search(profile: V20Profile, onSelect: (RealMedia) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Search", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.weight(1f), placeholder = { Text("Movies and shows") }, singleLine = true)
            Button(onClick = { scope.launch { results = RealCatalog.search(query).filter { !profile.kids || it.genres.any { g -> g.equals("Animation", true) || g.equals("Family", true) } } } }) { Text("Search") }
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results) { media ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(media) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(media.poster, media.name, Modifier.size(72.dp, 105.dp), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column { Text(media.name, color = Color.White, fontWeight = FontWeight.Bold); Text(media.releaseInfo, color = Color(0xFF93A9BD)) }
                }
            }
        }
    }
}

@Composable
private fun V20MyStuff(profile: V20Profile, onSelect: (RealMedia) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("My Stuff", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Watchlist and progress for ${profile.name}", color = Color(0xFF93A9BD))
            Spacer(Modifier.height(10.dp))
            Text("Saved titles and progress tracking are being wired into the 2.0 cycle.", color = Color(0xFFBCD2E5))
        }
    }
}

@Composable
private fun V20ManageProfiles(profiles: List<V20Profile>, onSave: (List<V20Profile>) -> Unit) {
    var local by remember(profiles) { mutableStateOf(profiles) }
    var newName by remember { mutableStateOf("") }
    val avatars = listOf("😎", "🤓", "😄", "🥳", "🤖", "🧒")
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Profiles", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        items(local) { p ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2033))) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.avatar, fontSize = 40.sp)
                        Spacer(Modifier.width(12.dp))
                        OutlinedTextField(p.name, { name -> local = local.map { if (it.id == p.id) it.copy(name = name.take(24)) else it } }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Kids profile", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(p.kids, { checked -> local = local.map { if (it.id == p.id) it.copy(kids = checked) else it } })
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(avatars) { avatar -> Text(avatar, fontSize = 28.sp, modifier = Modifier.clickable { local = local.map { if (it.id == p.id) it.copy(avatar = avatar) else it } }.padding(4.dp)) }
                    }
                    if (local.size > 1) TextButton(onClick = { local = local.filterNot { it.id == p.id } }) { Text("Remove profile") }
                }
            }
        }
        if (local.size < 5) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(newName, { newName = it }, modifier = Modifier.weight(1f), placeholder = { Text("New profile name") }, singleLine = true)
                Button(onClick = { if (newName.isNotBlank()) { local = local + V20Profile(UUID.randomUUID().toString(), newName.trim(), "😄", false); newName = "" } }) { Text("Add") }
            }
        }
        item { Button(onClick = { onSave(local) }, modifier = Modifier.fillMaxWidth()) { Text("Save profiles") } }
    }
}

@Composable
private fun V20Settings(profile: V20Profile) {
    var autoplay by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("English") }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item { Text("Profile: ${profile.name}", color = Color(0xFF93A9BD)) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Autoplay next episode", color = Color.White, modifier = Modifier.weight(1f)); Switch(autoplay, { autoplay = it }) } }
        item { OutlinedTextField(language, { language = it }, label = { Text("Preferred language") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Privacy", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { Text("No BluStream account registration. No BluStream sign-up wall. Profile data stays local in this alpha build.", color = Color(0xFFBCD2E5)) }
        item { Text("2.0 roadmap", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        item { Text("Multi-device sync, web configurator, catalog reordering, similar titles and broader HTTP extension adapters are planned for later 2.0 builds.", color = Color(0xFFBCD2E5)) }
    }
}

@Composable
private fun V20Details(media: RealMedia, profile: V20Profile, onBack: () -> Unit, onPlay: (String, String) -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sources by remember { mutableStateOf<List<BluStreamSource>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFF020C16)), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(media.background, media.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF020C16)))))
                Text("‹", color = Color.White, fontSize = 42.sp, modifier = Modifier.padding(16.dp).clickable(onClick = onBack))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(media.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(media.releaseInfo, color = Color(0xFF93A9BD))
                if (media.genres.isNotEmpty()) Text(media.genres.joinToString(" • "), color = Color(0xFF159CFF))
                Spacer(Modifier.height(12.dp))
                Text(media.description, color = Color(0xFFD2DEE9))
                Spacer(Modifier.height(16.dp))
                Button(enabled = !loading, onClick = {
                    scope.launch {
                        loading = true
                        runCatching { resolveInstalledAddonStreams(context, profile.id, media.type, media.id) }
                            .onSuccess { sources = it; if (it.isEmpty()) onMessage("No playable sources found in installed add-ons.") }
                            .onFailure { onMessage(it.message ?: "Source search failed") }
                        loading = false
                    }
                }) { Text(if (loading) "Finding…" else "Find sources") }
            }
        }
        items(sources) { source ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clickable {
                when (source.kind) {
                    BluSourceKind.DIRECT -> source.url?.let { onPlay(source.name.ifBlank { media.name }, it) }
                    BluSourceKind.EXTERNAL, BluSourceKind.YOUTUBE -> source.playableTarget?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    else -> onMessage("Select this source from Add-ons for P2P playback.")
                }
            }, colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2033))) {
                Column(Modifier.padding(14.dp)) { Text(source.name.ifBlank { "Stream" }, color = Color.White, fontWeight = FontWeight.Bold); Text(source.description.orEmpty(), color = Color(0xFF93A9BD), maxLines = 2) }
            }
        }
    }
}

@Composable
private fun V20Player(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxSize())
        Text("‹", color = Color.White, fontSize = 42.sp, modifier = Modifier.padding(16.dp).clickable(onClick = onBack))
        Text(title, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(24.dp), fontWeight = FontWeight.Bold)
    }
}
