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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay

// Я переименовал функцию в Onboarding и добавил onDone, чтобы MainActivity могла её вызвать
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
                onDone() // Вызываем колбэк, чтобы MainActivity переключилась на главный экран
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
        Box(Modifier.weight(1f).fillMaxHeight()) {
            AnimatedCurves()
        }
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
}

@Composable
fun AnimatedCurves() {
    val inf = rememberInfiniteTransition(label = "curves")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    
    Canvas(Modifier.fillMaxSize().padding(32.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, h * 0.5f)
            cubicTo(
                w * 0.3f, h * (0.2f + phase * 0.2f),
                w * 0.7f, h * (0.8f - phase * 0.2f),
                w, h * 0.5f
            )
        }
        drawPath(path, color = Color(0xFF90CAF9), style = Stroke(width = 4.dp.toPx()))
        
        val path2 = Path().apply {
            moveTo(0f, h * 0.6f)
            cubicTo(
                w * 0.4f, h * (0.9f - phase * 0.2f),
                w * 0.6f, h * (0.1f + phase * 0.2f),
                w, h * 0.6f
            )
        }
        drawPath(path2, color = Color(0xFF64B5F6), style = Stroke(width = 2.dp.toPx()))
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
