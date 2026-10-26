@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.normalizeRows
import com.tanix.launcher.ui.*

/**
 * Вкладка «Для вас»: вверху — крупный блок «Продолжить просмотр» (название и описание выбранной карточки,
 * размытый фон из её кадра), ниже ряды: «Продолжить просмотр», «Ваши приложения» и др.
 */
@Composable
fun ForYouPage(
    vm: HomeViewModel,
    home: List<AppItem>,
    visible: List<AppItem>,
    watch: List<AppItem>,
    moving: String?,
    onMenu: (AppItem) -> Unit,
    onDone: () -> Unit,
    onBg: (AppItem?) -> Unit,
    onLaunch: (AppItem, Rect) -> Unit,
    onAddApps: () -> Unit,
    onPickWatch: () -> Unit,
    onChannels: () -> Unit,
    onReorder: () -> Unit
) {
    val s = vm.s
    var idx by remember { mutableIntStateOf(0) }
    val cur = watch.getOrNull(idx.coerceIn(0, maxOf(0, watch.lastIndex)))
    LaunchedEffect(watch.map { it.pkg }) { idx = 0; onBg(watch.firstOrNull()) }

    val keys = normalizeRows(s.rowOrder).filter {
        when (it) { "watch" -> s.showWatch; "apps" -> s.showAppsRow; "fav" -> s.showFavRow; else -> s.showRecentRow }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (s.showWatch) HeroInfo(cur, cur?.let { s.opened[it.pkg] }, idx, watch.size, Modifier.fillMaxWidth().height(160.dp))
        keys.forEachIndexed { i, key ->
            EnterAnim(i) {
                when (key) {
                    "watch" -> WatchSection(vm, watch, onLaunch, onMenu, onPickWatch) { n -> idx = n; onBg(watch.getOrNull(n)) }
                    "apps" -> HomeAppsSection(home, vm, moving, onMenu, onDone, onAddApps, onLaunch)
                    "fav" -> AppsSection(tr("Избранное", "Favorites"), visible.filter { it.pkg in s.favorites }, vm, moving, onMenu, onDone, onLaunch)
                    else -> AppsSection(tr("Недавние", "Recent"), s.recents.mapNotNull { p -> visible.firstOrNull { it.pkg == p } }, vm, null, onMenu, onDone, onLaunch)
                }
            }
        }
        // «Настроить каналы», как в Android TV Home
        Row(Modifier.padding(start = 8.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Chip(tr("Настроить каналы", "Customize channels"), onClick = onChannels)
            Chip(tr("Порядок приложений", "App order"), onClick = onReorder)
        }
        Spacer(Modifier.height(40.dp))
    }
}

/** Верхний блок: метка, крупное название, описание и точки-индикатор (как у фильма в Google TV). */
@Composable
private fun HeroInfo(item: AppItem?, opened: Long?, index: Int, total: Int, modifier: Modifier) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val en = st.lang == 1
    Box(modifier) {
        Crossfade(targetState = item, animationSpec = tween(animMs(350)), label = "heroInfo") { cur ->
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.62f).padding(start = 8.dp, bottom = 8.dp)) {
                    if (st.posterBadge) {
                        Text(tr("Продолжить просмотр", "Continue watching"), color = cs.primary, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (cur != null) {
                        Text(cur.label, color = cs.onSurface, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (st.posterDesc) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (opened != null) tr("Последний запуск: ", "Last opened: ") + agoText(opened, en)
                                else tr("Нажмите, чтобы открыть", "Press OK to open"),
                                color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(tr("Продолжить просмотр", "Continue watching"), color = cs.onSurface, style = MaterialTheme.typography.headlineLarge, maxLines = 2)
                        if (st.posterDesc) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tr("Выберите приложения (Lampa, YouTube, кино…), которые будут здесь", "Choose the apps (Lampa, YouTube, movies…) to show here"),
                                color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        if (total > 1) {
            Row(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(minOf(total, 12)) { i ->
                    Box(
                        Modifier.size(if (i == index) 8.dp else 6.dp).clip(CircleShape)
                            .background(if (i == index) cs.onSurface else cs.onSurface.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchSection(
    vm: HomeViewModel, watch: List<AppItem>, onLaunch: (AppItem, Rect) -> Unit,
    onMenu: (AppItem) -> Unit, onPick: () -> Unit, onFocusIdx: (Int) -> Unit
) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Продолжить просмотр", "Continue watching"), color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
        itemsIndexed(watch, key = { _, a -> a.pkg }) { i, app ->
            WatchCard(
                app, s.opened[app.pkg], onFocused = { onFocusIdx(i) },
                onClick = { r -> onLaunch(app, r) }, onLongClick = { onMenu(app) },
                modifier = Modifier.animateItemPlacement(tween(animMs(300)))
            )
        }
        item { WatchAddCard(onPick) }
    }
}

/** «Ваши приложения»: выбранные вами приложения (около 15) + плитка «+». */
@Composable
private fun HomeAppsSection(
    list: List<AppItem>, vm: HomeViewModel, moving: String?,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit, onAdd: () -> Unit, onLaunch: (AppItem, Rect) -> Unit
) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    Text(tr("Ваши приложения", "Your apps"), color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
        items(list, key = { it.pkg }) { app ->
            val isMoving = moving == app.pkg
            AppTile(
                app, app.pkg in s.favorites, isMoving,
                onClick = { r -> if (isMoving) onDone() else onLaunch(app, r) },
                onLongClick = { if (!isMoving) onMenu(app) },
                onKey = if (isMoving) moveKeyHandler(1) { d -> vm.moveHome(app.pkg, d) } else null,
                modifier = Modifier.animateItemPlacement(tween(animMs(300)))
            )
        }
        item { AddTile(onAdd) }
    }
}

@Composable
private fun AppsSection(
    title: String, list: List<AppItem>, vm: HomeViewModel, moving: String?,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit, onLaunch: (AppItem, Rect) -> Unit
) {
    if (list.isEmpty()) return
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val subset = list.map { it.pkg }
    Text(title, color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)) {
        items(list, key = { it.pkg }) { app ->
            val isMoving = moving == app.pkg
            AppTile(
                app, app.pkg in s.favorites, isMoving,
                onClick = { r -> if (isMoving) onDone() else onLaunch(app, r) },
                onLongClick = { if (!isMoving) onMenu(app) },
                onKey = if (isMoving) moveKeyHandler(1) { d -> vm.moveApp(app.pkg, d, subset) } else null,
                modifier = Modifier.animateItemPlacement(tween(animMs(300)))
            )
        }
    }
}

/** Вкладка «Приложения»: сетка, в которой плитки можно переставлять. */
@Composable
fun AppsGridPage(
    vm: HomeViewModel, list: List<AppItem>, emptyText: String, moving: String?,
    onMenu: (AppItem) -> Unit, onDone: () -> Unit, onLaunch: (AppItem, Rect) -> Unit
) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val tileW = (if (s.tileStyle == 0) Look.at(Look.TileW, s.iconSize) else Look.at(Look.IconSizes, s.iconSize)).dp
    val subset = list.map { it.pkg }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cols = maxOf(1, (maxWidth / (tileW + 24.dp)).toInt())
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(list, key = { it.pkg }) { app ->
                val isMoving = moving == app.pkg
                Box(Modifier.animateItemPlacement(tween(animMs(300))), contentAlignment = Alignment.Center) {
                    AppTile(
                        app, app.pkg in s.favorites, isMoving,
                        onClick = { r -> if (isMoving) onDone() else onLaunch(app, r) },
                        onLongClick = { if (!isMoving) onMenu(app) },
                        onKey = if (isMoving) moveKeyHandler(cols) { d -> vm.moveApp(app.pkg, d, subset) } else null
                    )
                }
            }
        }
    }
}

/** Вкладка «Магазин приложений»: библиотека Google Play (открывается магазин на приставке). */
@Composable
fun StorePage(vm: HomeViewModel) {
    val cs = MaterialTheme.colorScheme
    val cats = listOf(
        tr("Приложения", "Apps") to "apps",
        tr("Игры", "Games") to "games",
        tr("Видео и ТВ", "Video & TV") to "video player tv",
        tr("Музыка", "Music") to "music",
        tr("Инструменты", "Tools") to "file manager",
        tr("Браузеры", "Browsers") to "browser tv"
    )
    val picks = listOf(
        "YouTube" to "com.google.android.youtube.tv", "Netflix" to "com.netflix.ninja",
        "VLC" to "org.videolan.vlc", "Kodi" to "org.xbmc.kodi", "Spotify" to "com.spotify.tv.android",
        "Twitch" to "tv.twitch.android.app", "Plex" to "com.plexapp.android", "Prime Video" to "com.amazon.amazonvideo.livingroom"
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 12.dp)) {
        Text(tr("Магазин приложений", "App store"), color = cs.onSurface, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            tr("Библиотека приложений Google Play — откроется магазин на вашей приставке.", "Google Play app library — the store opens on your device."),
            color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(18.dp))
        FocusBox(Modifier, cardShape(), 1.04f, onClick = { vm.openStore() }, ringColor = tileRing()) {
            Row(
                Modifier.width(360.dp).background(
                    Brush.horizontalGradient(listOf(Color(0xFF1B5E20), Color(0xFF0D47A1)))
                ).padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Google Play", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(tr("Открыть магазин", "Open the store"), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                }
                Text("▶", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(tr("Категории", "Categories"), color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cats) { (name, q) -> Chip(name) { vm.openStore(query = q) } }
        }
        Spacer(Modifier.height(22.dp))
        Text(tr("Популярное для ТВ", "Popular on TV"), color = cs.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(picks) { (name, pkg) -> Chip(name) { vm.openStore(details = pkg) } }
        }
        Spacer(Modifier.height(30.dp))
    }
}
