package com.blustream.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private data class PosterItem(
    val media: MediaCard,
    val image: String,
    val progress: Float? = null
)

private val cinematicContinue = listOf(
    PosterItem(MediaCard("Harbour City", "42 min left", Color(0xFF385D73)), "https://picsum.photos/id/1011/600/900", 0.62f),
    PosterItem(MediaCard("Red Line", "Episode 4", Color(0xFF713B3B)), "https://picsum.photos/id/1005/600/900", 0.34f),
    PosterItem(MediaCard("Glass House", "18 min left", Color(0xFF4E476F)), "https://picsum.photos/id/1027/600/900", 0.78f)
)

private val cinematicTrending = listOf(
    PosterItem(MediaCard("Night Signal", "Movie • 2026", Color(0xFF294B7A)), "https://picsum.photos/id/1015/600/900"),
    PosterItem(MediaCard("Northbound", "Series • S1", Color(0xFF405A3B)), "https://picsum.photos/id/1040/600/900"),
    PosterItem(MediaCard("After Dark", "Movie • 2025", Color(0xFF6B3443)), "https://picsum.photos/id/1039/600/900"),
    PosterItem(MediaCard("The Crossing", "Series • S2", Color(0xFF57456D)), "https://picsum.photos/id/1025/600/900"),
    PosterItem(MediaCard("Orbit", "Movie • 2026", Color(0xFF314E5C)), "https://picsum.photos/id/1016/600/900")
)

private val cinematicHero = MediaCard("Night Signal", "Movie • 2026", Color(0xFF294B7A))

@Composable
fun CinematicHomeScreen(
    profile: String,
    onProfile: () -> Unit,
    onSelect: (MediaCard) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF020A12))
            .verticalScroll(rememberScrollState())
    ) {
        CinematicTopBar(profile, onProfile)
        CinematicHero(onSelect)
        Spacer(Modifier.height(18.dp))
        PosterRail("Continue Watching", cinematicContinue, onSelect, showSeeAll = true)
        Spacer(Modifier.height(20.dp))
        PosterRail("Trending Now", cinematicTrending, onSelect, showSeeAll = true)
        Spacer(Modifier.height(22.dp))
        PosterRail("Popular Movies", cinematicTrending.reversed(), onSelect, showSeeAll = true)
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CinematicTopBar(profile: String, onProfile: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("☰", color = Color.White, fontSize = 27.sp)
        Text(
            "BLUSTREAM",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⌕", color = Color.White, fontSize = 27.sp)
            Spacer(Modifier.width(12.dp))
            Surface(
                modifier = Modifier.clickable { onProfile() },
                color = Color(0xFF0D2B47),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(profile.take(1), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFF39A0FF))
            }
        }
    }
}

@Composable
private fun CinematicHero(onSelect: (MediaCard) -> Unit) {
    val hero = cinematicHero
    Box(
        Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clickable { onSelect(hero) }
    ) {
        AsyncImage(
            model = "https://picsum.photos/id/1018/1200/700",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color(0x33020A12),
                        1f to Color(0xFF020A12)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 22.dp)
                .fillMaxWidth(0.75f)
        ) {
            Text("TRENDING", color = Color(0xFF199BFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(hero.title.uppercase(), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onSelect(hero) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128DFF)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text("▶  Watch Now", color = Color.White, fontSize = 16.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(5) { index ->
                    Surface(
                        modifier = Modifier.size(if (index == 0) 10.dp else 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (index == 0) Color(0xFF128DFF) else Color(0xFF4A5664)
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun PosterRail(
    title: String,
    items: List<PosterItem>,
    onSelect: (MediaCard) -> Unit,
    showSeeAll: Boolean
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        if (showSeeAll) Text("See All", color = Color(0xFF1394FF), fontSize = 14.sp)
    }
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item -> PosterCard(item, onSelect) }
    }
}

@Composable
private fun PosterCard(item: PosterItem, onSelect: (MediaCard) -> Unit) {
    Card(
        modifier = Modifier.width(145.dp).clickable { onSelect(item.media) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071624)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            Box(Modifier.height(185.dp).fillMaxWidth()) {
                AsyncImage(
                    model = item.image,
                    contentDescription = item.media.title,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.Center).size(38.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0x88000000)
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 16.sp) }
                }
            }
            Column(Modifier.padding(9.dp)) {
                Text(item.media.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(item.media.subtitle, color = Color(0xFFA8B6C4), fontSize = 11.sp, maxLines = 1)
                item.progress?.let { progress ->
                    Spacer(Modifier.height(7.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = Color(0xFF1394FF),
                        trackColor = Color(0xFF162A3C)
                    )
                }
            }
        }
    }
}
