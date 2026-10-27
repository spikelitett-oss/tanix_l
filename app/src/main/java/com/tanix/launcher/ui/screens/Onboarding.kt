package com.tanix.launcher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay

@Composable
fun Onboarding(vm: HomeViewModel, onDone: () -> Unit = {}) {
    var step by remember { mutableIntStateOf(0) }
    val cs = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize().background(cs.background)) {
        when (step) {
            0 -> WelcomeScreen(onNext = { step = 1 })
            1 -> SetupStep(
                title = "Выберите язык",
                options = listOf("Русский", "English"),
                onSelect = { vm.update { s -> s.copy(lang = it) } },
                onNext = { step = 2 }
            )
            2 -> SetupStep(
                title = "Выберите тему",
                options = listOf("Темная", "Светлая", "Системная"),
                onSelect = { vm.update { s -> s.copy(themeMode = it) } },
                onNext = { step = 3 }
            )
            3 -> FinishScreen(onFinish = {
                vm.update { it.copy(onboarded = true) }
                onDone()
            })
        }
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    var showEnglish by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(10000L)
        showEnglish = false
    }

    Row(Modifier.fillMaxSize()) {
        // Левая часть с красивыми кругами
        Box(Modifier.weight(1f).fillMaxHeight()) {
            CollidingCircles()
        }
        // Правая часть с текстом
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            AnimatedContent(
                targetState = showEnglish,
                transitionSpec = { fadeIn(tween(1000)) togetherWith fadeOut(tween(1000)) },
                label = "welcome_text"
            ) { isEng ->
                if (isEng) {
                    Text(
                        "Welcome to\nyour Tanix Launcher",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Text(
                        "Добро пожаловать\nв Tanix Launcher",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
    
    // Кнопка "Далее", чтобы можно было продолжить настройку
    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.BottomEnd) {
        FocusBox(Modifier, CircleShape, 1.1f, onClick = onNext) { f ->
            Box(
                Modifier.size(72.dp).background(if (f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = if (f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun CollidingCircles() {
    val inf = rememberInfiniteTransition(label = "circles")
    // Анимация от 0 до 1 и обратно
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing), 
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Canvas(Modifier.fillMaxSize().padding(48.dp)) {
        val w = size.width
        val h = size.height
        val baseRadius = w * 0.35f
        
        // Позиция первого круга (движется вправо)
        val cx1 = w * 0.2f + (w * 0.3f * phase)
        // Позиция второго круга (движется влево)
        val cx2 = w * 0.8f - (w * 0.3f * phase)
        val cy = h * 0.5f

        // Первый круг (сине-голубой)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF64B5F6), Color(0xFF1976D2)),
                center = Offset(cx1 - baseRadius * 0.2f, cy - baseRadius * 0.2f)
            ),
            radius = baseRadius,
            center = Offset(cx1, cy)
        )

        // Второй круг (розово-фиолетовый, как в Material You)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF48FB1), Color(0xFFC2185B)),
                center = Offset(cx2 + baseRadius * 0.2f, cy + baseRadius * 0.2f)
            ),
            radius = baseRadius,
            center = Offset(cx2, cy)
        )
        
        // Эффект свечения при столкновении (когда они близко)
        if (phase > 0.7f) {
            val glowAlpha = (phase - 0.7f) / 0.3f
            drawCircle(
                color = Color.White.copy(alpha = glowAlpha * 0.4f),
                radius = baseRadius * 0.8f,
                center = Offset(w * 0.5f, cy)
            )
        }
    }
}

@Composable
fun SetupStep(title: String, options: List<String>, onSelect: (Int) -> Unit, onNext: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    Row(Modifier.fillMaxSize().padding(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f)
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            options.forEachIndexed { i, opt ->
                Chip(text = opt, selected = i == selected, onClick = {
                    selected = i
                    onSelect(i)
                })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    
    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.BottomEnd) {
        FocusBox(Modifier, CircleShape, 1.1f, onClick = onNext) { f ->
            Box(
                Modifier.size(72.dp).background(if (f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    tint = if (f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun FinishScreen(onFinish: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Настройка завершена",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(48.dp))
            FocusBox(Modifier, CircleShape, 1.05f, onClick = onFinish) { f ->
                Box(
                    Modifier.background(if (f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 48.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Начать",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
