package com.tanix.launcher.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.ui.FocusBox
import com.tanix.launcher.ui.LocalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class IptvChannel(val name: String, val url: String, val logo: String?, val group: String)

@OptIn(UnstableApi::class)
@Composable
fun TvScreen(vm: HomeViewModel) {
    val context = LocalContext.current
    val s = LocalSettings.current
    
    var channels by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var groups by remember { mutableStateOf<List<String>>(listOf("Все")) }
    var selectedGroup by remember { mutableStateOf("Все") }
    var isLoading by remember { mutableStateOf(true) }

    var currentChannel by remember { 
        mutableStateOf<IptvChannel?>(if (s.lastTvUrl.isNotEmpty()) IptvChannel(s.lastTvName, s.lastTvUrl, s.lastTvLogo, s.lastTvGroup) else null) 
    }

    LaunchedEffect(Unit) {
        channels = fetchPlaylist("http://cdntv.online/hls/6rkzpedbum/playlist.m3u8")
        groups = listOf("Все") + channels.map { it.group }.distinct().sorted()
        isLoading = false
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    
    LaunchedEffect(currentChannel) {
        currentChannel?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(it.url))
            exoPlayer.prepare()
            vm.update { settings -> 
                settings.copy(lastTvName = it.name, lastTvUrl = it.url, lastTvLogo = it.logo ?: "", lastTvGroup = it.group) 
            }
        }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    val scaleFactor = s.tvScale / 100f

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (currentChannel != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                modifier = Modifier.fillMaxSize().scale(scaleFactor)
            )
            
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(24.dp).scale(scaleFactor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentChannel!!.logo,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(if (s.tvWhiteLogoBg) Color.White else Color.Transparent)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(currentChannel!!.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("Сейчас в эфире", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (isLoading || !exoPlayer.isPlaying) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка плейлиста...", color = Color.White)
                }
            } else {
                Row(Modifier.fillMaxSize().scale(scaleFactor)) {
                    Column(Modifier.width(220.dp).fillMaxHeight().background(Color(0xFF1A1A1A)).padding(8.dp)) {
                        Text("Категории", color = Color.White, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                        LazyColumn {
                            items(groups) { group ->
                                FocusBox(Modifier.fillMaxWidth().padding(vertical = 4.dp), RoundedCornerShape(8.dp), onClick = { selectedGroup = group }) { f ->
                                    Text(group, color = if (f) Color.Black else Color.White,
                                        modifier = Modifier.background(if (f) Color(0xFF00E5FF) else Color.Transparent).padding(12.dp).fillMaxWidth())
                                }
                            }
                        }
                    }
                    
                    LazyColumn(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF0A0A0A)).padding(16.dp)) {
                        val filtered = if (selectedGroup == "Все") channels else channels.filter { it.group == selectedGroup }
                        items(filtered) { channel ->
                            FocusBox(Modifier.fillMaxWidth().padding(vertical = 4.dp), RoundedCornerShape(12.dp), onClick = { 
                                currentChannel = channel; exoPlayer.play() 
                            }) { f ->
                                Row(Modifier.fillMaxWidth().background(if (f) Color(0xFF00E5FF) else Color(0xFF222222)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = channel.logo, contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(if (s.tvWhiteLogoBg) Color.White else Color.Transparent)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(channel.name, color = if (f) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                        Text("Программа передач", color = if (f) Color.DarkGray else Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun fetchPlaylist(url: String): List<IptvChannel> = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
    try {
        val body = client.newCall(Request.Builder().url(url).build()).execute().body?.string() ?: return@withContext emptyList()
        val channels = mutableListOf<IptvChannel>()
        var currentName = ""; var currentLogo: String? = null; var currentGroup = "Без группы"
        body.lines().forEach { line ->
            if (line.startsWith("#EXTINF:")) {
                currentName = Regex(",(.*)$").find(line)?.groupValues?.get(1)?.trim() ?: "Unknown"
                currentLogo = Regex("tvg-logo=\"(.*?)\"").find(line)?.groupValues?.get(1)
                currentGroup = Regex("group-title=\"(.*?)\"").find(line)?.groupValues?.get(1) ?: "Без группы"
            } else if (line.startsWith("http")) {
                channels.add(IptvChannel(currentName, line.trim(), currentLogo, currentGroup))
            }
        }
        channels
    } catch (e: Exception) { emptyList() }
}
