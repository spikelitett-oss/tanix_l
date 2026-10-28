package com.tanix.launcher

import android.app.ActivityOptions
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tanix.launcher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LaunchAnim(
    val app: AppItem,
    val rect: Rect,
    val corner: Float,
    val expanded: Boolean = true,
    val launched: Boolean = false
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)

    var s by mutableStateOf(store.load()); private set
    var apps by mutableStateOf(listOf<AppItem>()); private set
    var weather by mutableStateOf<Weather?>(null); private set
    var weatherError by mutableStateOf<String?>(null); private set
    var homeSignal by mutableIntStateOf(0); private set
    var launching by mutableStateOf<LaunchAnim?>(null); private set

    val orderedApps: List<AppItem> get() = Repo.orderApps(apps, s)
    val homeAppList: List<AppItem> get() = Repo.homeApps(apps, s)
    val watchApps: List<AppItem>
        get() {
            val chosen = s.cwApps.mapNotNull { p -> apps.firstOrNull { it.pkg == p } }
            return chosen.sortedByDescending { s.opened[it.pkg] ?: 0L }
        }

    init {
        viewModelScope.launch {
            while (true) {
                val ok = loadWeatherNow()
                delay(if (ok) 30 * 60_000L else 30_000L)
            }
        }
    }

    fun onHomePressed() { homeSignal++; onResumed() }

    fun update(f: (LauncherSettings) -> LauncherSettings) {
        val old = s
        val new = f(old)
        s = new
        store.save(new)
        if (old.lat != new.lat || old.lon != new.lon || old.fahrenheit != new.fahrenheit) loadWeather()
    }

    fun resetAppearance() = update {
        LauncherSettings(
            onboarded = true, lang = it.lang, city = it.city, lat = it.lat, lon = it.lon,
            favorites = it.favorites, hidden = it.hidden, recents = it.recents,
            homeConfigured = it.homeConfigured, homeApps = it.homeApps, cwApps = it.cwApps,
            opened = it.opened, appOrder = it.appOrder
        )
    }

    fun freezeOrder() = update { it.copy(appOrder = Repo.orderApps(apps, it).map { a -> a.pkg }, sortMode = 1) }
    fun resetOrder() = update { it.copy(appOrder = emptyList(), sortMode = 0) }

    fun moveApp(pkg: String, delta: Int, subset: List<String>) {
        val i = subset.indexOf(pkg)
        if (i < 0) return
        val j = (i + delta).coerceIn(0, subset.lastIndex)
        if (i == j) return
        val reordered = subset.toMutableList().apply { removeAt(i); add(j, pkg) }
        val g = Repo.orderApps(apps, s).map { it.pkg }.toMutableList()
        val set = subset.toSet()
        val slots = g.indices.filter { g[it] in set }
        if (slots.size != reordered.size) return
        slots.forEachIndexed { k, slot -> g[slot] = reordered[k] }
        update { it.copy(appOrder = g, sortMode = 1) }
    }

    fun toggleHome(pkg: String) = update {
        val cur = Repo.homeApps(apps, it).map { a -> a.pkg }
        it.copy(homeConfigured = true, homeApps = if (pkg in cur) cur - pkg else cur + pkg)
    }

    fun moveHome(pkg: String, delta: Int) = update {
        val cur = Repo.homeApps(apps, it).map { a -> a.pkg }.toMutableList()
        val i = cur.indexOf(pkg)
        if (i < 0) return@update it
        val j = (i + delta).coerceIn(0, cur.lastIndex)
        if (i != j) { cur.removeAt(i); cur.add(j, pkg) }
        it.copy(homeConfigured = true, homeApps = cur)
    }

    fun moveRow(i: Int, d: Int) = update {
        val l = it.rowOrder.toMutableList()
        val j = (i + d).coerceIn(0, l.lastIndex)
        if (i != j && i in l.indices) { val x = l.removeAt(i); l.add(j, x) }
        it.copy(rowOrder = l)
    }

    fun setRowOn(key: String, on: Boolean) = update {
        when (key) {
            "watch" -> it.copy(showWatch = on)
            "apps" -> it.copy(showAppsRow = on)
            "fav" -> it.copy(showFavRow = on)
            else -> it.copy(showRecentRow = on)
        }
    }

    fun toggleWatch(pkg: String) = update { it.copy(cwApps = if (pkg in it.cwApps) it.cwApps - pkg else it.cwApps + pkg) }
    fun clearOpened() = update { it.copy(opened = emptyMap()) }

    fun refreshApps() {
        viewModelScope.launch { apps = withContext(Dispatchers.IO) { Repo.loadApps(getApplication()) } }
    }

    fun loadWeather() { viewModelScope.launch { loadWeatherNow() } }

    private suspend fun loadWeatherNow(): Boolean {
        val snap = s
        val r = withContext(Dispatchers.IO) { Repo.loadWeather(snap) }
        r.onSuccess { weather = it; weatherError = null }
        r.onFailure { weatherError = it.message ?: "error" }
        return r.isSuccess
    }

    fun toggleFavorite(pkg: String) = update { it.copy(favorites = if (pkg in it.favorites) it.favorites - pkg else it.favorites + pkg) }

    fun launch(pkg: String, noAnim: Boolean = false): Boolean {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val ok = runCatching {
            if (noAnim) ctx.startActivity(intent, ActivityOptions.makeCustomAnimation(ctx, 0, 0).toBundle())
            else ctx.startActivity(intent)
        }.isSuccess
        if (ok) {
            val now = System.currentTimeMillis()
            update {
                it.copy(
                    recents = (listOf(pkg) + it.recents.filter { r -> r != pkg }).take(10),
                    opened = it.opened + (pkg to now)
                )
            }
        }
        return ok
    }

    fun startLaunch(app: AppItem, rect: Rect, corner: Float) {
        if (!s.animOn || !s.launchAnim || launching != null || rect.width < 2f || rect.height < 2f) {
            launch(app.pkg); return
        }
        launching = LaunchAnim(app, rect, corner)
    }

    fun launchNow() {
        val la = launching ?: return
        if (la.launched) return
        launching = if (launch(la.app.pkg, noAnim = true)) la.copy(launched = true)
        else la.copy(expanded = false, launched = true)
    }

    fun onResumed() {
        val la = launching ?: return
        if (la.launched && la.expanded) launching = la.copy(expanded = false)
    }

    fun clearLaunch() { launching = null }

    fun openAppInfo(pkg: String) = startSafe(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    fun uninstall(pkg: String) = startSafe(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))

    fun openDeviceSettings() {
        if (!startSafe(Intent(android.provider.Settings.ACTION_SETTINGS))) {
            startSafe(Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS))
        }
    }

    fun openStore(query: String? = null, details: String? = null) {
        if (query == null && details == null) {
            if (!launch("com.android.vending")) startSafe(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps")))
            return
        }
        val market = if (details != null) "market://details?id=$details" else "market://search?q=${Uri.encode(query)}&c=apps"
        val web = if (details != null) "https://play.google.com/store/apps/details?id=$details" else "https://play.google.com/store/search?q=${Uri.encode(query)}&c=apps"
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(market)).setPackage("com.android.vending")
        if (!startSafe(i)) startSafe(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
    }

    private fun startSafe(i: Intent): Boolean =
        try { getApplication<Application>().startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (_: Exception) { false }
}
