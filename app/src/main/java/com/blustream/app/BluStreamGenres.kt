package com.blustream.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun BluStreamGenres(kids: Boolean, onSelect: (RealMedia) -> Unit) {
    val scope = rememberCoroutineScope()
    val allGenres = if (kids) {
        listOf("Animation", "Family", "Adventure", "Comedy", "Fantasy")
    } else {
        listOf("Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary", "Drama", "Family", "Fantasy", "History", "Horror", "Music", "Mystery", "Romance", "Science Fiction", "Thriller", "War", "Western")
    }
    var selectedGenre by remember { mutableStateOf(allGenres.first()) }
    var selectedType by remember { mutableStateOf("movie") }
    var results by remember { mutableStateOf<List<RealMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            results = runCatching { RealCatalog.genre(selectedType, selectedGenre) }
                .getOrDefault(emptyList())
                .distinctBy { it.id }
                .take(90)
            loading = false
        }
    }

    LaunchedEffect(selectedGenre, selectedType, kids) { load() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Genres", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedType == "movie", onClick = { selectedType = "movie" }, label = { Text("Movies") })
                FilterChip(selected = selectedType == "series", onClick = { selectedType = "series" }, label = { Text("TV Shows") })
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allGenres) { genre ->
                    FilterChip(selected = selectedGenre == genre, onClick = { selectedGenre = genre }, label = { Text(genre) })
                }
            }
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else if (results.isEmpty()) {
            item { Text("No titles found in $selectedGenre.", color = Color(0xFF93A9BD)) }
        } else {
            items(results.chunked(3)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { media ->
                        Card(
                            onClick = { onSelect(media) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                coil.compose.AsyncImage(
                                    model = media.poster,
                                    contentDescription = media.name,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(media.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text(media.releaseInfo, color = Color(0xFF93A9BD), fontSize = 12.sp)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
