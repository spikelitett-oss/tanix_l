@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.tanix.launcher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.animation.core.VectorConverter
import com.tanix.launcher.data.LauncherSettings
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tanix.launcher.data.AppItem
import kotlinx.coroutines.delay

fun Modifier.tvClickable(onClick: () -> Unit, onLongClick: (() -> Unit)? = null) = composed {
    var isPressed by remember { mutableStateOf(false) }
    var longPressFired by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed && onLongClick != null) {
            longPressFired = false
            delay(550L)
            if (isPressed) {
                longPressFired = true
                onLongClick.invoke()
            }
        }
    }
    this.onKeyEvent { event ->
        if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
            when (event.type) {
                KeyEventType.KeyDown -> {
                    if (!isPressed) { isPressed = true; true } else false
                }
                KeyEventType.KeyUp -> {
                    if (isPressed) {
                        isPressed = false
                        if (!longPressFired) onClick()
                        true
                    } else false
                }
                else -> false
            }
        } else false
    }.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
        onClick()
    }
}

class FocusRingHost {
    var rect by mutableStateOf<Rect?>(null)
    var radius by mutableFloatStateOf(0f)
    var color by mutableStateOf(Color.White)
    var widthPx by mutableFloatStateOf(6f)
    var active by mutableStateOf(false)
    var owner: Any? = null
}

val LocalRingHost = staticCompositionLocalOf<FocusRingHost?> { null }

class CoordsHolder { var c: LayoutCoordinates? = null }
class RectHolder { var r: Rect = Rect.Zero }

@Composable
fun FocusRingOverlay(host: FocusRingHost) {
    // Отключено для Material You 3 Expressive
}

@Composable
fun FocusBox(
    modifier: Modifier = Modifier,
    shape: Shape,
    focusScale: Float = 1.06f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onKey: ((KeyEvent) -> Boolean)? = null,
    ringColor: Color? = null,
    focusRequester: FocusRequester? = null,
    content: @Composable (Boolean) -> Unit
) {
    val st = LocalSettings.current
    val cs = MaterialTheme.colorScheme
    var f by remember { mutableStateOf(false) }
    
    val target = 1f + (focusScale - 1f) * Look.at(Look.FocusK, st.focusK)
    val damping = if (st.expressive) 0.5f else 0.75f
    val spec: AnimationSpec<Float> = if (st.animOn) spring(dampingRatio = damping, stiffness = 400f / Look.at(Look.SpeedK, st.animSpeed)) else snap()
    val sc by animateFloatAsState(if (f) target else 1f, spec, label = "focusScale")
    
    // Material You 3 Expressive: заливка фона при фокусе
    val bg by animateColorAsState(
        when {
            f -> cs.primary
            else -> Color.Transparent
        },
        animationSpec = tween(animMs(200)), label = "m3_bg"
    )

    Box(
        modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scale(sc)
            .onFocusChanged {
                f = it.isFocused
                onFocusChange?.invoke(it.isFocused)
            }
            .onKeyEvent { e -> onKey?.invoke(e) ?: false }
            .clip(shape)
            .background(bg)
            .tvClickable(onClick = onClick, onLongClick = onLongClick)
    ) { content(f) }
}

@Composable
fun Chip(text: String, selected: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusBox(modifier, CircleShape, 1.05f, onClick) { f ->
        val bg = when { f -> cs.primary; selected -> cs.secondaryContainer; else -> cs.surfaceContainerHigh }
        val fg = when { f -> cs.onPrimary; selected -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant }
        Text(
            (if (selected) "✓ " else "") + text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (LocalSettings.current.expressive) 10.dp else 8.dp)
        )
    }
}

@Composable
fun TabPill(text: String, selected: Boolean, onFocus: () -> Unit, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    FocusBox(Modifier, CircleShape, 1.04f, onClick, onFocusChange = { if (it) onFocus() }) { f ->
        val bg by animateColorAsState(
            when { f -> cs.primary; selected -> cs.secondaryContainer; else -> Color.Transparent },
            tween(animMs(200)), label = "tabBg"
        )
        val fg = when { f -> cs.onPrimary; selected -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            modifier = Modifier.background(bg).padding(horizontal = 20.dp, vertical = 10.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Text(title, color = cs.primary, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainer).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun SettingRow(
    title: String, subtitle: String? = null, onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    FocusBox(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), 1.01f, onClick, onLongClick) { f ->
        Row(
            Modifier.fillMaxWidth().background(if (f) cs.primaryContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = if (f) cs.onPrimaryContainer else cs.onSurface, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(it, color = if (f) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, subtitle, onClick = { onChange(!checked) }) { Switch(checked = checked, onCheckedChange = null) }
}

@Composable
fun OptionRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = cs.onSurface, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, o -> Chip(o, i == selected) { onSelect(i) } }
        }
    }
}

@Composable
fun AccentPicker(hue: Float, onPick: (Float) -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AccentHues.forEach { h ->
            FocusBox(Modifier, CircleShape, 1.15f, { onPick(h) }) {
                Box(Modifier.size(38.dp).background(hsl(h, .7f, .6f)), contentAlignment = Alignment.Center) {
                    if (h == hue) Icon(Icons.Filled.Check, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun TvIcon(tint: Color, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier) {
        val sw = 2.2.dp.toPx()
        drawRoundRect(
            tint, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height * 0.74f),
            cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(sw)
        )
        drawLine(tint, Offset(size.width * 0.28f, size.height - sw / 2), Offset(size.width * 0.72f, size.height - sw / 2),
            strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
fun AppIcon(app: AppItem, size: Dp, shape: Shape = iconShapeOf(0)) {
    val circle = shape === CircleShape
    Box(Modifier.size(size).clip(shape).background(if (circle) Color.White else NeutralTile), contentAlignment = Alignment.Center) {
        app.icon?.let {
            if (app.adaptive) Image(it, app.label, Modifier.fillMaxSize().scale(if (circle) 1.2f else 1.5f))
            else Image(it, app.label, Modifier.fillMaxSize().padding(size * (if (circle) 0.22f else 0.18f)))
        }
    }
}
