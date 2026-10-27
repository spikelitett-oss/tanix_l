package com.tanix.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherIcon(code: Int, size: Dp, tint: Color? = null) {
    Canvas(Modifier.size(size)) { drawWeather(code, tint) }
}

private fun DrawScope.sun(c: Offset, r: Float, rays: Boolean, tint: Color? = null) {
    if (tint != null) {
        if (rays) {
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                drawLine(
                    tint, c + Offset(dx, dy) * (r * 1.32f), c + Offset(dx, dy) * (r * 1.72f),
                    strokeWidth = r * 0.22f, cap = StrokeCap.Round
                )
            }
        }
        drawCircle(tint, radius = r, center = c)
        return
    }

    drawCircle(
        Brush.radialGradient(listOf(Color(0x66FFC107), Color.Transparent), center = c, radius = r * 2.2f),
        radius = r * 2.2f, center = c
    )
    if (rays) {
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(
                Color(0xFFFFB300), c + Offset(dx, dy) * (r * 1.32f), c + Offset(dx, dy) * (r * 1.72f),
                strokeWidth = r * 0.22f, cap = StrokeCap.Round
            )
        }
    }
    drawCircle(
        Brush.radialGradient(
            0f to Color(0xFFFFF9C4), 0.5f to Color(0xFFFFCA28), 1f to Color(0xFFFF8F00),
            center = c + Offset(-r * 0.3f, -r * 0.3f), radius = r * 1.6f
        ),
        radius = r, center = c
    )
    drawCircle(Color.White.copy(alpha = 0.4f), radius = r * 0.26f, center = c + Offset(-r * 0.38f, -r * 0.42f))
}

private fun DrawScope.cloudParts(left: Float, top: Float, w: Float, h: Float, fill: (Offset) -> Unit, rr: (Offset, Size, Float) -> Unit) {
    val baseTop = top + h * 0.5f
    rr(Offset(left, baseTop), Size(w, top + h - baseTop), (top + h - baseTop) / 2f)
    fill(Offset(left + w * 0.30f, top + h * 0.55f))
    fill(Offset(left + w * 0.58f, top + h * 0.42f))
}

private fun DrawScope.cloud(left: Float, top: Float, w: Float, h: Float, light: Color, dark: Color) {
    val rSmall = h * 0.28f
    val rBig = h * 0.42f
    val shadow = Color(0x33000000)
    val so = Offset(0f, h * 0.07f)
    cloudParts(left, top, w, h,
        fill = { c -> drawCircle(shadow, if (c.x < left + w * 0.45f) rSmall else rBig, c + so) },
        rr = { o, s, r -> drawRoundRect(shadow, o + so, s, CornerRadius(r)) })
    val brush = Brush.verticalGradient(listOf(light, dark), startY = top, endY = top + h)
    cloudParts(left, top, w, h,
        fill = { c -> drawCircle(brush, if (c.x < left + w * 0.45f) rSmall else rBig, c) },
        rr = { o, s, r -> drawRoundRect(brush, o, s, CornerRadius(r)) })
    val cx = left + w * 0.58f
    val cy = top + h * 0.42f
    drawArc(
        Color.White.copy(alpha = 0.65f), startAngle = 200f, sweepAngle = 65f, useCenter = false,
        topLeft = Offset(cx - rBig * 0.84f, cy - rBig * 0.84f), size = Size(rBig * 1.68f, rBig * 1.68f),
        style = Stroke(h * 0.05f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drop(x: Float, y: Float, w: Float, h: Float) {
    val p = Path().apply {
        moveTo(x, y - h * 0.5f)
        cubicTo(x + w * 0.6f, y - h * 0.05f, x + w * 0.55f, y + h * 0.5f, x, y + h * 0.5f)
        cubicTo(x - w * 0.55f, y + h * 0.5f, x - w * 0.6f, y - h * 0.05f, x, y - h * 0.5f)
        close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFF1E88E5)), startY = y - h * 0.5f, endY = y + h * 0.5f))
    drawCircle(Color.White.copy(alpha = 0.55f), radius = w * 0.14f, center = Offset(x - w * 0.18f, y + h * 0.05f))
}

private fun DrawScope.flake(x: Float, y: Float, r: Float) {
    drawCircle(
        Brush.radialGradient(listOf(Color.White, Color(0xFFB3E5FC)), center = Offset(x, y), radius = r),
        radius = r, center = Offset(x, y)
    )
    for (i in 0 until 3) {
        val a = Math.toRadians(i * 60.0)
        val d = Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * 1.7f)
        drawLine(Color.White.copy(alpha = 0.9f), Offset(x, y) - d, Offset(x, y) + d, strokeWidth = r * 0.35f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.bolt(x: Float, y: Float, w: Float, h: Float) {
    fun pt(px: Float, py: Float) = Offset(x + px * w, y + py * h)
    val p = Path().apply {
        moveTo(pt(0.58f, 0f).x, pt(0.58f, 0f).y)
        lineTo(pt(0.18f, 0.58f).x, pt(0.18f, 0.58f).y)
        lineTo(pt(0.46f, 0.58f).x, pt(0.46f, 0.58f).y)
        lineTo(pt(0.32f, 1f).x, pt(0.32f, 1f).y)
        lineTo(pt(0.84f, 0.38f).x, pt(0.84f, 0.38f).y)
        lineTo(pt(0.55f, 0.38f).x, pt(0.55f, 0.38f).y)
        lineTo(pt(0.76f, 0f).x, pt(0.76f, 0f).y)
        close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFF176), Color(0xFFFF9800)), startY = y, endY = y + h))
    drawPath(p, Color(0xFFFFE082), style = Stroke(w * 0.04f))
}

private fun DrawScope.drawWeather(code: Int, tint: Color? = null) {
    val s = size.minDimension
    val lightCloud = Color(0xFFFFFFFF)
    val midCloud = Color(0xFFB0BEC5)
    val darkLight = Color(0xFFB0BEC5)
    val darkCloud = Color(0xFF607D8B)
    when (code) {
        0 -> sun(Offset(s * 0.5f, s * 0.5f), s * 0.24f, true, tint)
        1 -> {
            sun(Offset(s * 0.42f, s * 0.40f), s * 0.2f, true, tint)
            cloud(s * 0.30f, s * 0.46f, s * 0.64f, s * 0.40f, lightCloud, Color(0xFFCFD8DC))
        }
        2 -> {
            sun(Offset(s * 0.36f, s * 0.34f), s * 0.19f, true, tint)
            cloud(s * 0.2f, s * 0.38f, s * 0.72f, s * 0.46f, lightCloud, midCloud)
        }
        45, 48 -> {
            cloud(s * 0.14f, s * 0.16f, s * 0.72f, s * 0.44f, lightCloud, midCloud)
            for (i in 0 until 3) {
                val y = s * (0.68f + i * 0.1f)
                drawLine(Color(0xFFCFD8DC), Offset(s * (0.2f + i * 0.04f), y), Offset(s * (0.8f - i * 0.04f), y),
                    strokeWidth = s * 0.055f, cap = StrokeCap.Round)
            }
        }
        in 51..57, in 61..67, in 80..82 -> {
            val heavy = code >= 63 || code >= 80
            cloud(s * 0.12f, s * 0.10f, s * 0.76f, s * 0.48f, Color(0xFFECEFF1), darkLight)
            drop(s * 0.32f, s * 0.78f, s * 0.13f, s * 0.2f)
            drop(s * 0.52f, s * 0.84f, s * 0.13f, s * 0.2f)
            if (heavy) drop(s * 0.72f, s * 0.77f, s * 0.13f, s * 0.2f)
        }
        in 71..77, 85, 86 -> {
            cloud(s * 0.12f, s * 0.10f, s * 0.76f, s * 0.48f, Color(0xFFECEFF1), Color(0xFFB0BEC5))
            flake(s * 0.30f, s * 0.78f, s * 0.045f)
            flake(s * 0.52f, s * 0.86f, s * 0.045f)
            flake(s * 0.72f, s * 0.76f, s * 0.045f)
        }
        in 95..99 -> {
            cloud(s * 0.12f, s * 0.08f, s * 0.76f, s * 0.48f, darkLight, darkCloud)
            bolt(s * 0.34f, s * 0.50f, s * 0.34f, s * 0.46f)
            drop(s * 0.26f, s * 0.82f, s * 0.1f, s * 0.16f)
            drop(s * 0.76f, s * 0.80f, s * 0.1f, s * 0.16f)
        }
        else -> cloud(s * 0.12f, s * 0.24f, s * 0.76f, s * 0.5f, lightCloud, midCloud)
    }
}
