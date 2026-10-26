package com.tanix.launcher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.R
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay

/** Первый запуск: анимация приветствия (как на новом Pixel), затем пошаговая настройка. */
@Composable
fun Onboarding(vm: HomeViewModel) {
    var splash by remember { mutableStateOf(true) }
    if (splash) WelcomeSplash { splash = false } else OnboardingSteps(vm)
}

/** Приветствие в стиле Pixel: плавающие цветные пятна, логотип «вырастает», текст сменяется через 5 секунд. */
@Composable
private fun WelcomeSplash(onDone: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    var logoIn by remember { mutableStateOf(false) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        logoIn = true
        delay(5000)
        phase = 1
        delay(4300)
        onDone()
    }
    LaunchedEffect(Unit) { delay(400); runCatching { fr.requestFocus() } }

    val logoA by animateFloatAsState(if (logoIn) 1f else 0f, tween(1500, easing = FastOutSlowInEasing), label = "logoA")
    val logoS by animateFloatAsState(if (logoIn) 1f else 0.5f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow), label = "logoS")
    val inf = rememberInfiniteTransition(label = "blobs")
    val t by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "t"
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun blob(c: Color, cx: Float, cy: Float, r: Float) {
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.38f), Color.Transparent), center = Offset(cx, cy), radius = r),
                    radius = r, center = Offset(cx, cy)
                )
            }
            blob(Color(0xFF4285F4), w * (0.22f + 0.12f * t), h * (0.30f + 0.10f * t), w * 0.42f)
            blob(Color(0xFFEA4335), w * (0.80f - 0.10f * t), h * (0.25f + 0.12f * t), w * 0.34f)
            blob(Color(0xFFFBBC04), w * (0.70f + 0.08f * t), h * (0.85f - 0.12f * t), w * 0.36f)
            blob(Color(0xFF34A853), w * (0.20f + 0.10f * t), h * (0.88f - 0.10f * t), w * 0.34f)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.tanix_logo), null,
                Modifier.height(66.dp).aspectRatio(720f / 177f).graphicsLayer { alpha = logoA; scaleX = logoS; scaleY = logoS },
                colorFilter = ColorFilter.tint(Color.White)
            )
            Spacer(Modifier.height(34.dp))
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(tween(900, delayMillis = 350)) + slideInVertically(tween(900, easing = FastOutSlowInEasing)) { it / 2 } +
                        scaleIn(tween(900), initialScale = 0.9f)) togetherWith
                        (fadeOut(tween(550)) + slideOutVertically(tween(650)) { -it / 2 })
                },
                label = "welcomeText"
            ) { p ->
                WelcomeText(if (p == 0) "Welcome to Tanix Launcher" else "Добро пожаловать в Tanix Launcher")
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
            Chip(tr("Пропустить", "Skip"), modifier = Modifier.focusRequester(fr)) { onDone() }
        }
    }
}

/** Слова появляются по очереди: проявление + всплытие. */
@Composable
private fun WelcomeText(text: String) {
    val words = text.split(" ")
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
        words.forEachIndexed { i, word ->
            var shown by remember(text) { mutableStateOf(false) }
            LaunchedEffect(text) { delay(500L + i * 260L); shown = true }
            val a by animateFloatAsState(if (shown) 1f else 0f, tween(750, easing = FastOutSlowInEasing), label = "w$i")
            Text(
                word,
                Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 28f },
                style = TextStyle(
                    color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light, letterSpacing = 0.5.sp
                )
            )
        }
    }
}

@Composable
private fun OnboardingSteps(vm: HomeViewModel) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val ctx = rememberCtx()
    var step by remember { mutableIntStateOf(0) }
    var showTvApp by remember { mutableStateOf(false) }
    var showHome by remember { mutableStateOf(false) }
    var showWatch by remember { mutableStateOf(false) }
    val last = 5
    val next = remember { FocusRequester() }
    LaunchedEffect(step) { delay(200); runCatching { next.requestFocus() } }

    Box(Modifier.fillMaxSize().background(cs.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(R.drawable.tanix_logo), null,
                Modifier.height(if (step == 0) 54.dp else 30.dp).aspectRatio(720f / 177f).alpha(0.85f),
                colorFilter = ColorFilter.tint(cs.onBackground)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                tr("Шаг ", "Step ") + (step + 1) + tr(" из ", " of ") + (last + 1),
                color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(14.dp))

            when (step) {
                0 -> {
                    Text(tr("Добро пожаловать!", "Welcome!"), color = cs.onBackground, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tr(
                            "Tanix — лаунчер в стиле Material You для вашей приставки.\n\n" +
                                "• главный экран как в Google TV: «Продолжить просмотр» и ваши приложения\n" +
                                "• вкладки «Для вас», «Приложения», «Магазин приложений»\n" +
                                "• значки можно переставлять (долгое нажатие OK)\n" +
                                "• погода на главном экране\n" +
                                "• кнопка ТВ: быстрый запуск Lampa, ILTV и др.\n" +
                                "• десятки настроек оформления и анимаций\n\n" +
                                "Настроим всё за минуту.",
                            "Tanix is a Material You style launcher for your set-top box.\n\n" +
                                "• Google TV style home: Continue watching and your apps\n" +
                                "• tabs: For you, Apps, App store\n" +
                                "• reorder icons (long press OK)\n" +
                                "• weather on the home screen\n" +
                                "• TV button: quick launch for Lampa, ILTV, etc.\n" +
                                "• dozens of look & animation settings\n\n" +
                                "Let's set it up in a minute."
                        ),
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start
                    )
                    SettingsSection(tr("Язык", "Language")) { LanguageBlock(vm) }
                }
                1 -> {
                    StepTitle(tr("Внешний вид", "Appearance"))
                    SettingsSection(tr("Тема и цвета", "Theme and colors")) { ThemeBlock(vm) }
                }
                2 -> {
                    StepTitle(tr("Плитки приложений", "App tiles"))
                    SettingsSection(tr("Вид и размер", "Style and size")) { IconsBlock(vm) }
                }
                3 -> {
                    StepTitle(tr("Что показывать на главном экране", "What to show on the home screen"))
                    SettingsSection(tr("Можно изменить позже в настройках", "You can change this later in settings")) {
                        SwitchRow(tr("Погода", "Weather"), null, s.showWeather) { v -> vm.update { it.copy(showWeather = v) } }
                        SwitchRow(tr("Часы", "Clock"), null, s.showClock) { v -> vm.update { it.copy(showClock = v) } }
                        SwitchRow(tr("Продолжить просмотр", "Continue watching"), null, s.showWatch) { v -> vm.update { it.copy(showWatch = v) } }
                        SwitchRow(tr("Ряд «Избранное»", "Favorites row"), null, s.showFavRow) { v -> vm.update { it.copy(showFavRow = v) } }
                        SwitchRow(tr("Ряд «Недавние»", "Recent row"), null, s.showRecentRow) { v -> vm.update { it.copy(showRecentRow = v) } }
                        OptionRow(
                            tr("Фон", "Background"),
                            listOf(tr("Градиент", "Gradient"), tr("Однотонный", "Solid"), tr("Как в Google TV", "Like Google TV")), s.bgStyle
                        ) { v -> vm.update { it.copy(bgStyle = v) } }
                    }
                }
                4 -> {
                    StepTitle(tr("Погода", "Weather"))
                    SettingsSection(tr("Город и единицы", "City and units")) { WeatherBlock(vm) }
                }
                else -> {
                    StepTitle(tr("Почти готово", "Almost done"))
                    Text(
                        tr("Выберите приложения для «Продолжить просмотр» и кнопки ТВ, затем сделайте Tanix главным экраном (кнопка Home).",
                            "Choose apps for Continue watching and the TV button, then make Tanix your home screen (Home button)."),
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                    )
                    SettingsSection(tr("Последний шаг", "Last step")) {
                        SettingRow(
                            tr("Продолжить просмотр", "Continue watching"),
                            tr("Выбрано приложений: ", "Apps chosen: ") + s.cwApps.size, onClick = { showWatch = true }
                        )
                        SettingRow(tr("Приложения на главном экране", "Apps on the home screen"), tr("Выберите около 15", "Choose about 15"), onClick = { showHome = true })
                        SettingRow(
                            tr("Приложение для кнопки ТВ", "App for the TV button"),
                            vm.apps.firstOrNull { it.pkg == s.tvPkg }?.label ?: tr("Например Lampa или ILTV", "For example Lampa or ILTV"),
                            onClick = { showTvApp = true }
                        )
                        SettingRow(
                            tr("Открыть настройки главного экрана", "Open home screen settings"),
                            tr("Выберите Tanix в списке", "Pick Tanix in the list"), onClick = { openHomeSettings(ctx) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("Если системного пункта нет — через ADB:\n", "If there is no system option, use ADB:\n") +
                            "adb shell cmd package set-home-activity com.tanix.launcher/.MainActivity",
                        color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                    )
                }
            }

            if (showHome) HomeAppsDialog(vm) { showHome = false }
            if (showWatch) WatchAppsDialog(vm) { showWatch = false }
            if (showTvApp) AppListDialog(tr("Приложение для кнопки ТВ", "App for the TV button"), vm.orderedApps, onClick = { app ->
                vm.update { it.copy(tvPkg = app.pkg) }; showTvApp = false
            }, onDismiss = { showTvApp = false })

            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) Chip(tr("Назад", "Back")) { step-- }
                Chip(
                    if (step == last) tr("Готово", "Done") else if (step == 0) tr("Начать", "Start") else tr("Далее", "Next"),
                    selected = true, modifier = Modifier.focusRequester(next)
                ) {
                    if (step == last) vm.update { it.copy(onboarded = true) } else step++
                }
            }
        }
    }
}

@Composable
private fun StepTitle(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
}
