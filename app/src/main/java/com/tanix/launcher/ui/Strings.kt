package com.tanix.launcher.ui

import androidx.compose.runtime.Composable

/** Двуязычные строки: русский (по умолчанию) и английский, выбирается в настройках. */
@Composable
fun tr(ru: String, en: String): String = if (LocalSettings.current.lang == 1) en else ru

fun agoText(ts: Long, en: Boolean): String {
    val m = (System.currentTimeMillis() - ts) / 60_000L
    return when {
        m < 1 -> if (en) "Just now" else "Только что"
        m < 60 -> if (en) "$m min ago" else "$m мин. назад"
        m < 60 * 24 -> if (en) "${m / 60} h ago" else "${m / 60} ч. назад"
        else -> if (en) "${m / 1440} d ago" else "${m / 1440} дн. назад"
    }
}
