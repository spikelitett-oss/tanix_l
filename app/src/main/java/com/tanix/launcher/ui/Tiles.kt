package com.tanix.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import kotlinx.coroutines.delay

@Composable
fun tileRing(): Color = if (LocalDark.current) Color.White else MaterialTheme.colorScheme.primary

@Composable
private fun tileShape(): Shape {
    val st = LocalSettings.current
    return if (st.tileStyle == 0) RoundedCornerShape(12.dp) else iconShapeOf(st.tileStyle - 1)
}

private fun tileSize(tileStyle: Int, iconSize: Int): Pair<Dp, Dp> {
    return if (tileStyle == 0) {
        val w = Look.at(Look.TileW, iconSize).dp
        w to w * 9f / 16f
    } else {
        val w = Look.at(Look.IconSizes, iconSize).dp
        w to w
    }
}

@Composable
fun AppTile(
    app: AppItem,
    isFav: Boolean,
    moving: Boolean,
    onClick: (Rect) -> Unit,
    onLongClick: () -> Unit,
    onKey: ((KeyEvent) -> Boolean)?,
    modifier: Modifier = Modifier
) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val banner = st.tileStyle == 0
    val shape = tileShape()
    val (w, h) = tileSize(st.tileStyle, st.iconSize)
    var focused by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    val holder = remember { RectHolder() }
    LaunchedEffect(moving) { if (moving) { delay(120); runCatching { fr.requestFocus() } } }

    Column(modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusBox(
            Modifier.graphicsLayer { if (moving) { scaleX = 1.08f; scaleY = 1.08f } },
            shape, 1.1f, { onClick(holder.r) }, onLongClick,
            onFocusChange = { focused = it },
            onKey = onKey, ringColor = if (moving) cs.tertiary else tileRing(), focusRequester = fr
        ) {
            Box(Modifier.width(w).height(h).onGloballyPositioned { holder.r = it.boundsInRoot() }) {
                when {
                    banner && app.banner != null ->
                        Image(app.banner, app.label, Modifier.fillMaxSize().clip(shape), contentScale = ContentScale.Crop)
                    banner -> Row(
                        Modifier.align(Alignment.Center).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.icon?.let { Image(it, null, Modifier.size(h * 0.55f).clip(RoundedCornerShape(8.dp))) }
                        Spacer(Modifier.width(6.dp))
                        Text(app.label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    else -> AppIcon(app, w, shape)
                }
                if (isFav) {
                    Text("★", color = Color(0xFFFFD54F), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                }
            }
        }
        val showLabel = when (st.labelMode) { 0 -> true; 1 -> focused || moving; else -> false }
        Box(Modifier.height(22.dp).padding(top = 4.dp), contentAlignment = Alignment.Center) {
            if (showLabel) {
                Text(
                    app.label, color = if (moving) cs.tertiary else cs.onSurface, style = MaterialTheme.typography.labelMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AddTile(onClick: () -> Unit) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val shape = tileShape()
    val (w, h) = tileSize(st.tileStyle, st.iconSize)
    Column(Modifier.width(w), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusBox(Modifier, shape, 1.1f, onClick, ringColor = tileRing()) {
            Box(Modifier.width(w).height(h).background(cs.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Text("+", color = cs.onSurfaceVariant, style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

fun moveKeyHandler(cols: Int, onMove: (Int) -> Unit): (KeyEvent) -> Boolean = { e ->
    if (e.type == KeyEventType.KeyDown) {
        when (e.key) {
            Key.DirectionLeft -> { onMove(-1); true }
            Key.DirectionRight -> { onMove(1); true }
            Key.DirectionUp -> { if (cols > 1) onMove(-cols); true }
            Key.DirectionDown -> { if (cols > 1) onMove(cols); true }
            else -> false
        }
    } else false
}

@Composable
fun WatchCard(
    app: AppItem,
    opened: Long?,
    onFocused: () -> Unit,
    onClick: (Rect) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val st = LocalSettings.current
    val w = Look.at(Look.PickW, st.posterSize)
    val h = w * 9 / 16
    val holder = remember { RectHolder() }
    FocusBox(
        modifier, cardShape(), 1.07f, onClick = { onClick(holder.r) }, onLongClick = onLongClick,
        onFocusChange = { if (it) onFocused() }, ringColor = tileRing()
    ) {
        Box(Modifier.width(w.dp).height(h.dp).background(NeutralTile).onGloballyPositioned { holder.r = it.boundsInRoot() }) {
            val b = app.banner
            if (b != null) {
                Image(b, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.align(Alignment.Center)) { AppIcon(app, (h * 0.55f).dp) }
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.82f))))
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 9.dp, vertical = 7.dp)) {
                Text(app.label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (opened != null) {
                    Text(
                        agoText(opened, st.lang == 1), color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall, maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun WatchAddCard(onClick: () -> Unit) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    val w = Look.at(Look.PickW, st.posterSize)
    val h = w * 9 / 16
    FocusBox(Modifier, cardShape(), 1.07f, onClick, ringColor = tileRing()) {
        Column(
            Modifier.width(w.dp).height(h.dp).background(cs.surfaceContainerHigh),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text("+", color = cs.onSurfaceVariant, style = MaterialTheme.typography.headlineMedium)
            Text(
                tr("Выбрать приложения", "Choose apps"), color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center
            )
        }
    }
}

/** Ряд классических каналов (YouTube, Lampa и т.д.) вместо огромного баннера */
@Composable
fun ChannelsRow(vm: HomeViewModel) {
    val channels = listOf("YouTube", "Lampa", "Twitch", "Netflix") // Заглушка, можно подтянуть из установленных
    Column(Modifier.fillMaxWidth()) {
        Text(
            tr("Популярные каналы", "Popular channels"),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            items(channels) { name ->
                FocusBox(Modifier, CircleShape, 1.15f, onClick = { /* open app */ }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(72.dp).clip(CircleShape).background(Color(0xFF333333)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.first().toString(), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}