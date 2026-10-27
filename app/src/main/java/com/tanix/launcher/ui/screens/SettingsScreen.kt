package com.tanix.launcher.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.R
import com.tanix.launcher.ui.*
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(vm: HomeViewModel, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val s = vm.s
    var cat by remember { mutableIntStateOf(0) }
    val ms = animMs(240)
    val cats = listOf(
        tr("Внешний вид", "Appearance"), tr("Главный экран", "Home screen"), tr("Плитки", "Tiles"),
        tr("Продолжить просмотр", "Continue watching"), tr("Анимации и контур", "Animations & outline"),
        tr("Погода и время", "Weather & time"), tr("Приложения", "Apps"), tr("Система", "System"),
        tr("О лаунчере", "About")
    )
    val pad = if (s.safeArea) Look.at(Look.Paddings, s.screenPadding).dp else 12.dp
    val vpad = if (s.safeArea) 24.dp else 8.dp

    Box(Modifier.fillMaxSize().background(cs.background)) {
        Row(Modifier.fillMaxSize().padding(horizontal = pad, vertical = vpad)) {
            Column(
                Modifier.width(260.dp).fillMaxHeight().clip(RoundedCornerShape(if (s.expressive) 36.dp else 28.dp))
                    .background(cs.surfaceContainerLow).verticalScroll(rememberScrollState()).padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                    FocusBox(Modifier, CircleShape, 1.12f, onClick = onBack) { f ->
                        Box(Modifier.size(40.dp).background(if (f) cs.primary else cs.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Назад", "Back"), tint = if (f) cs.onPrimary else cs.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(tr("Настройки", "Settings"), color = cs.onSurface, style = MaterialTheme.typography.titleLarge)
                }
                cats.forEachIndexed { i, name ->
                    FocusBox(Modifier.fillMaxWidth(), CircleShape, 1.0f, onClick = { cat = i }, onFocusChange = { if (it) cat = i }) { f ->
                        Text(
                            name,
                            color = when { f -> cs.onPrimary; i == cat -> cs.onSecondaryContainer; else -> cs.onSurfaceVariant },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.fillMaxWidth()
                                .background(when { f -> cs.primary; i == cat -> cs.secondaryContainer; else -> androidx.compose.ui.graphics.Color.Transparent })
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.weight(1f))
                Image(
                    painterResource(R.drawable.tanix_logo), null,
                    Modifier.padding(top = 16.dp, start = 6.dp).height(18.dp).aspectRatio(720f / 177f).alpha(0.5f),
                    colorFilter = ColorFilter.tint(cs.onSurface)
                )
            }

            Spacer(Modifier.width(24.dp))

            AnimatedContent(
                targetState = cat,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                transitionSpec = { fadeIn(tween(ms, delayMillis = ms / 3)) togetherWith fadeOut(tween(ms / 2)) },
                label = "settingsPane"
            ) { c ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
                    Text(cats[c], color = cs.onSurface, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                    when (c) {
                        0 -> AppearancePane(vm)
                        1 -> HomePane(vm)
                        2 -> IconsPane(vm)
                        3 -> WatchPane(vm)
                        4 -> AnimationsPane(vm)
                        5 -> WeatherTimePane(vm)
                        6 -> AppsPane(vm)
                        7 -> SystemPane(vm)
                        else -> AboutPane(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearancePane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection(tr("Язык", "Language")) { LanguageBlock(vm) }
    SettingsSection(tr("Тема и цвета", "Theme and colors")) {
        ThemeBlock(vm)
        SwitchRow(tr("AMOLED: чисто чёрный фон", "AMOLED: pure black background"), tr("Для тёмной темы", "For the dark theme"), s.amoled) { v -> vm.update { it.copy(amoled = v) } }
        SwitchRow(
            tr("Material 3 Expressive (имитация)", "Material 3 Expressive (imitation)"),
            tr("Пружинные анимации, крупные скругления, жирные шрифты", "Springy animations, bigger corners, bolder type"), s.expressive
        ) { v -> vm.update { it.copy(expressive = v) } }
        SwitchRow(
            tr("Fake Liquid Glass", "Fake Liquid Glass"),
            tr("Полупрозрачные поверхности с бликами и обводкой", "Translucent surfaces with shine and borders"), s.liquidGlass
        ) { v -> vm.update { it.copy(liquidGlass = v) } }
    }
    SettingsSection(tr("Размеры и формы", "Sizes and shapes")) {
        OptionRow(tr("Масштаб интерфейса", "Interface scale"), listOf("85%", "92%", "100%", "110%", "120%"), s.uiScale) { v -> vm.update { it.copy(uiScale = v) } }
        OptionRow(
            tr("Размер шрифта", "Font size"),
            listOf(tr("Мелкий", "Small"), tr("Обычный", "Normal"), tr("Крупный", "Large")), s.fontScale
        ) { v -> vm.update { it.copy(fontScale = v) } }
        OptionRow(
            tr("Скругление карточек", "Card corners"),
            listOf(tr("Малое", "Small"), tr("Среднее", "Medium"), tr("Большое", "Large")), s.cornerStyle
        ) { v -> vm.update { it.copy(cornerStyle = v) } }
        SwitchRow(
            tr("Поля экрана", "Screen margins"),
            tr("Включите, если телевизор обрезает края; выключено — на весь экран", "Turn on if your TV crops the edges; off — edge to edge"),
            s.safeArea
        ) { v -> vm.update { it.copy(safeArea = v) } }
        if (s.safeArea) {
            OptionRow(tr("Размер полей", "Margin size"), listOf("24", "40", "56", "72"), s.screenPadding) { v -> vm.update { it.copy(screenPadding = v) } }
        }
    }
    SettingsSection(tr("Фон", "Background")) {
        OptionRow(
            tr("Стиль фона", "Background style"),
            listOf(tr("Градиент", "Gradient"), tr("Однотонный", "Solid"), tr("Как в Google TV", "Like Google TV")), s.bgStyle
        ) { v -> vm.update { it.copy(bgStyle = v) } }
        OptionRow(
            tr("Затемнение фона", "Background dimming"),
            listOf(tr("Слабое", "Light"), tr("Среднее", "Medium"), tr("Сильное", "Strong")), s.bgDim
        ) { v -> vm.update { it.copy(bgDim = v) } }
    }
}

@Composable
private fun HomePane(vm: HomeViewModel) {
    val s = vm.s
    val cs = MaterialTheme.colorScheme
    val ru = s.lang != 1
    var showChannels by remember { mutableStateOf(false) }
    SettingsSection(tr("Шапка", "Top bar")) {
        SwitchRow(tr("Логотип Tanix", "Tanix logo"), null, s.showLogo) { v -> vm.update { it.copy(showLogo = v) } }
        OptionRow(tr("Прозрачность логотипа", "Logo opacity"), listOf("35%", "55%", "75%", "100%"), s.logoAlpha) { v -> vm.update { it.copy(logoAlpha = v) } }
        SwitchRow(tr("Вкладки «Для вас / Приложения / Магазин»", "Tabs: For you / Apps / Store"), null, s.showTabs) { v -> vm.update { it.copy(showTabs = v) } }
        SwitchRow(
            tr("Переключать вкладки при наведении", "Switch tabs on focus"),
            tr("Как в Google TV; иначе — по нажатию OK", "Like Google TV; otherwise on OK"), s.tabOnFocus
        ) { v -> vm.update { it.copy(tabOnFocus = v) } }
    }
    SettingsSection(tr("Что показывать на вкладке «Для вас»", "What to show on the For you tab")) {
        SwitchRow(tr("Продолжить просмотр", "Continue watching"), null, s.showWatch) { v -> vm.update { it.copy(showWatch = v) } }
        SwitchRow(tr("Ряд «Ваши приложения»", "Your apps row"), null, s.showAppsRow) { v -> vm.update { it.copy(showAppsRow = v) } }
        SwitchRow(
            tr("Ряд «Избранное»", "Favorites row"),
            tr("Долгое нажатие OK на приложении → «В избранное»", "Long press OK on an app → Add to favorites"), s.showFavRow
        ) { v -> vm.update { it.copy(showFavRow = v) } }
        SwitchRow(tr("Ряд «Недавние»", "Recent row"), null, s.showRecentRow) { v -> vm.update { it.copy(showRecentRow = v) } }
    }
    SettingsSection(tr("Порядок рядов", "Row order")) {
        s.rowOrder.forEachIndexed { i, rk ->
            key(rk) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rowTitle(rk, ru), color = cs.onSurface, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Chip("▲") { vm.moveRow(i, -1) }
                    Spacer(Modifier.width(8.dp))
                    Chip("▼") { vm.moveRow(i, 1) }
                }
            }
        }
        SettingRow(
            tr("Настроить каналы", "Customize channels"),
            tr("Включить/выключить ряды и поменять порядок, как в Android TV", "Turn rows on/off and reorder, like Android TV"),
            onClick = { showChannels = true }
        )
    }
    if (showChannels) ChannelsDialog(vm) { showChannels = false }
}

@Composable
private fun IconsPane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection(tr("Плитки приложений", "App tiles")) { IconsBlock(vm) }
    SettingsSection(tr("Порядок", "Order")) {
        OptionRow(
            tr("Сортировка (вкладка «Приложения»)", "Sorting (Apps tab)"),
            listOf(tr("По алфавиту", "Alphabetical"), tr("Свой порядок", "Custom"), tr("Недавние сверху", "Recent first")), s.sortMode
        ) { v -> vm.update { it.copy(sortMode = v) } }
        SettingRow(tr("Сбросить порядок", "Reset order"), tr("Вернуть сортировку по алфавиту", "Back to alphabetical"), onClick = { vm.resetOrder() })
    }
}

@Composable
private fun WatchPane(vm: HomeViewModel) {
    val s = vm.s
    var showApps by remember { mutableStateOf(false) }
    SettingsSection(tr("Продолжить просмотр", "Continue watching")) {
        SwitchRow(tr("Показывать «Продолжить просмотр»", "Show Continue watching"), null, s.showWatch) { v -> vm.update { it.copy(showWatch = v) } }
        SettingRow(
            tr("Приложения для «Продолжить просмотр»", "Apps for Continue watching"),
            tr("Выбрано: ", "Chosen: ") + s.cwApps.size + tr(" · недавно открытые — первыми", " · recently opened first"),
            onClick = { showApps = true }
        )
        OptionRow(
            tr("Размер карточек", "Card size"),
            listOf(tr("Малые", "Small"), tr("Средние", "Medium"), tr("Большие", "Large")), s.posterSize
        ) { v -> vm.update { it.copy(posterSize = v) } }
        SwitchRow(tr("Подпись «Продолжить просмотр» над названием", "“Continue watching” label above the title"), null, s.posterBadge) { v -> vm.update { it.copy(posterBadge = v) } }
        SwitchRow(tr("Время последнего запуска в большом блоке", "Last-opened time in the big block"), null, s.posterDesc) { v -> vm.update { it.copy(posterDesc = v) } }
        SettingRow(tr("Очистить историю запусков", "Clear launch history"), null, onClick = { vm.clearOpened() })
    }
    if (showApps) WatchAppsDialog(vm) { showApps = false }
}

@Composable
private fun AnimationsPane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection(tr("Пресеты", "Presets")) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Chip(tr("Как в Google TV", "Like Google TV")) {
                vm.update {
                    it.copy(animOn = true, animSpeed = 1, focusK = 2, focusStyle = 0, enterAnim = true,
                        glow = true, shine = true, kenBurns = true, bgStyle = 2, launchAnim = true, liquidGlass = true)
                }
            }
            Chip(tr("Лёгкий режим (слабые приставки)", "Light mode (weak devices)")) {
                vm.update {
                    it.copy(animOn = false, enterAnim = false, glow = false, shine = false, kenBurns = false, bgStyle = 0, focusK = 1, launchAnim = false, liquidGlass = false)
                }
            }
        }
    }
    SettingsSection(tr("Анимации", "Animations")) {
        SwitchRow(tr("Анимации включены", "Animations on"), null, s.animOn) { v -> vm.update { it.copy(animOn = v) } }
        OptionRow(tr("Скорость", "Speed"), listOf(tr("Быстро", "Fast"), tr("Обычно", "Normal"), tr("Плавно", "Smooth")), s.animSpeed) { v -> vm.update { it.copy(animSpeed = v) } }
        SwitchRow(tr("Появление рядов каскадом", "Cascading rows"), null, s.enterAnim) { v -> vm.update { it.copy(enterAnim = v) } }
        SwitchRow(
            tr("Приложение раскрывается на весь экран", "App expands to full screen"),
            tr("При запуске плитка растёт на весь экран, при закрытии — сворачивается обратно", "The tile grows to full screen on launch and shrinks back when closed"),
            s.launchAnim
        ) { v -> vm.update { it.copy(launchAnim = v) } }
    }
    SettingsSection(tr("Эффекты", "Effects")) {
        SwitchRow(tr("Свечение вокруг плитки при фокусе", "Glow around the focused tile"), null, s.glow) { v -> vm.update { it.copy(glow = v) } }
        SwitchRow(tr("Блик, пробегающий по плитке", "Shine sweeping across the tile"), null, s.shine) { v -> vm.update { it.copy(shine = v) } }
        SwitchRow(
            tr("Медленный наезд фона", "Slow background zoom"),
            tr("Эффект Кена Бернса; на слабых приставках лучше выключить", "Ken Burns effect; better off on weak devices"), s.kenBurns
        ) { v -> vm.update { it.copy(kenBurns = v) } }
    }
    SettingsSection(tr("Фокус и контур", "Focus and outline")) {
        OptionRow(
            tr("Выделение", "Highlight"),
            listOf(
                tr("Контур + увеличение", "Outline + zoom"), tr("Только увеличение", "Zoom only"),
                tr("Только контур", "Outline only"), tr("Плавный контур (без увеличения)", "Gliding outline (no zoom)")
            ), s.focusStyle
        ) { v -> vm.update { it.copy(focusStyle = v) } }
        OptionRow(
            tr("Увеличение при фокусе", "Focus zoom"),
            listOf(tr("Нет", "None"), tr("Малое", "Small"), tr("Обычное", "Normal"), tr("Сильное", "Strong")), s.focusK
        ) { v -> vm.update { it.copy(focusK = v) } }
        OptionRow(
            tr("Толщина контура", "Outline thickness"),
            listOf(tr("Тонкий", "Thin"), tr("Средний", "Medium"), tr("Толстый", "Thick")), s.ringWidth
        ) { v -> vm.update { it.copy(ringWidth = v) } }
    }
}

@Composable
private fun WeatherTimePane(vm: HomeViewModel) {
    val s = vm.s
    SettingsSection(tr("Погода (Open-Meteo)", "Weather (Open-Meteo)")) {
        SwitchRow(tr("Показывать погоду", "Show weather"), null, s.showWeather) { v -> vm.update { it.copy(showWeather = v) } }
        WeatherBlock(vm)
        SwitchRow(tr("Описание (ясно, дождь…)", "Description (clear, rain…)"), null, s.weatherDesc) { v -> vm.update { it.copy(weatherDesc = v) } }
        SwitchRow(tr("Ощущаемая температура", "Feels-like temperature"), null, s.weatherFeels) { v -> vm.update { it.copy(weatherFeels = v) } }
    }
    SettingsSection(tr("Время", "Time")) {
        SwitchRow(tr("Часы", "Clock"), null, s.showClock) { v -> vm.update { it.copy(showClock = v) } }
        SwitchRow(tr("24-часовой формат", "24-hour format"), null, s.clock24) { v -> vm.update { it.copy(clock24 = v) } }
        SwitchRow(tr("Дата", "Date"), null, s.showDate) { v -> vm.update { it.copy(showDate = v) } }
    }
}

@Composable
private fun AppsPane(vm: HomeViewModel) {
    val s = vm.s
    var showTv by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    var showHome by remember { mutableStateOf(false) }
    var showReorder by remember { mutableStateOf(false) }
    SettingsSection(tr("Приложения на главном экране", "Apps on the home screen")) {
        SettingRow(
            tr("Выбрать приложения", "Choose apps"),
            tr("Сейчас: ", "Now: ") + vm.homeAppList.size + tr(" (рекомендуем около 15)", " (about 15 recommended)"),
            onClick = { showHome = true }
        )
        SettingRow(tr("Порядок приложений", "App order"), tr("Переставить значки кнопками ▲ ▼", "Reorder icons with ▲ ▼"), onClick = { showReorder = true })
        SettingRow(tr("Сбросить к автоматическому списку", "Reset to the automatic list"), tr("Первые 15 приложений", "First 15 apps"), onClick = {
            vm.update { it.copy(homeConfigured = false, homeApps = emptyList()) }
        })
    }
    SettingsSection(tr("Кнопка ТВ", "TV button")) {
        SettingRow(
            tr("Приложение для кнопки ТВ", "App for the TV button"),
            vm.apps.firstOrNull { it.pkg == s.tvPkg }?.label ?: tr("Не выбрано (Lampa, ILTV…)", "Not set (Lampa, ILTV…)"),
            onClick = { showTv = true }
        )
    }
    SettingsSection(tr("Скрытые приложения", "Hidden apps")) {
        SettingRow(tr("Выбрать скрытые", "Choose hidden apps"), tr("Скрыто: ", "Hidden: ") + s.hidden.size, onClick = { showHidden = true })
        SettingRow(tr("Показать все", "Show all"), null, onClick = { vm.update { it.copy(hidden = emptySet()) } })
    }
    if (showHome) HomeAppsDialog(vm) { showHome = false }
    if (showReorder) ReorderAppsDialog(vm) { showReorder = false }
    if (showTv) {
        AppListDialog(tr("Приложение для кнопки ТВ", "App for the TV button"), vm.orderedApps, onClick = { app ->
            vm.update { it.copy(tvPkg = app.pkg) }; showTv = false
        }, onDismiss = { showTv = false })
    }
    if (showHidden) {
        AppListDialog(tr("Скрытые приложения", "Hidden apps"), vm.orderedApps, checked = s.hidden, onClick = { app ->
            vm.update { it.copy(hidden = if (app.pkg in it.hidden) it.hidden - app.pkg else it.hidden + app.pkg) }
        }, onDismiss = { showHidden = false })
    }
}

@Composable
private fun SystemPane(vm: HomeViewModel) {
    val ctx = rememberCtx()
    var confirmReset by remember { mutableStateOf(false) }
    SettingsSection(tr("Устройство", "Device")) {
        SettingRow(
            tr("Настройки устройства", "Device settings"),
            tr("Откроются системные настройки Android", "Opens the Android system settings"),
            onClick = { vm.openDeviceSettings() }
        )
    }
    SettingsSection(tr("Главный экран", "Home screen")) {
        SettingRow(tr("Сделать Tanix главным экраном", "Make Tanix the home screen"), tr("Откроются системные настройки главного эк