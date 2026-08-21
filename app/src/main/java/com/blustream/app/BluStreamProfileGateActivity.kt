package com.blustream.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class BluStreamProfileGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProfileGate() }
    }

    private fun openMain(profileId: String) {
        getSharedPreferences("blustream_v20_profiles", Context.MODE_PRIVATE)
            .edit()
            .putString("last_profile", profileId)
            .apply()
        startActivity(Intent(this, BluStreamV20Activity::class.java))
        finish()
    }

    @Composable
    private fun ProfileGate() {
        val profiles = remember { loadProfiles() }
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF159CFF), background = Color(0xFF020C16))) {
            Column(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF020912), Color(0xFF06213A)))
                ).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BluStreamBrand()
                Spacer(Modifier.height(28.dp))
                Text("Who's watching?", color = Color(0xFFBCD2E5), fontSize = 26.sp)
                Spacer(Modifier.height(30.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    items(profiles) { profile ->
                        Column(
                            Modifier.width(120.dp).clickable { openMain(profile.id) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                Modifier.size(96.dp),
                                shape = CircleShape,
                                color = if (profile.kids) Color(0xFF04C98B) else Color(0xFF13B5EA)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(profile.name.take(1).uppercase(), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(profile.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            if (profile.kids) Text("Kids", color = Color(0xFF68E5B7), fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
                OutlinedButton(onClick = {
                    startActivity(Intent(this@BluStreamProfileGateActivity, BluStreamV20Activity::class.java))
                    finish()
                }) { Text("Manage Profiles") }
            }
        }
    }

    @Composable
    private fun BluStreamBrand() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("BLU", color = Color(0xFF078CFF), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("STREAM", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(5.dp))
            Text("STREAM ANYTHING", color = Color(0xFF159CFF), fontSize = 13.sp, letterSpacing = 5.sp)
        }
    }

    private fun loadProfiles(): List<GateProfile> {
        val prefs = getSharedPreferences("blustream_v20_profiles", Context.MODE_PRIVATE)
        val raw = prefs.getString("profiles", null)
        if (raw.isNullOrBlank()) return listOf(GateProfile("macca", "Macca", false))
        return raw.split(";;").mapNotNull { row ->
            val p = row.split("|")
            if (p.size < 4) null else GateProfile(p[0], p[1].ifBlank { "Profile" }, p[3] == "1")
        }.ifEmpty { listOf(GateProfile("macca", "Macca", false)) }
    }
}

private data class GateProfile(val id: String, val name: String, val kids: Boolean)
