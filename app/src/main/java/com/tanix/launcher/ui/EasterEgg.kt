package com.tanix.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tanix.launcher.R
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Пасхалка: логотип Tanix (после 7 нажатий). OK — закрыть, долгое удержание OK на логотипе — змейка. */
@Composable
fun LogoEgg(onDismiss: () -> Unit, onPlay: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(450))
    }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) { delay(250); runCatching { fr.requestFocus() } }
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                FocusBox(Modifier.focusRequester(fr), RoundedCornerShape(24.dp), 1.06f, onClick = onDismiss, onLongClick = onPlay) {
                    Box(
                        Modifier.background(Color(0xFF15151B)).padding(horizontal = 36.dp, vertical = 28.dp)
                            .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
                    ) {
                        Image(
                            painterResource(R.drawable.tanix_logo), "Tanix",
                            Modifier.width(260.dp).aspectRatio(720f / 177f),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Tanix Launcher 1.0", color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private data class P(val x: Int, val y: Int)

/** Мини-игра «Змейка»: стрелки на пульте, OK — начать заново, Назад — выход. */
@Composable
fun SnakeGame(onDismiss: () -> Unit) {
    val cols = 28
    val rows = 15
    var snake by remember { mutableStateOf(listOf(P(8, 7), P(7, 7), P(6, 7))) }
    var dir by remember { mutableStateOf(P(1, 0)) }
    var food by remember { mutableStateOf(P(18, 7)) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var round by remember { mutableIntStateOf(0) }
    val fr = remember { FocusRequester() }
    val cs = MaterialTheme.colorScheme

    fun newFood(body: List<P>): P {
        while (true) {
            val p = P(Random.nextInt(cols), Random.nextInt(rows))
            if (p !in body) return p
        }
    }

    fun restart() {
        snake = listOf(P(8, 7), P(7, 7), P(6, 7))
        dir = P(1, 0)
        food = newFood(snake)
        score = 0
        over = false
        round++
    }

    LaunchedEffect(Unit) { delay(200); runCatching { fr.requestFocus() } }

    LaunchedEffect(round) {
        while (!over) {
            delay(max(70L, 150L - score * 4L))
            val head = snake.first()
            val d = dir
            val nx = head.x + d.x
            val ny = head.y + d.y
            val np = P(nx, ny)
            if (nx < 0 || ny < 0 || nx >= cols || ny >= rows || np in snake.dropLast(1)) {
                over = true
                best = max(best, score)
            } else {
                val ate = np == food
                val body = if (ate) listOf(np) + snake else listOf(np) + snake.dropLast(1)
                snake = body
                if (ate) {
                    score++
                    food = newFood(body)
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF0B0B10))
                .focusRequester(fr).focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val cur = dir
                    when (e.key) {
                        Key.DirectionUp -> { if (cur.y == 0) dir = P(0, -1); true }
                        Key.DirectionDown -> { if (cur.y == 0) dir = P(0, 1); true }
                        Key.DirectionLeft -> { if (cur.x == 0) dir = P(-1, 0); true }
                        Key.DirectionRight -> { if (cur.x == 0) dir = P(1, 0); true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { if (over) restart(); true }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp)) {
                val cell = min(size.width / cols, size.height / rows)
                val ox = (size.width - cell * cols) / 2f
                val oy = (size.height - cell * rows) / 2f
                drawRoundRect(
                    Color(0xFF14141C), Offset(ox, oy), Size(cell * cols, cell * rows), CornerRadius(cell * 0.6f)
                )
                val pad = cell * 0.08f
                snake.forEachIndexed { i, p ->
                    val c = if (i == 0) Color(0xFF9CFF8A) else Color(0xFF4CAF50).copy(alpha = max(0.45f, 1f - i * 0.02f))
                    drawRoundRect(
                        c, Offset(ox + p.x * cell + pad, oy + p.y * cell + pad),
                        Size(cell - pad * 2, cell - pad * 2), CornerRadius(cell * 0.3f)
                    )
                }
                drawCircle(Color(0xFFFF5252), cell * 0.38f, Offset(ox + food.x * cell + cell / 2f, oy + food.y * cell + cell / 2f))
            }
            Text(
                "Tanix Snake · " + score + "   (" + tr("рекорд", "best") + " " + best + ")",
                color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
            )
            if (over) {
                Column(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(Color(0xE61E1E28)).padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Game over", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("OK — играть снова · Назад — выход", "OK — play again · Back — exit"), color = cs.onSurfaceVariant, fontSize = 14.sp)
                }
            } else if (score == 0 && snake.size == 3) {
                Text(
                    tr("Стрелки — управление", "Arrow keys to steer"), color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
                )
            }
        }
    }
}
