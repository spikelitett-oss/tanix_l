@file:OptIn(ExperimentalFoundationApi::class)

package com.tanix.launcher.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
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
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }
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

    LaunchedEffect(vm.homeSignal) { tab = 0; moving = null }
    LaunchedEffect(s.showTabs) { if (!s.showTabs) tab = 0 }

    BackHandler(enabled = moving != null) { moving = null }
    BackHandler(enabled = moving == null && tab != 0) { tab = 0 }

    val ms = animMs(320)
    val pad = if (s.safeArea) Look.at(Look.Paddings, s.screenPadding).dp else 8.dp
    val vpad = if (s.safeArea) 18.dp else 6.dp

    CompositionLocalProvider(LocalRingHost provides ringHost) {
        Box(Modifier.fillMaxSize()) {
            HomeBackground(if (tab == 0) bgApp else null)

            Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = vpad)) {
                TopBar(vm, tab, onTab = { tab = it }, openSettings = openSettings, onPick = { showPicker = true })
                Spacer(Modifier.height(10.dp))

                // Переключение вкладок: сдвиг + проявление в сторону движения (как в Google TV)
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(ms, delayMillis = ms / 3)) + slideInHorizontally(tween(ms)) { dir * it / 10 }) togetherWith
                            (fadeOut(tween(ms / 2)) + slideOutHorizontally(tween(ms)) { -dir * it / 10 })
                    },
                    label = "tabs"
                ) { t ->
                    when (t) {
                        0 -> ForYouPage(
                            vm, home, visible, watch, moving,
                            onMenu = { menuApp = it }, onDone = { moving = null },
                            onBg = { bgApp = it },
                            onLaunch = launchApp,
                            onAddApps = { showHomeApps = true },
                            onPickWatch = { showWatchApps = true },
                            onChannels = { showChannels = true },
                            onReorder = { showReorder = true }
                        )
                        1 -> AppsGridPage(
                            vm, visible, tr("Нет приложений", "No apps"), moving,
                            onMenu = { menuApp = it }, onDone = { moving = null }, onLaunch = launchApp
                        )
                        else -> StorePage(vm)
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
                    tr("◀ ▶ ▲ ▼ — переместить   ·   OK или Назад — готово", "◀ ▶ ▲ ▼ — move   ·   OK or Back — done"),
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

    if (showPicker) {
        AppListDialog(tr("Приложение для кнопки ТВ", "App for the TV button"), visible, onClick = { app ->
            vm.update { it.copy(tvPkg = app.pkg) }; showPicker = false
        }, onDismiss = { showPicker = false })
    }
    if (showHomeApps) HomeAppsDialog(vm) { showHomeApps = false }
    if (showWatchApps) WatchAppsDialog(vm) { showWatchApps = false }
    if (showChannels) ChannelsDialog(vm) { showChannels = false }
    if (showReorder) ReorderAppsDialog(vm) { showReorder = false }
}

/** Приложение «раскрывается» из плитки на весь экран; при закрытии приложения — сворачивается обратно в плитку. */
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

/** Медленный наезд фона (эффект Кена Бернса). */
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

/** Фон: градиент, однотонный или размытый кадр выбранной карточки «Продолжить просмотр» (как в Google TV). */
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
                            // blur работает на Android 12+; на старых версиях картинка и так мягкая (увеличенный маленький баннер)
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
private fun TopBar(vm: HomeViewModel, tab: Int, onTab: (Int) -> Unit, openSettings: () -> Unit, onPick: () -> Unit) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }
    val tabs = listOf(tr("Для вас", "For you"), tr("Приложения", "Apps"), tr("Магазин приложений", "App store"))

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
                tabs.forEachIndexed { i, name ->
                    TabPill(name, tab == i, onFocus = { if (s.tabOnFocus) onTab(i) }, onClick = { onTab(i) })
                    if (i < tabs.lastIndex) Spacer(Modifier.width(10.dp))
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

        // Кнопка ТВ: открывает выбранное приложение (Lampa, ILTV…). Долгое нажатие — сменить приложение.
        FocusBox(
            Modifier, CircleShape, 1.12f,
            onClick = { if (vm.openTv()) onPick() },
            onLongClick = onPick
        ) { f ->
            Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                TvIcon(if (f) cs.onPrimary else cs.onSecondaryContainer)
            }
        }
        Spacer(Modifier.width(16.dp))
        FocusBox(Modifier, CircleShape, 1.12f, onClick = openSettings) { f ->
            Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Settings, tr("Настройки", "Settings"), tint = if (f) cs.onPrimary else cs.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun WeatherChip(vm: HomeViewModel) {
    val cs = MaterialTheme.colorScheme
    val s = vm.s
    val en = s.lang == 1
    val w = vm.weather
    FocusBox(Modifier, CircleShape, 1.05f, onClick = { vm.loadWeather() }) { f ->
        val fg = if (f) cs.onPrimary else cs.onSecondaryContainer
        Row(
            Modifier.background(if (f) cs.primary else cs.secondaryContainer).padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (w == null) {
                WeatherIcon(3, 32.dp)
                Spacer(Modifier.width(6.dp))
                Text(if (vm.weatherError != null) "—°" else "…", color = fg, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(s.city, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            } else {
                val r = w.temp.roundToInt()
                WeatherIcon(w.code, 32.dp)
                Spacer(Modifier.width(6.dp))
                Text((if (r > 0) "+$r" else "$r") + "°", color = fg, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(s.city, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                if (s.weatherDesc) {
                    Spacer(Modifier.width(8.dp))
                    Text(com.tanix.launcher.data.Repo.weatherDesc(w.code, en), color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
                if (s.weatherFeels) {
                    val fl = w.feels.roundToInt()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        (if (en) "feels " else "ощущ. ") + (if (fl > 0) "+$fl" else "$fl") + "°",
                        color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1
                    )
                }
            }
        }
    }
}
