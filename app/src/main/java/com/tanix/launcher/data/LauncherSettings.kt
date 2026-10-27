package com.tanix.launcher.data

val DefaultRowOrder = listOf("watch", "apps", "fav", "recent")

fun normalizeRows(l: List<String>): List<String> =
    (l.map { if (it == "posters") "watch" else it }.filter { it in DefaultRowOrder }.distinct() + DefaultRowOrder).distinct()

/** Все настройки лаунчера. Числовые значения — индексы вариантов (см. ui/Look.kt). */
data class LauncherSettings(
    val onboarded: Boolean = false,
    val lang: Int = 0,                   // 0 русский (по умолчанию), 1 English

    // ---- Оформление ----
    val themeMode: Int = 1,
    val dynamicColor: Boolean = true,
    val accentHue: Float = 250f,
    val amoled: Boolean = false,
    val expressive: Boolean = true,      // имитация Material 3 Expressive
    val liquidGlass: Boolean = true,     // фейковый Liquid Glass
    val uiScale: Int = 2,
    val fontScale: Int = 1,
    val cornerStyle: Int = 1,
    val safeArea: Boolean = true,        // поля экрана вкл/выкл
    val screenPadding: Int = 1,
    val bgStyle: Int = 2,
    val bgDim: Int = 1,

    // ---- Шапка ----
    val showLogo: Boolean = true,
    val logoAlpha: Int = 2,
    val showTabs: Boolean = true,
    val tabOnFocus: Boolean = true,
    val showWeather: Boolean = true,
    val weatherDesc: Boolean = false,
    val weatherFeels: Boolean = false,
    val showClock: Boolean = true,
    val clock24: Boolean = true,
    val showDate: Boolean = false,

    // ---- Ряды ----
    val showWatch: Boolean = true,
    val showAppsRow: Boolean = true,
    val showFavRow: Boolean = false,
    val showRecentRow: Boolean = false,
    val rowOrder: List<String> = DefaultRowOrder,

    // ---- Продолжить просмотр ----
    val posterSize: Int = 1,
    val posterDesc: Boolean = true,
    val posterBadge: Boolean = true,
    val cwApps: List<String> = emptyList(),
    val opened: Map<String, Long> = emptyMap(),

    // ---- Значки ----
    val iconSize: Int = 2,
    val sortMode: Int = 1,
    val tileStyle: Int = 1, // 0 баннеры, 1 круг (по умолчанию как в Google TV)
    val labelMode: Int = 1,

    // ---- Анимации ----
    val animOn: Boolean = true,
    val animSpeed: Int = 1,
    val focusK: Int = 2,
    val focusStyle: Int = 0,             
    val enterAnim: Boolean = true,
    val glow: Boolean = true,
    val shine: Boolean = true,
    val kenBurns: Boolean = true,
    val launchAnim: Boolean = true,      
    val ringWidth: Int = 1,
    val ringColor: Int = 0,

    // ---- Погода ----
    val city: String = "Москва",
    val lat: Double = 55.7558,
    val lon: Double = 37.6176,
    val fahrenheit: Boolean = false,

    // ---- Приложения ----
    val tvPkg: String? = null,
    val hidden: Set<String> = emptySet(),
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val appOrder: List<String> = emptyList(),
    val homeConfigured: Boolean = false,
    val homeApps: List<String> = emptyList()
)