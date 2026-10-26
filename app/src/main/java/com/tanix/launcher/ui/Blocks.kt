package com.tanix.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.Repo

// Общие блоки настроек: используются и в экране настроек, и в первом запуске.

@Composable
fun LanguageBlock(vm: HomeViewModel) {
    OptionRow("Язык / Language", listOf("Русский", "English"), vm.s.lang) { v -> vm.update { it.copy(lang = v) } }
}

@Composable
fun ThemeBlock(vm: HomeViewModel) {
    val s = vm.s
    OptionRow(
        tr("Тема", "Theme"),
        listOf(tr("Системная", "System"), tr("Тёмная", "Dark"), tr("Светлая", "Light")), s.themeMode
    ) { v -> vm.update { it.copy(themeMode = v) } }
    val dynOk = android.os.Build.VERSION.SDK_INT >= 31
    SwitchRow(
        tr("Динамические цвета Material You", "Material You dynamic colors"),
        if (dynOk) tr("Цвета подбираются из обоев", "Colors are taken from the wallpaper")
        else tr("Нужен Android 12+ — на этой приставке работает выбранный акцент", "Needs Android 12+ — the chosen accent is used on this device"),
        checked = s.dynamicColor && dynOk
    ) { v -> if (dynOk) vm.update { it.copy(dynamicColor = v) } }
    Text(tr("Акцентный цвет", "Accent color"), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp))
    AccentPicker(s.accentHue) { h -> vm.update { it.copy(accentHue = h, dynamicColor = false) } }
}

@Composable
fun IconsBlock(vm: HomeViewModel) {
    val s = vm.s
    OptionRow(
        tr("Вид плиток приложений", "App tile style"),
        listOf(
            tr("Баннеры (как в Google TV)", "Banners (like Google TV)"), tr("Круг", "Circle"),
            tr("Скруглённый квадрат", "Rounded square"), tr("Квадрат", "Square")
        ), s.tileStyle
    ) { v -> vm.update { it.copy(tileStyle = v) } }
    OptionRow(
        tr("Размер плиток", "Tile size"),
        listOf(tr("Мелкие", "Small"), tr("Средние", "Medium"), tr("Крупные", "Large"), tr("Огромные", "Huge")), s.iconSize
    ) { v -> vm.update { it.copy(iconSize = v) } }
    OptionRow(
        tr("Подписи", "Labels"),
        listOf(tr("Всегда", "Always"), tr("При фокусе", "On focus"), tr("Скрыть", "Hidden")), s.labelMode
    ) { v -> vm.update { it.copy(labelMode = v) } }
}

@Composable
fun WeatherBlock(vm: HomeViewModel) {
    val s = vm.s
    var showCity by remember { mutableStateOf(false) }
    SettingRow(tr("Город", "City"), s.city, onClick = { showCity = true })
    OptionRow(tr("Единицы температуры", "Temperature units"), listOf("°C", "°F"), if (s.fahrenheit) 1 else 0) { v -> vm.update { it.copy(fahrenheit = v == 1) } }
    SettingRow(
        tr("Обновить сейчас", "Refresh now"),
        vm.weatherError?.let { tr("Ошибка: ", "Error: ") + it }
            ?: vm.weather?.let { Repo.weatherDesc(it.code, s.lang == 1) + (if (s.lang == 1) ", feels like " else ", ощущается как ") + it.feels.toInt() + "°" },
        onClick = { vm.loadWeather() }
    )
    if (showCity) CityDialog({ showCity = false }) { c ->
        vm.update { it.copy(city = c.name, lat = c.lat, lon = c.lon) }
        showCity = false
    }
}

fun openHomeSettings(ctx: Context) {
    for (a in listOf(android.provider.Settings.ACTION_HOME_SETTINGS, android.provider.Settings.ACTION_SETTINGS)) {
        try { ctx.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) { }
    }
}

@Composable
fun rememberCtx(): Context = LocalContext.current
