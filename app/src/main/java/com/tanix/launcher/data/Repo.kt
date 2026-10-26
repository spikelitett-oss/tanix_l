package com.tanix.launcher.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

object Repo {

    // ---------- Приложения ----------
    fun loadApps(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val result = LinkedHashMap<String, AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName || result.containsKey(pkg)) continue
                runCatching {
                    val d = ri.loadIcon(pm)
                    // Баннер 16:9, который приложение предоставляет для Android TV (как плитки Netflix, YouTube…)
                    val bannerDrawable = runCatching {
                        ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
                    }.getOrNull()
                    val banner = bannerDrawable?.let { b -> runCatching { b.toBitmap(320, 180).asImageBitmap() }.getOrNull() }
                    result[pkg] = AppItem(
                        ri.loadLabel(pm).toString(), pkg,
                        d.toBitmap(216, 216).asImageBitmap(), d is AdaptiveIconDrawable, banner
                    )
                }
            }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    /** Порядок приложений по выбранному режиму сортировки. */
    fun orderApps(apps: List<AppItem>, s: LauncherSettings): List<AppItem> = when (s.sortMode) {
        1 -> {
            val idx = HashMap<String, Int>()
            s.appOrder.forEachIndexed { i, p -> idx[p] = i }
            apps.sortedWith(compareBy<AppItem> { idx[it.pkg] ?: Int.MAX_VALUE }.thenBy { it.label.lowercase() })
        }
        2 -> {
            val r = s.recents
            apps.sortedWith(compareBy<AppItem> { r.indexOf(it.pkg).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it.label.lowercase() })
        }
        else -> apps.sortedBy { it.label.lowercase() }
    }

    /** Приложения на главном экране: выбранные вручную или (пока не настроено) первые 15. */
    fun homeApps(apps: List<AppItem>, s: LauncherSettings): List<AppItem> {
        val visible = orderApps(apps, s).filter { it.pkg !in s.hidden }
        return if (s.homeConfigured) s.homeApps.mapNotNull { p -> visible.firstOrNull { it.pkg == p } } else visible.take(15)
    }

    // ---------- Погода ----------
    fun searchCity(q: String, en: Boolean = false): List<City> {
        if (q.isBlank()) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(q, "UTF-8")}&count=6&language=${if (en) "en" else "ru"}&format=json"
        val arr = JSONObject(Net.get(url)).optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            City(
                o.optString("name"),
                listOf(o.optString("admin1"), o.optString("country")).filter { s -> s.isNotBlank() }.joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude")
            )
        }
    }

    /** Сначала Open-Meteo, при ошибке — MET Norway. В ошибке указано, что именно не сработало. */
    fun loadWeather(s: LauncherSettings): Result<Weather> {
        val a = runCatching { openMeteo(s) }
        if (a.isSuccess) return a
        val b = runCatching { metNo(s) }
        if (b.isSuccess) return b
        return Result.failure(
            Exception("Open-Meteo: ${a.exceptionOrNull()?.message ?: "?"}; MET.no: ${b.exceptionOrNull()?.message ?: "?"}")
        )
    }

    private fun openMeteo(s: LauncherSettings): Weather {
        val unit = if (s.fahrenheit) "&temperature_unit=fahrenheit" else ""
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${s.lat}&longitude=${s.lon}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m&wind_speed_unit=ms&timezone=auto$unit"
        val c = JSONObject(Net.get(url)).getJSONObject("current")
        return Weather(c.getDouble("temperature_2m"), c.getDouble("apparent_temperature"), c.getInt("weather_code"), c.getDouble("wind_speed_10m"))
    }

    private fun metNo(s: LauncherSettings): Weather {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" +
            String.format(Locale.US, "%.4f", s.lat) + "&lon=" + String.format(Locale.US, "%.4f", s.lon)
        val data = JSONObject(Net.get(url, "TanixLauncher/1.0 github.com/tanix-launcher"))
            .getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val d = data.getJSONObject("instant").getJSONObject("details")
        var t = d.getDouble("air_temperature")
        val wind = d.optDouble("wind_speed", 0.0)
        val sym = data.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: data.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: "cloudy"
        if (s.fahrenheit) t = t * 9.0 / 5.0 + 32.0
        return Weather(t, t, symbolToCode(sym), wind)
    }

    private fun symbolToCode(sym: String): Int {
        val b = sym.substringBefore('_')
        return when {
            b.startsWith("clearsky") -> 0
            b.startsWith("fair") -> 1
            b.startsWith("partlycloudy") -> 2
            b == "cloudy" -> 3
            b.contains("fog") -> 45
            b.contains("thunder") -> 95
            b.contains("snow") || b.contains("sleet") -> 73
            b.contains("lightrain") -> 61
            b.contains("rain") -> 63
            else -> 3
        }
    }

    fun weatherDesc(code: Int, en: Boolean): String {
        val ru = when (code) {
            0 -> "Ясно"
            1 -> "Малооблачно"
            2 -> "Облачно с прояснениями"
            3 -> "Пасмурно"
            45, 48 -> "Туман"
            in 51..57 -> "Морось"
            in 61..67 -> "Дождь"
            in 71..77 -> "Снег"
            in 80..82 -> "Ливень"
            85, 86 -> "Снегопад"
            in 95..99 -> "Гроза"
            else -> "—"
        }
        if (!en) return ru
        return when (code) {
            0 -> "Clear"
            1 -> "Mostly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            in 51..57 -> "Drizzle"
            in 61..67 -> "Rain"
            in 71..77 -> "Snow"
            in 80..82 -> "Showers"
            85, 86 -> "Snowfall"
            in 95..99 -> "Thunderstorm"
            else -> "—"
        }
    }
}
