package com.blustream.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun BluStreamApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var playingTitle by remember { mutableStateOf("BluStream") }
    var message by remember { mutableStateOf<String?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF2F80ED),
            background = Color(0xFF07111F),
            surface = Color(0xFF0E1B2D)
        )
    ) {
        if (playingUrl != null) {
            PlayerScreen(playingTitle, playingUrl!!) { playingUrl = null }
            return@MaterialTheme
        }

        Scaffold(
            containerColor = Color(0xFF07111F),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF0B1626)) {
                    listOf("Home", "Add-ons").forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Text(if (index == 0) "⌂" else "+") },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                if (tab == 0) {
                    HomeScreen(
                        message = message,
                        onDemo = {
                            playingTitle = "Big Buck Bunny"
                            playingUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                        }
                    )
                } else {
                    AddonsScreen(
                        profile = "Macca",
                        onProfile = {},
                        onPlaySource = { source ->
                            when (source.kind) {
                                BluSourceKind.DIRECT -> {
                                    source.url?.let {
                                        playingTitle = source.name
                                        playingUrl = it
                                    }
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

@Composable
fun Header(profile: String, onProfile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("BluStream", color = Color.White, fontSize = 25.sp)
        Text(profile, Modifier.clickable { onProfile() }, color = Color(0xFF8EC5FF))
    }
}

@Composable
private fun HomeScreen(message: String?, onDemo: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Header("Macca") {}
        Spacer(Modifier.height(24.dp))
        Text("BluStream 0.5 Alpha", color = Color.White, fontSize = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text("Android streaming test build with add-ons, direct playback and P2P support.", color = Color(0xFFB8C9DC))
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12345A))) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Playback test", color = Color.White, fontSize = 20.sp)
                Text("Open sample video", color = Color(0xFFC6D5E6))
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDemo) { Text("Play demo") }
            }
        }
        if (message != null) {
            Spacer(Modifier.height(20.dp))
            Text(message, color = Color(0xFF8EC5FF))
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

    Column(Modifier.fillMaxSize()) {
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
