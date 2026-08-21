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

class BluStreamV07Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateManager.check(this, manual = false)
        setContent { BluStreamV07App() }
    }
}

private enum class BluPage(val label: String, val icon: String) {
    HOME("Home", "⌂"), DISCOVER("Discover", "◈"), SEARCH("Search", "⌕"),
    MY_LIST("My List", "♡"), ADDONS("Add-ons", "+"), SETTINGS("Settings", "⚙")
}

private object ProfileStore {
    private const val PREFS = "blustream_profiles"
    private const val KEY = "profiles"

    fun load(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return listOf("Macca", "Guest")
        return runCatching {
            val a = JSONArray(raw)
            buildList { for (i in 0 until a.length()) a.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add) }
                .ifEmpty { listOf("Macca", "Guest") }
        }.getOrDefault(listOf("Macca", "Guest"))
    }

    fun save(context: Context, names: List<String>) {
        val a = JSONArray()
        names.take(5).forEach { a.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluStreamV07App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf(ProfileStore.load(context)) }
    var profile by remember { mutableStateOf(profiles.firstOrNull() ?: "Macca") }
    var showProfiles by remember { mutableStateOf(true) }
    var manageProfiles by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(BluPage.HOME) }
    var selected by remember { mutableStateOf<RealMedia?>(null) }
    var playing by remember { mutableStateOf<Pair<String, String>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val drawer = rememberDrawerState(DrawerValue.Closed)

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF1597FF), background = Color(0xFF020A12), surface = Color(0xFF071624))) {
        when {
            showProfiles -> ProfilePickerV07(
                profiles = profiles,
                onSelect = { profile = it; showProfiles = false },
                onManage = { manageProfiles = true }
            )
            manageProfiles -> ManageProfilesV07(
                initial = profiles,
                onDone = {
                    profiles = it
                    ProfileStore.save(context, it)
                    if (profile !in it) profile = it.firstOrNull() ?: "Macca"
                    manageProfiles = false
                    showProfiles = true
                }
            )
            playing != null -> LocalPlayerV07(playing!!.first, playing!!.second) { playing = null }
            selected != null -> RealDetailV07(selected!!, onBack = { selected = null }, onFindSources = { page = BluPage.ADDONS; selected = null })
            else -> ModalNavigationDrawer(
                drawerState = drawer,
                drawerContent = {
                    ModalDrawerSheet(drawerContainerColor = Color(0xFF06111F), modifier = Modifier.width(290.dp)) {
                        Spacer(Modifier.height(24.dp))
                        Text("BLUSTREAM", Modifier.padding(horizontal = 20.dp), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(profile, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = Color(0xFF75BEFF))
                        HorizontalDivider(color = Color(0xFF17304A))
                        BluPage.entries.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(item.label) },
                                icon = { Text(item.icon, fontSize = 20.sp) },
                                selected = page == item,
                                onClick = { page = item; scope.launch { drawer.close() } },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFF17304A), modifier = Modifier.padding(vertical = 8.dp))
                        NavigationDrawerItem(
                            label = { Text("Switch profile") }, icon = { Text("◉") }, selected = false,
                            onClick = { showProfiles = true; scope.launch { drawer.close() } }, modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        NavigationDrawerItem(
                            label = { Text("Manage profiles") }, icon = { Text("✎") }, selected = false,
                            onClick = { manageProfiles = true; scope.launch { drawer.close() } }, modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            ) {
                Box(Modifier.fillMaxSize().background(Color(0xFF020A12))) {
                    when (page) {
                        BluPage.HOME -> RealHomeV07(profile, onMenu = { scope.launch { drawer.open() } }, onProfile = { showProfiles = true }, onSelect = { selected = it })
                        BluPage.DISCOVER -> RealBrowseV07("Discover", profile, { scope.launch { drawer.open() } }, { selected = it })
                        BluPage.SEARCH -> RealBrowseV07("Search", profile, { scope.launch { drawer.open() } }, { selected = it }, searchMode = true)
                        BluPage.MY_LIST -> SimplePageV07("My List", profile) { scope.launch { drawer.open() } }
                        BluPage.ADDONS -> AddonsScreen(
                            profile = profile,
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
                        BluPage.SETTINGS -> SettingsV07(profile, onMenu = { scope.launch { drawer.open() } }, onProfiles = { manageProfiles = true })
                    }
                    message?.let {
                        Surface(Modifier.align(Alignment.TopCenter).padding(12.dp), color = Color(0xFF173C67), shape = RoundedCornerShape(12.dp)) {
                            Text(it, Modifier.padding(12.dp), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarV07(profile: String, onMenu: () -> Unit, onProfile: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(76.dp).background(Color(0xFF020A12)).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("☰", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { onMenu() })
        Text("BLUSTREAM", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f).padding(start = 26.dp))
        Text("⌕", color = Color.White, fontSize = 28.sp)
        Spacer(Modifier.width(14.dp))
        Surface(Modifier.size(44.dp).clickable { onProfile() }, shape = CircleShape, color = Color(0xFF073D64)) {
            Box(contentAlignment = Alignment.Center) { Text(profile.take(1).uppercase(), color = Color(0xFF22A0FF), fontSize = 20.sp) }
        }
    }
}

@Composable
private fun RealHomeV07(profile: String, onMenu: () -> Unit, onProfile: () -> Unit, onSelect: (RealMedia) -> Unit) {
    var movies by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var series by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    LaunchedEffect(Unit) { movies = RealCatalog.topMovies(); series = RealCatalog.topSeries() }
    val hero = movies.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu, onProfile)
        if (hero != null) {
            Box(Modifier.fillMaxWidth().height(370.dp).clickable { onSelect(hero) }) {
                AsyncImage(model = hero.background.ifBlank { hero.poster }, contentDescription = hero.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x22020A12), Color(0xFF020A12)))))
                Column(Modifier.align(Alignment.BottomStart).padding(24.dp).fillMaxWidth(.78f)) {
                    Text("TRENDING", color = Color(0xFF1597FF), fontWeight = FontWeight.Bold)
                    Text(hero.name, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSelect(hero) }, shape = RoundedCornerShape(8.dp)) { Text("▶  View title") }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(250.dp).background(Color(0xFF081A2A)), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        MediaRailV07("Popular Movies", movies, onSelect)
        MediaRailV07("Popular Series", series, onSelect)
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
private fun RealBrowseV07(title: String, profile: String, onMenu: () -> Unit, onSelect: (RealMedia) -> Unit, searchMode: Boolean = false) {
    var movies by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var series by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { movies = RealCatalog.topMovies(); series = RealCatalog.topSeries() }
    val all = movies + series
    val shown = if (searchMode && query.isNotBlank()) all.filter { it.name.contains(query, true) } else all
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBarV07(profile, onMenu)
        Text(title, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        if (searchMode) OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Movies and series") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp))
        shown.chunked(3).forEach { row ->
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
}

@Composable
private fun RealDetailV07(media: RealMedia, onBack: () -> Unit, onFindSources: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFF020A12))) {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            AsyncImage(model = media.background.ifBlank { media.poster }, contentDescription = media.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF020A12)))))
            Text("‹", color = Color.White, fontSize = 48.sp, modifier = Modifier.padding(18.dp).clickable { onBack() })
        }
        Column(Modifier.padding(20.dp)) {
            Text(media.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(media.releaseInfo, color = Color(0xFF91A9BE))
            Spacer(Modifier.height(12.dp))
            if (media.description.isNotBlank()) Text(media.description, color = Color(0xFFD5DFE8), lineHeight = 21.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onFindSources) { Text("Find sources in Add-ons") }
        }
    }
}

@Composable
private fun ProfilePickerV07(profiles: List<String>, onSelect: (String) -> Unit, onManage: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF020A12), Color(0xFF08223A)))).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("BLUSTREAM", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Who's watching?", color = Color.White, fontSize = 24.sp)
        Spacer(Modifier.height(26.dp))
        profiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(vertical = 9.dp)) {
                row.forEach { name ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(105.dp).clickable { onSelect(name) }) {
                        Surface(Modifier.size(78.dp), shape = RoundedCornerShape(14.dp), color = Color(0xFF0B5E98)) { Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = Color.White, fontSize = 34.sp) } }
                        Spacer(Modifier.height(8.dp)); Text(name, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onManage) { Text("Manage Profiles") }
    }
}

@Composable
private fun ManageProfilesV07(initial: List<String>, onDone: (List<String>) -> Unit) {
    var names by remember { mutableStateOf(initial.ifEmpty { listOf("Macca") }) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFF020A12)).padding(22.dp)) {
        Text("Manage Profiles", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Edit names or add up to 5 profiles.", color = Color(0xFF9CB2C7))
        Spacer(Modifier.height(18.dp))
        names.forEachIndexed { index, value ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = value, onValueChange = { new -> names = names.toMutableList().also { it[index] = new.take(24) } }, label = { Text("Profile ${index + 1}") }, modifier = Modifier.weight(1f))
                if (names.size > 1) TextButton(onClick = { names = names.toMutableList().also { it.removeAt(index) } }) { Text("Remove") }
            }
        }
        if (names.size < 5) Button(onClick = { names = names + "New Profile" }) { Text("+ Add Profile") }
        Spacer(Modifier.height(22.dp))
        Button(onClick = { onDone(names.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Macca") }) }) { Text("Done") }
    }
}

@Composable
private fun SettingsV07(profile: String, onMenu: () -> Unit, onProfiles: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color(0xFF020A12))) {
        TopBarV07(profile, onMenu)
        Column(Modifier.padding(20.dp)) {
            Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onProfiles) { Text("Manage profiles") }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { UpdateManager.check(context as ComponentActivity, manual = true) }) { Text("Check for updates") }
        }
    }
}

@Composable
private fun SimplePageV07(title: String, profile: String, onMenu: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF020A12))) { TopBarV07(profile, onMenu); Text(title, Modifier.padding(20.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
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
