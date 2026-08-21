package com.blustream.app

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BluStreamApp() }
    }
}

data class MediaCard(val title: String, val subtitle: String, val accent: Color)

private val trending = listOf(
    MediaCard("Night Signal", "Movie • 2026", Color(0xFF294B7A)),
    MediaCard("Northbound", "Series • S1", Color(0xFF405A3B)),
    MediaCard("After Dark", "Movie • 2025", Color(0xFF6B3443)),
    MediaCard("The Crossing", "Series • S2", Color(0xFF57456D)),
    MediaCard("Orbit", "Movie • 2026", Color(0xFF314E5C))
)

private val continueWatching = listOf(
    MediaCard("Harbour City", "42 min left", Color(0xFF385D73)),
    MediaCard("Red Line", "Episode 4", Color(0xFF713B3B)),
    MediaCard("Glass House", "18 min left", Color(0xFF4E476F))
)

private val discovery = listOf(
    MediaCard("Drama", "Browse titles", Color(0xFF473E78)),
    MediaCard("Comedy", "Browse titles", Color(0xFF7B5D2C)),
    MediaCard("Sci-Fi", "Browse titles", Color(0xFF2F586D)),
    MediaCard("Crime", "Browse titles", Color(0xFF633840)),
    MediaCard("Documentary", "Browse titles", Color(0xFF3C634B))
)

@Composable
fun BluStreamApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var playingTitle by remember { mutableStateOf("BluStream") }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedTitle by remember { mutableStateOf<MediaCard?>(null) }
    var showProfilePicker by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf("Macca") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF3493FF),
            secondary = Color(0xFF8EC5FF),
            background = Color(0xFF06111F),
            surface = Color(0xFF0C192A)
        )
    ) {
        when {
            showProfilePicker -> ProfilePicker(
                onSelect = { profile = it; showProfilePicker = false }
            )
            playingUrl != null -> PlayerScreen(playingTitle, playingUrl!!) { playingUrl = null }
            selectedTitle != null -> DetailScreen(
                media = selectedTitle!!,
                onBack = { selectedTitle = null },
                onPlayDemo = {
                    playingTitle = selectedTitle!!.title
                    playingUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                }
            )
            else -> {
                Scaffold(
                    containerColor = Color(0xFF06111F),
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF091524)) {
                            val items = listOf(
                                "⌂" to "Home",
                                "◈" to "Discover",
                                "⌕" to "Search",
                                "♡" to "My List",
                                "+" to "Add-ons",
                                "⚙" to "Settings"
                            )
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    icon = { Text(item.first, fontSize = 18.sp) },
                                    label = { Text(item.second, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        when (tab) {
                            0 -> HomeScreen(profile, onProfile = { showProfilePicker = true }, onSelect = { selectedTitle = it })
                            1 -> DiscoverScreen(profile, onProfile = { showProfilePicker = true }, onSelect = { selectedTitle = it })
                            2 -> SearchScreen(profile, onProfile = { showProfilePicker = true }, onSelect = { selectedTitle = it })
                            3 -> MyListScreen(profile, onProfile = { showProfilePicker = true }, onSelect = { selectedTitle = it })
                            4 -> AddonsScreen(
                                profile = profile,
                                onProfile = { showProfilePicker = true },
                                onPlaySource = { source ->
                                    when (source.kind) {
                                        BluSourceKind.DIRECT -> source.url?.let {
                                            playingTitle = source.name
                                            playingUrl = it
                                        }
                                        BluSourceKind.TORRENT -> {
                                            message = "Connecting to P2P peers…"
                                            scope.launch {
                                                runCatching { P2pEngine.prepare(context.applicationContext, source) }
                                                    .onSuccess {
                                                        message = null
                                                        playingTitle = source.name.ifBlank { it.title }
                                                        playingUrl = it.url
                                                    }
                                                    .onFailure { message = it.message ?: "P2P playback failed" }
                                            }
                                        }
                                        BluSourceKind.EXTERNAL, BluSourceKind.YOUTUBE -> {
                                            source.playableTarget?.let { target ->
                                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
                                                    .onFailure { message = "No app is available to open this source." }
                                            }
                                        }
                                        else -> message = "This source type is listed but has no player yet."
                                    }
                                }
                            )
                            5 -> SettingsScreen(profile, onProfile = { showProfilePicker = true })
                        }

                        message?.let {
                            Surface(
                                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                                color = Color(0xFF173C67),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(it, Modifier.padding(12.dp), color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePicker(onSelect: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF06111F), Color(0xFF0A213C)))
        ).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("BluStream", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Who's watching?", color = Color(0xFFBFD8F2), fontSize = 20.sp)
        Spacer(Modifier.height(30.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            listOf("Macca", "Guest").forEach { name ->
                Card(
                    Modifier.width(140.dp).clickable { onSelect(name) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102744)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(70.dp),
                            shape = RoundedCornerShape(35.dp),
                            color = Color(0xFF277BDE)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(name.take(1), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(name, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun Header(profile: String, onProfile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("BluStream", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.clickable { onProfile() },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF102744)
        ) {
            Text(profile, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFF8EC5FF))
        }
    }
}

@Composable
private fun HomeScreen(profile: String, onProfile: () -> Unit, onSelect: (MediaCard) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Header(profile, onProfile)
        Spacer(Modifier.height(8.dp))
        HeroBanner(onSelect)
        Spacer(Modifier.height(24.dp))
        MediaRail("Continue Watching", continueWatching, onSelect)
        Spacer(Modifier.height(22.dp))
        MediaRail("Trending Now", trending, onSelect)
        Spacer(Modifier.height(22.dp))
        MediaRail("Popular on BluStream", trending.reversed(), onSelect)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroBanner(onSelect: (MediaCard) -> Unit) {
    val hero = trending.first()
    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp).clickable { onSelect(hero) },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF17446F), Color(0xFF07111F)))
            ).padding(24.dp)
        ) {
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.82f)) {
                Text("FEATURED", color = Color(0xFF8EC5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(hero.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text("A mysterious transmission pulls a coastal town into a dangerous investigation.", color = Color(0xFFD0DEEC), fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onSelect(hero) }) { Text("▶ Play") }
                    OutlinedButton(onClick = { onSelect(hero) }) { Text("More info") }
                }
            }
        }
    }
}

@Composable
private fun MediaRail(title: String, items: List<MediaCard>, onSelect: (MediaCard) -> Unit) {
    Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { MediaTile(it, onSelect) }
    }
}

@Composable
private fun MediaTile(item: MediaCard, onSelect: (MediaCard) -> Unit) {
    Card(
        modifier = Modifier.width(155.dp).height(205.dp).clickable { onSelect(item) },
        colors = CardDefaults.cardColors(containerColor = item.accent),
        shape = RoundedCornerShape(15.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(14.dp)) {
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(item.subtitle, color = Color(0xFFD9E6F2), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DiscoverScreen(profile: String, onProfile: () -> Unit, onSelect: (MediaCard) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Header(profile, onProfile)
        Text("Discover", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        discovery.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Card(
                        modifier = Modifier.weight(1f).height(110.dp).clickable { onSelect(item) },
                        colors = CardDefaults.cardColors(containerColor = item.accent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                            Text(item.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(item.subtitle, color = Color(0xFFD8E5EF), fontSize = 12.sp)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(12.dp))
        MediaRail("Top picks", trending, onSelect)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SearchScreen(profile: String, onProfile: () -> Unit, onSelect: (MediaCard) -> Unit) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val results = if (query.text.isBlank()) trending else trending.filter { it.title.contains(query.text, true) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Header(profile, onProfile)
        Text("Search", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Movies, series, people") },
            singleLine = true
        )
        Spacer(Modifier.height(20.dp))
        Text(if (query.text.isBlank()) "Popular searches" else "Results", color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))
        results.forEach { item ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onSelect(item) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2138)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(62.dp), color = item.accent, shape = RoundedCornerShape(10.dp)) {}
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, color = Color(0xFFB7C9DC), fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MyListScreen(profile: String, onProfile: () -> Unit, onSelect: (MediaCard) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Header(profile, onProfile)
        Text("My List", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Watchlist and saved titles", color = Color(0xFFB7C9DC))
        Spacer(Modifier.height(18.dp))
        MediaRail("Watchlist", listOf(trending[1], trending[3], trending[4]), onSelect)
        Spacer(Modifier.height(22.dp))
        MediaRail("Watch again", continueWatching, onSelect)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsScreen(profile: String, onProfile: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Header(profile, onProfile)
        Text("Settings", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        SettingsCard("Playback", "Autoplay next episode • Quality • Player")
        SettingsCard("Language", "Audio and subtitle preferences")
        SettingsCard("Profiles", "Manage profiles and viewing history")
        SettingsCard("Add-ons", "Installed sources and catalogues")
        SettingsCard("P2P", "Peer playback settings")
        SettingsCard("About", "BluStream 0.6 Alpha")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2138)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFFB7C9DC), fontSize = 12.sp)
            }
            Text("›", color = Color(0xFF8EC5FF), fontSize = 24.sp)
        }
    }
}

@Composable
private fun DetailScreen(media: MediaCard, onBack: () -> Unit, onPlayDemo: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).background(Color(0xFF06111F))) {
        Box(
            Modifier.fillMaxWidth().height(300.dp).background(
                Brush.verticalGradient(listOf(media.accent, Color(0xFF06111F)))
            ).padding(20.dp)
        ) {
            Text("‹ Back", Modifier.clickable { onBack() }.align(Alignment.TopStart), color = Color.White, fontSize = 18.sp)
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(media.title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text(media.subtitle, color = Color(0xFFD6E3EF))
            }
        }
        Column(Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onPlayDemo) { Text("▶ Play") }
                OutlinedButton(onClick = {}) { Text("+ My List") }
            }
            Spacer(Modifier.height(18.dp))
            Text("A BluStream title page with synopsis, source selection, episodes and related titles.", color = Color(0xFFC4D5E5), fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))
            Text("Available sources", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            SettingsCard("Direct stream", "Ready for compatible HTTP/HLS sources")
            SettingsCard("Add-on sources", "Open installed add-ons to choose a stream")
            SettingsCard("P2P", "Peer source support")
            Spacer(Modifier.height(16.dp))
            MediaRail("More like this", trending.filter { it.title != media.title }, { })
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlayerScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Back", Modifier.clickable { onBack() }, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.width(16.dp))
            Text(title, color = Color.White, fontSize = 20.sp)
        }
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
