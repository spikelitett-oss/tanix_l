@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.LaunchAnim
import com.tanix.launcher.R
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: HomeViewModel, openSettings: () -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showHomeApps by remember { mutableStateOf(false) }
    var showWatchApps by remember { mutableStateOf(false) }
    var showChannels by remember { mutableStateOf(false) }
    var showReorder by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppItem?>(null) }
    var moving by remember { mutableStateOf<String?>(null) }
    var bgApp by remember { mutableStateOf<AppItem?>(null) }
    val ringHost = remember { FocusRingHost() }

    val visible = vm.orderedApps.filter { it.pkg !in s.hidden }
    val home = vm.homeAppList
    val watch = vm.watchApps

    val launchApp: (AppItem, Rect) -> Unit = { app, r ->
        val corner = if (s.tileStyle == 1) r.height / 2f else 12f * density.density
        vm.startLaunch(app, r, corner)
    }

    LaunchedEffect(vm.homeSignal) { tabIndex = 0; moving = null }

    BackHandler(enabled = moving != null) { moving = null }
    BackHandler(enabled = moving == null && tabIndex != 0) { tabIndex = 0 }

    val ms = animMs(320)
    val pad = if (s.safeArea) Look.at(Look.Paddings, s.screenPadding).dp else 8.dp
    val vpad = if (s.safeArea) 18.dp else 6.dp

    CompositionLocalProvider(LocalRingHost provides ringHost) {
        Box(Modifier.fillMaxSize()) {
            HomeBackground(if (tabIndex == 0) bgApp else null)

            Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = vpad)) {
                TopBar(vm, tabIndex, onTab = { tabIndex = it }, openSettings = openSettings)
                
                Spacer(Modifier.height(24.dp)) 

                AnimatedContent(
                    targetState = tabIndex,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(ms, delayMillis = ms / 3)) + slideInHorizontally(tween(ms)) { dir * it / 10 }) togetherWith
                            (fadeOut(tween(ms / 2)) + slideOutHorizontally(tween(ms)) { -dir * it / 10 })
                    },
                    label = "tabs"
                ) { t ->
                    val actualTab = s.tabOrder.getOrElse(t) { 0 }
                    when (actualTab) {
                        0 -> Column(Modifier.fillMaxSize()) {
                            TvBanner(vm) { 
                                val tvIndex = s.tabOrder.indexOf(2)
                                if (tvIndex != -1) tabIndex = tvIndex
                            }
                            Spacer(Modifier.height(24.dp))
                            
                            ForYouPage(
                                vm, home, visible, watch, moving,
                                onMenu = { menuApp = it }, onDone = { moving = null },
                                onBg = { bgApp = it },
                                onLaunch = launchApp,
                                onAddApps = { showHomeApps = true },
                                onPickWatch = { showWatchApps = true },
                                onChannels = { showChannels = true },
                                onReorder = { showReorder = true }
                            )
                        }
                        1 -> AppsGridPage(
                            vm, visible, tr("Нет приложений", "No apps"), moving,
                            onMenu = { menuApp = it }, onDone = { moving = null }, onLaunch = launchApp
                        )
                        else -> TvScreen(vm)
                    }
                }
            }

            AnimatedVisibility(
                visible = moving != null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                enter = fadeIn(tween(ms)) + slideInVertically(tween(ms)) { it },
                exit = fadeOut(tween(ms)) + slideOutVertically(tween(ms)) { it }
            ) {
                Text(
                    tr("◀ ▶ ▲ ▼ - переместить   ·   OK или Назад - готово", "◀ ▶ ▲ ▼ - move   ·   OK or Back - done"),
                    color = cs.onTertiaryContainer, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape).background(cs.tertiaryContainer).padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            if (s.focusStyle == 3) FocusRingOverlay(ringHost)

            vm.launching?.let { LaunchOverlay(vm, it) }
        }
    }

    menuApp?.let { app ->
        AppMenuDialog(app, app.pkg in s.favorites, home.any { it.pkg == app.pkg }, app.pkg in s.cwApps, onAction = { a ->
            when (a) {
                AppAction.OPEN -> vm.launch(app.pkg)
                AppAction.MOVE -> { vm.freezeOrder(); moving = app.pkg }
                AppAction.HOME -> vm.toggleHome(app.pkg)
                AppAction.WATCH -> vm.toggleWatch(app.pkg)
                AppAction.FAVORITE -> vm.toggleFavorite(app.pkg)
                AppAction.HIDE -> vm.update { it.copy(hidden = it.hidden + app.pkg) }
                AppAction.INFO -> vm.openAppInfo(app.pkg)
                AppAction.UNINSTALL -> vm.uninstall(app.pkg)
            }
            menuApp = null
        }, onDismiss = { menuApp = null })
    }

    if (showHomeApps) HomeAppsDialog(vm) { showHomeApps = false }
    if (showWatchApps) WatchAppsDialog(vm) { showWatchApps = false }
    if (showChannels) ChannelsDialog(vm) { showChannels = false }
    if (showReorder) ReorderAppsDialog(vm) { showReorder = false }
}

@Composable
private fun LaunchOverlay(vm: HomeViewModel, la: LaunchAnim) {
    val prog = remember { Animatable(if (la.launched) 1f else 0f) }
    val ms = animMs(320)
    LaunchedEffect(la.expanded) {
        if (la.expanded) {
            if (!la.launched) {
                prog.animateTo(1f, tween(ms, easing = FastOutSlowInEasing))
                vm.launchNow()
            }
        } else {
            prog.animateTo(0f, tween(ms, easing = FastOutSlowInEasing))
            vm.clearLaunch()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fullW = constraints.maxWidth.toFloat()
        val fullH = constraints.maxHeight.toFloat()
        val p = prog.value
        val r = la.rect
        val x = lerp(r.left, 0f, p)
        val y = lerp(r.top, 0f, p)
        val w = lerp(r.width, fullW, p)
        val h = lerp(r.height, fullH, p)
        val corner = lerp(la.corner, 0f, p)
        val d = LocalDensity.current
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(with(d) { w.toDp() }, with(d) { h.toDp() })
                .clip(RoundedCornerShape(with(d) { corner.toDp() }))
                .background(NeutralTile),
            contentAlignment = Alignment.Center
        ) {
            val b = la.app.banner
            if (b != null) {
                Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                la.app.icon?.let { Image(it, null, Modifier.size(with(d) { (minOf(w, h) * 0.5f).toDp() })) }
            }
        }
    }
}

@Composable
private fun KenBurns(enabled: Boolean, content: @Composable () -> Unit) {
    if (!enabled) { Box(Modifier.fillMaxSize().graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }) { content() }; return }
    val inf = rememberInfiniteTransition(label = "kenburns")
    val k by inf.animateFloat(
        initialValue = 1.12f, targetValue = 1.22f,
        animationSpec = infiniteRepeatable(animation = tween(18000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "kb"
    )
    Box(Modifier.fillMaxSize().graphicsLayer { scaleX = k; scaleY = k }) { content() }
}

@Composable
private fun HomeBackground(app: AppItem?) {
    val s = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background)) {
        when (s.bgStyle) {
            1 -> Unit
            0 -> Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(cs.background, cs.primaryContainer.copy(alpha = 0.35f), cs.background))
                )
            )
            else -> {
                var shown by remember { mutableStateOf(app) }
                LaunchedEffect(app) { delay(250); shown = app }
                Crossfade(targetState = shown, animationSpec = tween(animMs(550)), label = "bg") { a ->
                    val bmp = a?.banner ?: a?.icon
                    if (bmp != null) {
                        KenBurns(s.kenBurns && s.animOn) {
                            val m = if (Build.VERSION.SDK_INT >= 31) Modifier.fillMaxSize().blur(26.dp) else Modifier.fillMaxSize()
                            Image(bmp, null, m, contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(cs.background, cs.primaryContainer.copy(alpha = 0.35f), cs.background))
                            )
                        )
                    }
                }
                val dim = Look.at(Look.BgDim, s.bgDim)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(cs.background.copy(alpha = 0.92f), cs.background.copy(alpha = dim * 0.45f)))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(cs.background.copy(alpha = dim * 0.5f), cs.background.copy(alpha = dim * 0.7f), cs.background))))
            }
        }
    }
}

@Composable
private fun TopBar(vm: HomeViewModel, tabIndex: Int, onTab: (Int) -> Unit, openSettings: () -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (s.showLogo) {
            Image(
                painterResource(R.drawable.tanix_logo), "Tanix",
                Modifier.height(26.dp).aspectRatio(720f / 177f).alpha(Look.at(Look.LogoAlphas, s.logoAlpha)),
                colorFilter = ColorFilter.tint(cs.onSurface)
            )
            Spacer(Modifier.width(28.dp))
        }
        if (s.showTabs) {
            Row(
                Modifier.clip(CircleShape).background(cs.surfaceContainer.copy(alpha = 0.75f)).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                s.tabOrder.forEachIndexed { i, tabId ->
                    val title = when (tabId) {
                        0 -> tr("Дом", "Home")
                        1 -> tr("Приложения", "Apps")
                        else -> tr("ТВ", "TV")
                    }
                    val icon = when (tabId) {
                        0 -> Icons.Default.Home
                        1 -> Icons.Default.Menu
                        else -> Icons.Default.PlayArrow
                    }
                    TabPillWithIcon(title, icon, tabIndex == i, onFocus = { if (s.tabOnFocus) onTab(i) }, onClick = { onTab(i) })
                    if (i < s.tabOrder.lastIndex) Spacer(Modifier.width(16.dp)) 
                }
            }
        }
        Spacer(Modifier.weight(1f))

        if (s.showWeather) { WeatherChip(vm); Spacer(Modifier.width(20.dp)) }

        if (s.showClock || s.showDate) {
            Column(horizontalAlignment = Alignment.End) {
                val loc = if (s.lang == 1) Locale.ENGLISH else Locale("ru")
                if (s.showClock) {
                    val f = if (s.clock24) "HH:mm" else "h:mm a"
                    Text(SimpleDateFormat(f, loc).format(now), color = cs.onSurface, style = MaterialTheme.typography.titleLarge)
                }
                if (s.showDate) {
                    Text(SimpleDateFormat("EEE, d MMM", loc).format(now), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.width(20.dp))
        }

        FocusBox(Modifier, CircleShape, 1.12f, onClick = openSettings) { f ->
            Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, tr("Настройки", "Settings"), tint = if (f) cs.onPrimary else cs.onSecondaryContainer)
            }
        }
    }
}

@Composable
fun TabPillWithIcon(text: String, icon: ImageVector, selected: Boolean, onFocus: () -> Unit, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusBox(Modifier, CircleShape, 1.04f, onClick, onFocusChange = { if (it) onFocus() }) { f ->
        val bg = when { f -> cs.primary; selected -> cs.secondaryContainer; else -> Color.Transparent }
        val fg = when { f -> cs.onPrimary; selected -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant }
        Row(
            Modifier.background(bg).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun WeatherChip(vm: HomeViewModel) {
    val cs = MaterialTheme.colorScheme
    val s = vm.s
    val w = vm.weather
    
    val isSunny = w?.code in 0..3
    val bgColor = if (isSunny) Color(0xFFFFB300) else cs.surfaceContainerHigh
    
    FocusBox(Modifier, CircleShape, 1.05f, onClick = { /* Открыть детальную погоду */ }) { f ->
        Row(
            Modifier.background(if (f) cs.primary else bgColor).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentColor = if (f) cs.onPrimary else if (isSunny) Color.White else cs.onSurface
            
            if (w == null) {
                WeatherIcon(3, 24.dp, contentColor)
                Spacer(Modifier.width(8.dp))
                Text("...", color = contentColor, style = MaterialTheme.typography.titleMedium)
            } else {
                val r = w.temp.roundToInt()
                WeatherIcon(w.code, 24.dp, contentColor)
                Spacer(Modifier.width(8.dp))
                Text((if (r > 0) "+$r" else "$r") + "°", color = contentColor, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
