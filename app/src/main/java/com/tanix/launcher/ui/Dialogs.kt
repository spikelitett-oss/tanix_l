package com.tanix.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tanix.launcher.HomeViewModel
import com.tanix.launcher.data.AppItem
import com.tanix.launcher.data.City
import com.tanix.launcher.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DialogCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(24.dp),
        content = content
    )
}

@Composable
fun DialogTitle(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(14.dp))
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(title)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(tr("Отмена", "Cancel"), onClick = onDismiss)
                Chip(confirm, selected = true, onClick = onConfirm)
            }
        }
    }
}

@Composable
fun TextInputDialog(title: String, hint: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { fr.requestFocus() } }
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(title)
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true, label = { Text(hint) },
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave(text) })
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(tr("Отмена", "Cancel"), onClick = onDismiss)
                Chip(tr("Сохранить", "Save"), selected = true) { onSave(text) }
            }
        }
    }
}

@Composable
fun AppListDialog(
    title: String, apps: List<AppItem>, checked: Set<String>? = null,
    onClick: (AppItem) -> Unit, onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(title)
            LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(apps, key = { it.pkg }) { app ->
                    FocusBox(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), 1.01f, { onClick(app) }) { f ->
                        Row(
                            Modifier.fillMaxWidth().background(if (f) cs.primaryContainer else cs.surfaceContainer).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIcon(app, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(app.label, Modifier.weight(1f), color = if (f) cs.onPrimaryContainer else cs.onSurface)
                            if (checked != null) Switch(checked = app.pkg in checked, onCheckedChange = null)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Chip(tr("Закрыть", "Close"), onClick = onDismiss)
        }
    }
}

enum class AppAction { OPEN, MOVE, HOME, WATCH, FAVORITE, HIDE, INFO, UNINSTALL }

/** Контекстное меню приложения (долгое нажатие OK). */
@Composable
fun AppMenuDialog(app: AppItem, isFav: Boolean, inHome: Boolean, inWatch: Boolean, onAction: (AppAction) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, 44.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingRow(tr("Открыть", "Open"), onClick = { onAction(AppAction.OPEN) })
            SettingRow(tr("Переместить", "Move"), tr("Стрелками на пульте, OK — готово", "Use the arrow keys, OK when done"), onClick = { onAction(AppAction.MOVE) })
            SettingRow(if (inHome) tr("Убрать с главного экрана", "Remove from home") else tr("Добавить на главный экран", "Add to home"), onClick = { onAction(AppAction.HOME) })
            SettingRow(if (inWatch) tr("Убрать из «Продолжить просмотр»", "Remove from Continue watching") else tr("В «Продолжить просмотр»", "Add to Continue watching"), onClick = { onAction(AppAction.WATCH) })
            SettingRow(if (isFav) tr("Убрать из избранного", "Remove from favorites") else tr("В избранное", "Add to favorites"), onClick = { onAction(AppAction.FAVORITE) })
            SettingRow(tr("Скрыть с главного экрана", "Hide from home"), onClick = { onAction(AppAction.HIDE) })
            SettingRow(tr("О приложении", "App info"), onClick = { onAction(AppAction.INFO) })
            SettingRow(tr("Удалить", "Uninstall"), onClick = { onAction(AppAction.UNINSTALL) })
            }
        }
    }
}

@Composable
fun CityDialog(onDismiss: () -> Unit, onPick: (City) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val en = LocalSettings.current.lang == 1
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<City>()) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { fr.requestFocus() } }

    fun search() {
        scope.launch {
            msg = if (en) "Searching…" else "Ищем…"
            results = withContext(Dispatchers.IO) { runCatching { Repo.searchCity(q, en) }.getOrDefault(emptyList()) }
            msg = if (results.isEmpty()) (if (en) "Nothing found" else "Ничего не найдено") else ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(tr("Город для погоды", "Weather city"))
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true, label = { Text(tr("Название города", "City name")) },
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() })
            )
            Spacer(Modifier.height(10.dp))
            Chip(tr("Найти", "Search"), selected = true) { search() }
            if (msg.isNotEmpty()) Text(msg, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
            results.forEach { c -> SettingRow(c.name, c.region, onClick = { onPick(c) }) }
            Spacer(Modifier.height(10.dp))
            Chip(tr("Закрыть", "Close"), onClick = onDismiss)
        }
    }
}

/** Выбор приложений для главного экрана (рекомендуем около 15). */
@Composable
fun HomeAppsDialog(vm: HomeViewModel, onDismiss: () -> Unit) {
    val chosen = vm.homeAppList.map { it.pkg }.toSet()
    AppListDialog(
        tr("На главном экране: ", "On home screen: ") + chosen.size + tr(" (рекомендуем около 15)", " (about 15 recommended)"),
        vm.orderedApps.filter { it.pkg !in vm.s.hidden },
        checked = chosen, onClick = { vm.toggleHome(it.pkg) }, onDismiss = onDismiss
    )
}

/** Выбор приложений, которые отвечают за «Продолжить просмотр». */
@Composable
fun WatchAppsDialog(vm: HomeViewModel, onDismiss: () -> Unit) {
    AppListDialog(
        tr("Приложения для «Продолжить просмотр»", "Apps for Continue watching"),
        vm.orderedApps, checked = vm.s.cwApps.toSet(), onClick = { vm.toggleWatch(it.pkg) }, onDismiss = onDismiss
    )
}

fun rowTitle(key: String, ru: Boolean): String = when (key) {
    "watch" -> if (ru) "Продолжить просмотр" else "Continue watching"
    "apps" -> if (ru) "Ваши приложения" else "Your apps"
    "fav" -> if (ru) "Избранное" else "Favorites"
    else -> if (ru) "Недавние" else "Recent"
}

fun rowOn(s: com.tanix.launcher.data.LauncherSettings, key: String): Boolean = when (key) {
    "watch" -> s.showWatch
    "apps" -> s.showAppsRow
    "fav" -> s.showFavRow
    else -> s.showRecentRow
}

/** «Настроить каналы», как в Android TV Home: включить/выключить ряды и поменять их порядок. */
@Composable
fun ChannelsDialog(vm: HomeViewModel, onDismiss: () -> Unit) {
    val s = vm.s
    val ru = s.lang != 1
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(tr("Настроить каналы", "Customize channels"))
            Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                s.rowOrder.forEachIndexed { i, rk ->
                    key(rk) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                SwitchRow(rowTitle(rk, ru), null, rowOn(s, rk)) { v -> vm.setRowOn(rk, v) }
                            }
                            Spacer(Modifier.width(8.dp))
                            Chip("▲") { vm.moveRow(i, -1) }
                            Spacer(Modifier.width(6.dp))
                            Chip("▼") { vm.moveRow(i, 1) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Chip(tr("Готово", "Done"), selected = true, onClick = onDismiss)
        }
    }
}

/** Порядок приложений в ряду «Ваши приложения»: ▲ / ▼. */
@Composable
fun ReorderAppsDialog(vm: HomeViewModel, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val list = vm.homeAppList
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            DialogTitle(tr("Порядок приложений", "App order"))
            LazyColumn(Modifier.heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(list, key = { it.pkg }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainer).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app, 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, Modifier.weight(1f), color = cs.onSurface)
                        Chip("▲") { vm.moveHome(app.pkg, -1) }
                        Spacer(Modifier.width(6.dp))
                        Chip("▼") { vm.moveHome(app.pkg, 1) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Chip(tr("Готово", "Done"), selected = true, onClick = onDismiss)
        }
    }
}
