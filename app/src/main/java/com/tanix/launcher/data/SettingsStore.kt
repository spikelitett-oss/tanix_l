package com.tanix.launcher.data

import android.content.Context

class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tanix", Context.MODE_PRIVATE)

    private fun list(k: String): List<String> = (sp.getString(k, "") ?: "").split(",").filter { it.isNotBlank() }

    private fun openedMap(): Map<String, Long> {
        val m = LinkedHashMap<String, Long>()
        (sp.getString("opened", "") ?: "").split(";").forEach { e ->
            val p = e.split("=")
            if (p.size == 2) p[1].toLongOrNull()?.let { m[p[0]] = it }
        }
        return m
    }

    fun load(): LauncherSettings {
        val d = LauncherSettings()
        fun b(k: String, def: Boolean) = sp.getBoolean(k, def)
        fun i(k: String, def: Int) = sp.getInt(k, def)
        fun t(k: String, def: String) = sp.getString(k, def) ?: def
        return LauncherSettings(
            onboarded = b("onboarded", d.onboarded),
            lang = i("lang", d.lang),
            themeMode = i("themeMode", d.themeMode),
            dynamicColor = b("dynamicColor", d.dynamicColor),
            accentHue = sp.getFloat("accentHue", d.accentHue),
            amoled = b("amoled", d.amoled),
            expressive = b("expressive", d.expressive),
            uiScale = i("uiScale", d.uiScale),
            fontScale = i("fontScale", d.fontScale),
            cornerStyle = i("cornerStyle", d.cornerStyle),
            safeArea = b("safeArea", d.safeArea),
            screenPadding = i("screenPadding", d.screenPadding),
            bgStyle = i("bgStyle", d.bgStyle),
            bgDim = i("bgDim", d.bgDim),
            showLogo = b("showLogo", d.showLogo),
            logoAlpha = i("logoAlpha", d.logoAlpha),
            showTabs = b("showTabs", d.showTabs),
            tabOnFocus = b("tabOnFocus", d.tabOnFocus),
            showWeather = b("showWeather", d.showWeather),
            weatherDesc = b("weatherDesc", d.weatherDesc),
            weatherFeels = b("weatherFeels", d.weatherFeels),
            showClock = b("showClock", d.showClock),
            clock24 = b("clock24", d.clock24),
            showDate = b("showDate", d.showDate),
            showWatch = b("showWatch", d.showWatch),
            showAppsRow = b("showAppsRow", d.showAppsRow),
            showFavRow = b("showFavRow", d.showFavRow),
            showRecentRow = b("showRecentRow", d.showRecentRow),
            rowOrder = normalizeRows(list("rowOrder")),
            tabOrder = list("tabOrder").mapNotNull { it.toIntOrNull() }.ifEmpty { DefaultTabOrder },
            posterSize = i("posterSize", d.posterSize),
            posterDesc = b("posterDesc", d.posterDesc),
            posterBadge = b("posterBadge", d.posterBadge),
            cwApps = list("cwApps"),
            opened = openedMap(),
            iconSize = i("iconSize", d.iconSize),
            sortMode = i("sortMode", d.sortMode),
            tileStyle = i("tileStyle", d.tileStyle),
            labelMode = i("labelMode", d.labelMode),
            animOn = b("animOn", d.animOn),
            animSpeed = i("animSpeed", d.animSpeed),
            focusK = i("focusK", d.focusK),
            focusStyle = i("focusStyle", d.focusStyle),
            enterAnim = b("enterAnim", d.enterAnim),
            glow = b("glow", d.glow),
            shine = b("shine", d.shine),
            kenBurns = b("kenBurns", d.kenBurns),
            launchAnim = b("launchAnim", d.launchAnim),
            ringWidth = i("ringWidth", d.ringWidth),
            ringColor = i("ringColor", d.ringColor),
            city = t("city", d.city),
            lat = sp.getString("lat", null)?.toDoubleOrNull() ?: d.lat,
            lon = sp.getString("lon", null)?.toDoubleOrNull() ?: d.lon,
            fahrenheit = b("fahrenheit", d.fahrenheit),
            hidden = HashSet(sp.getStringSet("hidden", emptySet()) ?: emptySet()),
            favorites = HashSet(sp.getStringSet("favorites", emptySet()) ?: emptySet()),
            recents = list("recents"),
            appOrder = list("appOrder"),
            homeConfigured = b("homeConfigured", d.homeConfigured),
            homeApps = list("homeApps"),
            tvScale = i("tvScale", d.tvScale),
            tvWhiteLogoBg = b("tvWhiteLogoBg", d.tvWhiteLogoBg),
            lastTvName = t("lastTvName", d.lastTvName),
            lastTvLogo = t("lastTvLogo", d.lastTvLogo),
            lastTvUrl = t("lastTvUrl", d.lastTvUrl),
            lastTvGroup = t("lastTvGroup", d.lastTvGroup)
        )
    }

    fun save(s: LauncherSettings) {
        val e = sp.edit()
        e.putBoolean("onboarded", s.onboarded)
        e.putInt("lang", s.lang)
        e.putInt("themeMode", s.themeMode)
        e.putBoolean("dynamicColor", s.dynamicColor)
        e.putFloat("accentHue", s.accentHue)
        e.putBoolean("amoled", s.amoled)
        e.putBoolean("expressive", s.expressive)
        e.putInt("uiScale", s.uiScale)
        e.putInt("fontScale", s.fontScale)
        e.putInt("cornerStyle", s.cornerStyle)
        e.putBoolean("safeArea", s.safeArea)
        e.putInt("screenPadding", s.screenPadding)
        e.putInt("bgStyle", s.bgStyle)
        e.putInt("bgDim", s.bgDim)
        e.putBoolean("showLogo", s.showLogo)
        e.putInt("logoAlpha", s.logoAlpha)
        e.putBoolean("showTabs", s.showTabs)
        e.putBoolean("tabOnFocus", s.tabOnFocus)
        e.putBoolean("showWeather", s.showWeather)
        e.putBoolean("weatherDesc", s.weatherDesc)
        e.putBoolean("weatherFeels", s.weatherFeels)
        e.putBoolean("showClock", s.showClock)
        e.putBoolean("clock24", s.clock24)
        e.putBoolean("showDate", s.showDate)
        e.putBoolean("showWatch", s.showWatch)
        e.putBoolean("showAppsRow", s.showAppsRow)
        e.putBoolean("showFavRow", s.showFavRow)
        e.putBoolean("showRecentRow", s.showRecentRow)
        e.putString("rowOrder", s.rowOrder.joinToString(","))
        e.putString("tabOrder", s.tabOrder.joinToString(","))
        e.putInt("posterSize", s.posterSize)
        e.putBoolean("posterDesc", s.posterDesc)
        e.putBoolean("posterBadge", s.posterBadge)
        e.putString("cwApps", s.cwApps.joinToString(","))
        e.putString("opened", s.opened.entries.joinToString(";") { it.key + "=" + it.value })
        e.putInt("iconSize", s.iconSize)
        e.putInt("sortMode", s.sortMode)
        e.putInt("tileStyle", s.tileStyle)
        e.putInt("labelMode", s.labelMode)
        e.putBoolean("animOn", s.animOn)
        e.putInt("animSpeed", s.animSpeed)
        e.putInt("focusK", s.focusK)
        e.putInt("focusStyle", s.focusStyle)
        e.putBoolean("enterAnim", s.enterAnim)
        e.putBoolean("glow", s.glow)
        e.putBoolean("shine", s.shine)
        e.putBoolean("kenBurns", s.kenBurns)
        e.putBoolean("launchAnim", s.launchAnim)
        e.putInt("ringWidth", s.ringWidth)
        e.putInt("ringColor", s.ringColor)
        e.putString("city", s.city)
        e.putString("lat", s.lat.toString())
        e.putString("lon", s.lon.toString())
        e.putBoolean("fahrenheit", s.fahrenheit)
        e.putStringSet("hidden", HashSet(s.hidden))
        e.putStringSet("favorites", HashSet(s.favorites))
        e.putString("recents", s.recents.joinToString(","))
        e.putString("appOrder", s.appOrder.joinToString(","))
        e.putBoolean("homeConfigured", s.homeConfigured)
        e.putString("homeApps", s.homeApps.joinToString(","))
        e.putInt("tvScale", s.tvScale)
        e.putBoolean("tvWhiteLogoBg", s.tvWhiteLogoBg)
        e.putString("lastTvName", s.lastTvName)
        e.putString("lastTvLogo", s.lastTvLogo)
        e.putString("lastTvUrl", s.lastTvUrl)
        e.putString("lastTvGroup", s.lastTvGroup)
        e.apply()
    }
}
