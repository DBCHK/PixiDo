package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.Currencies
import kotlin.math.min
import kotlin.random.Random

/**
 * Paints home-screen widgets in the same Pulse / Wallet / Goals language
 * as the app. When glass is on, panels are translucent with a hairline and
 * top specular — wallpaper shows through like iOS widgets.
 */
object WidgetArt {

    fun render(
        context: Context,
        kind: WidgetKind,
        snapshot: WidgetSnapshot,
        widthPx: Int,
        heightPx: Int,
        theme: WidgetTheme
    ): Bitmap {
        val w = widthPx.coerceIn(200, 1600)
        val h = heightPx.coerceIn(140, 1000)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fonts = WidgetFonts.from(context)
        val density = context.resources.displayMetrics.density
        val panel = RectF(0f, 0f, w.toFloat(), h.toFloat())
        drawGlassPanel(canvas, panel, 28f * density, theme)
        val pad = 16f * density
        val inner = RectF(pad, pad, w - pad, h - pad)
        when (kind) {
            WidgetKind.TASKS -> drawTasks(canvas, inner, snapshot as? WidgetSnapshot.Tasks, theme, fonts, density)
            WidgetKind.GOALS -> drawGoals(canvas, inner, snapshot as? WidgetSnapshot.Goals, theme, fonts, density)
            WidgetKind.HEATMAP -> drawHeatmap(canvas, inner, snapshot as? WidgetSnapshot.Heatmap, theme, fonts, density)
            WidgetKind.TRANSACTIONS -> drawTransactions(canvas, inner, snapshot as? WidgetSnapshot.Transactions, theme, fonts, density)
            WidgetKind.SPEND_CURVE -> drawSpendCurve(canvas, inner, snapshot as? WidgetSnapshot.SpendCurve, theme, fonts, density)
            WidgetKind.FOCUS -> drawFocus(canvas, inner, snapshot as? WidgetSnapshot.Focus, theme, fonts, density)
        }
        return bmp
    }

    private fun drawGlassPanel(canvas: Canvas, rect: RectF, radius: Float, theme: WidgetTheme) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.paper }
        canvas.drawRoundRect(rect, radius, radius, fill)
        if (theme.glass) {
            val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    rect.left,
                    rect.top,
                    rect.left,
                    rect.top + rect.height() * 0.42f,
                    intArrayOf(theme.highlight, 0x00000000),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, radius, radius, highlight)
            val specX = rect.left + rect.width() * 0.62f
            val specY = rect.top + rect.height() * 0.18f
            val specR = min(rect.width(), rect.height()) * 0.48f
            val specular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.RadialGradient(
                    specX,
                    specY,
                    specR,
                    intArrayOf(0x66FFFFFF, 0x14FFFFFF, 0x00000000),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, radius, radius, specular)
            val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
                shader = LinearGradient(
                    rect.left + rect.width() * 0.15f,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    intArrayOf(0x99FFFFFF.toInt(), 0x22FFFFFF, 0x44FFFFFF),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect.insetCopy(1.2f), radius, radius, rim)
            val rng = Random(7)
            val grain = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.grain }
            repeat(140) {
                val x = rect.left + rng.nextFloat() * rect.width()
                val y = rect.top + rng.nextFloat() * rect.height()
                canvas.drawCircle(x, y, 0.8f + rng.nextFloat(), grain)
            }
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            shader = LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                intArrayOf(theme.hairlineTop, theme.hairlineBot),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect.insetCopy(0.6f), radius, radius, stroke)
    }

    private fun drawTasks(
        canvas: Canvas,
        inner: RectF,
        data: WidgetSnapshot.Tasks?,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val titleP = text(fonts.bold, 15f * d, theme.ink)
        val mutedP = text(fonts.medium, 11f * d, theme.muted)
        val rowP = text(fonts.semibold, 13f * d, theme.ink)
        canvas.drawText("Today’s tasks", inner.left, inner.top + 16f * d, titleP)
        val sub = data?.let { "${it.doneCount}/${it.totalCount} done · ${it.openCount} open" }
            ?: "Open PixiDo"
        canvas.drawText(sub, inner.left, inner.top + 32f * d, mutedP)
        drawPlus(canvas, inner.right - 16f * d, inner.top + 14f * d, 16f * d, theme)

        val items = data?.titles.orEmpty()
        var y = inner.top + 50f * d
        val rowH = 22f * d
        if (items.isEmpty()) {
            canvas.drawText("No todos yet — tap to add", inner.left, y + 8f * d, mutedP)
            return
        }
        items.take(5).forEach { (title, done) ->
            if (y + rowH > inner.bottom) return
            drawCheck(canvas, inner.left + 7f * d, y - 4f * d, 7f * d, done, theme)
            val t = ellipsize(title, inner.width() - 28f * d, rowP)
            rowP.color = if (done) theme.muted else theme.ink
            canvas.drawText(t, inner.left + 22f * d, y, rowP)
            y += rowH
        }
        val frac = data?.let {
            if (it.totalCount == 0) 0f else it.doneCount / it.totalCount.toFloat()
        } ?: 0f
        if (y + 10f * d < inner.bottom) {
            drawBar(canvas, inner.left, inner.bottom - 8f * d, inner.width(), 6f * d, frac, theme.mint, theme.track)
        }
    }

    private fun drawGoals(
        canvas: Canvas,
        inner: RectF,
        data: WidgetSnapshot.Goals?,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val titleP = text(fonts.bold, 15f * d, theme.ink)
        val mutedP = text(fonts.medium, 11f * d, theme.muted)
        val bigP = text(fonts.bold, 22f * d, theme.ink)
        canvas.drawText("Weekly progress", inner.left, inner.top + 16f * d, titleP)
        val streak = data?.streak ?: 0
        canvas.drawText(
            if (streak > 0) "🔥 ${streak}d streak" else "Build a streak",
            inner.left,
            inner.top + 32f * d,
            mutedP
        )

        val ring = min(inner.height() * 0.62f, 72f * d)
        val cx = inner.right - ring * 0.62f
        val cy = inner.centerY() + 4f * d
        val progress = data?.progress ?: 0f
        drawRing(canvas, cx, cy, ring * 0.46f, progress, theme.mint, theme.track, 7f * d)
        val pct = "${(progress * 100).toInt()}%"
        val tw = bigP.measureText(pct)
        canvas.drawText(pct, cx - tw / 2f, cy + 8f * d, bigP)

        val done = data?.habitsDone ?: 0
        val total = data?.habitsTotal ?: 0
        canvas.drawText(
            if (total > 0) "$done/$total habits today" else "Add a habit",
            inner.left,
            inner.top + 54f * d,
            text(fonts.semibold, 13f * d, theme.ink)
        )
        var y = inner.top + 76f * d
        val chipH = 22f * d
        data?.habitNames.orEmpty().take(3).forEach { (name, checked) ->
            if (y + chipH > inner.bottom) return@forEach
            drawCheck(canvas, inner.left + 7f * d, y - 5f * d, 7f * d, checked, theme)
            val row = text(fonts.medium, 12.5f * d, if (checked) theme.muted else theme.ink)
            canvas.drawText(ellipsize(name, inner.width() * 0.55f, row), inner.left + 22f * d, y, row)
            y += chipH
        }
    }

    private fun drawHeatmap(
        canvas: Canvas,
        inner: RectF,
        data: WidgetSnapshot.Heatmap?,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val titleP = text(fonts.bold, 15f * d, theme.ink)
        val mutedP = text(fonts.medium, 11f * d, theme.muted)
        canvas.drawText("Activity", inner.left, inner.top + 16f * d, titleP)
        val stats = data?.let { "${it.totalCompletions} check-ins · ${it.streak}d streak" } ?: "Goals heatmap"
        canvas.drawText(stats, inner.left, inner.top + 32f * d, mutedP)

        val gridTop = inner.top + 44f * d
        val grid = RectF(inner.left, gridTop, inner.right, inner.bottom - 4f * d)
        val weeks = 16
        val gap = 2.4f * d / d.coerceAtLeast(1f)
        val cell = min(
            (grid.width() - (weeks - 1) * gap) / weeks,
            (grid.height() - 6 * gap) / 7f
        ).coerceIn(6f, 18f)
        val keys = WidgetDataLoader.contributionGrid(weeks)
        val counts = data?.dayCounts.orEmpty()
        val empty = if (theme.dark) 0xFF1A2420.toInt() else 0xFFEEF4F0.toInt()
        val l1 = if (theme.dark) 0xFF1E4A38.toInt() else 0xFFC8F0DC.toInt()
        val l2 = if (theme.dark) 0xFF1A8A5C.toInt() else 0xFF8EE8C0.toInt()
        val l3 = if (theme.dark) 0xFF17C492.toInt() else 0xFF4DDC9A.toInt()
        val l4 = if (theme.dark) 0xFF2ED9A5.toInt() else 0xFF2ED9A5.toInt()
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val round = cell * 0.28f
        keys.forEachIndexed { wi, week ->
            week.forEachIndexed { di, key ->
                val c = counts[key] ?: 0
                cellPaint.color = when {
                    c <= 0 -> empty
                    c == 1 -> l1
                    c == 2 -> l2
                    c <= 4 -> l3
                    else -> l4
                }
                val left = grid.left + wi * (cell + gap)
                val top = grid.top + di * (cell + gap)
                canvas.drawRoundRect(RectF(left, top, left + cell, top + cell), round, round, cellPaint)
            }
        }
    }

    private fun drawTransactions(
        canvas: Canvas,
        inner: RectF,
        data: WidgetSnapshot.Transactions?,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val titleP = text(fonts.bold, 15f * d, theme.ink)
        val mutedP = text(fonts.medium, 11f * d, theme.muted)
        val moneyP = text(fonts.bold, 26f * d, theme.ink)
        canvas.drawText("Wallet", inner.left, inner.top + 16f * d, titleP)
        canvas.drawText(data?.periodLabel ?: "This month", inner.right - 8f * d - mutedP.measureText(data?.periodLabel ?: "This month"), inner.top + 16f * d, mutedP)

        val code = data?.currencyCode ?: "USD"
        val net = data?.net ?: 0.0
        val split = Currencies.split(net, code)
        val amount = "${if (split.negative) "−" else ""}${split.symbol}${split.whole}.${split.cents}"
        canvas.drawText(amount, inner.left, inner.top + 48f * d, moneyP)
        canvas.drawText("Net this month", inner.left, inner.top + 64f * d, mutedP)

        val chipH = 36f * d
        val chipY = inner.bottom - chipH
        val gap = 8f * d
        val chipW = (inner.width() - gap) / 2f
        drawMiniStat(
            canvas,
            RectF(inner.left, chipY, inner.left + chipW, inner.bottom),
            "Income",
            "+${WidgetDataLoader.formatMoney(data?.income ?: 0.0, code)}",
            theme.income,
            theme,
            fonts,
            d
        )
        drawMiniStat(
            canvas,
            RectF(inner.left + chipW + gap, chipY, inner.right, inner.bottom),
            "Spent",
            "−${WidgetDataLoader.formatMoney(data?.spent ?: 0.0, code)}",
            theme.spend,
            theme,
            fonts,
            d
        )
    }

    private fun drawSpendCurve(
        canvas: Canvas,
        inner: RectF,
        data: WidgetSnapshot.SpendCurve?,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val titleP = text(fonts.bold, 15f * d, theme.ink)
        val mutedP = text(fonts.medium, 11f * d, theme.muted)
        val moneyP = text(fonts.bold, 22f * d, theme.ink)
        canvas.drawText("Spending", inner.left, inner.top + 16f * d, titleP)
        canvas.drawText(data?.periodLabel ?: "This week", inner.left, inner.top + 32f * d, mutedP)
        val code = data?.currencyCode ?: "USD"
        val total = data?.total ?: 0.0
        canvas.drawText(Currencies.format(total, code), inner.left, inner.top + 56f * d, moneyP)

        val chart = RectF(inner.left, inner.top + 66f * d, inner.right, inner.bottom - 14f * d)
        if (chart.height() < 24f) return
        val values = data?.normalized.orEmpty()
        if (values.size < 2) {
            canvas.drawText("Log a spend to fill the curve", inner.left, chart.centerY(), mutedP)
            return
        }
        val pts = values.mapIndexed { i, v ->
            val x = chart.left + chart.width() * (i / (values.size - 1).toFloat())
            val y = chart.bottom - chart.height() * v.coerceIn(0f, 1f)
            x to y
        }
        val line = WidgetCurve.path(pts)
        val fill = WidgetCurve.fillUnder(line, pts.first().first, pts.last().first, chart.bottom)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, chart.top, 0f, chart.bottom,
                intArrayOf(theme.spend and 0x00FFFFFF or 0x48000000, theme.spend and 0x00FFFFFF or 0x05000000),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fill, fillPaint)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.spend
            style = Paint.Style.STROKE
            strokeWidth = 3.2f * d / d.coerceAtLeast(1f) * d
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        linePaint.strokeWidth = 3.2f * d
        canvas.drawPath(line, linePaint)
        val last = pts.last()
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.spend }
        canvas.drawCircle(last.first, last.second, 4.2f * d, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        canvas.drawCircle(last.first, last.second, 3f * d, dot)
        val labels = data?.labels.orEmpty()
        if (labels.size == values.size && labels.size <= 8) {
            val lp = text(fonts.medium, 9f * d, theme.muted)
            labels.forEachIndexed { i, lab ->
                val x = pts[i].first - lp.measureText(lab) / 2f
                canvas.drawText(lab, x, inner.bottom - 1f * d, lp)
            }
        }
    }

    private fun drawFocus(
        canvas: Canvas,
        inner: RectF,
        data: WidgetSnapshot.Focus?,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val titleP = text(fonts.medium, 12f * d, theme.muted)
        val timeP = text(fonts.bold, 34f * d, theme.ink)
        val mutedP = text(fonts.medium, 11f * d, theme.muted)
        canvas.drawText(data?.label ?: "Focus · 25 min", inner.left, inner.top + 16f * d, titleP)
        canvas.drawText("25:00", inner.left, inner.top + 52f * d, timeP)
        canvas.drawText(
            data?.let { "Today ${it.todayCompletions} check-ins" } ?: "Tap Start",
            inner.left,
            inner.top + 70f * d,
            mutedP
        )
        val btn = RectF(inner.right - 88f * d, inner.centerY() - 20f * d, inner.right, inner.centerY() + 20f * d)
        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.purple }
        canvas.drawRoundRect(btn, 20f * d, 20f * d, btnPaint)
        val bp = text(fonts.bold, 13f * d, 0xFFFFFFFF.toInt())
        val label = "Start"
        canvas.drawText(label, btn.centerX() - bp.measureText(label) / 2f, btn.centerY() + 5f * d, bp)
    }

    private fun drawMiniStat(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        accent: Int,
        theme: WidgetTheme,
        fonts: WidgetFonts,
        d: Float
    ) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.chip }
        canvas.drawRoundRect(rect, 14f * d, 14f * d, bg)
        val lp = text(fonts.medium, 10f * d, theme.muted)
        val vp = text(fonts.bold, 13f * d, accent)
        canvas.drawText(label, rect.left + 10f * d, rect.top + 14f * d, lp)
        canvas.drawText(ellipsize(value, rect.width() - 16f * d, vp), rect.left + 10f * d, rect.bottom - 8f * d, vp)
    }

    private fun drawPlus(canvas: Canvas, cx: Float, cy: Float, r: Float, theme: WidgetTheme) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.yellow }
        canvas.drawCircle(cx, cy, r, bg)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            strokeWidth = r * 0.18f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx - r * 0.38f, cy, cx + r * 0.38f, cy, p)
        canvas.drawLine(cx, cy - r * 0.38f, cx, cy + r * 0.38f, p)
    }

    private fun drawCheck(canvas: Canvas, cx: Float, cy: Float, r: Float, done: Boolean, theme: WidgetTheme) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (done) theme.mint else theme.muted
            style = if (done) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        canvas.drawCircle(cx, cy, r, p)
        if (done) {
            val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                strokeWidth = 1.8f
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
            }
            canvas.drawLine(cx - r * 0.35f, cy, cx - r * 0.05f, cy + r * 0.32f, tick)
            canvas.drawLine(cx - r * 0.05f, cy + r * 0.32f, cx + r * 0.4f, cy - r * 0.32f, tick)
        }
    }

    private fun drawRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        progress: Float,
        color: Int,
        track: Int,
        stroke: Float
    ) {
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = track
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        canvas.drawArc(oval, 0f, 360f, false, tp)
        val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(oval, -90f, 360f * progress.coerceIn(0f, 1f), false, fp)
    }

    private fun drawBar(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        frac: Float,
        fill: Int,
        track: Int
    ) {
        val r = h / 2f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = track }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), r, r, bg)
        val fw = (w * frac.coerceIn(0f, 1f)).coerceAtLeast(if (frac > 0f) h else 0f)
        if (fw > 0f) {
            val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }
            canvas.drawRoundRect(RectF(x, y, x + fw, y + h), r, r, fp)
        }
    }

    private fun text(typeface: Typeface, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
    }

    private fun ellipsize(text: String, maxW: Float, paint: TextPaint): String =
        TextUtils.ellipsize(text, paint, maxW, TextUtils.TruncateAt.END).toString()

    private fun RectF.insetCopy(by: Float) = RectF(left + by, top + by, right - by, bottom - by)
}

internal class WidgetFonts(
    val regular: Typeface,
    val medium: Typeface,
    val semibold: Typeface,
    val bold: Typeface
) {
    companion object {
        fun from(context: Context): WidgetFonts {
            fun font(id: Int, fallback: Typeface) =
                runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback
            return WidgetFonts(
                regular = font(R.font.inter_regular, Typeface.DEFAULT),
                medium = font(R.font.inter_medium, Typeface.DEFAULT),
                semibold = font(R.font.inter_semibold, Typeface.DEFAULT_BOLD),
                bold = font(R.font.inter_bold, Typeface.DEFAULT_BOLD)
            )
        }
    }
}

sealed class WidgetSnapshot {
    data class Tasks(
        val todayLabel: String,
        val openCount: Int,
        val doneCount: Int,
        val totalCount: Int,
        val titles: List<Pair<String, Boolean>>
    ) : WidgetSnapshot()

    data class Goals(
        val habitsDone: Int,
        val habitsTotal: Int,
        val streak: Int,
        val progress: Float,
        val habitNames: List<Pair<String, Boolean>>
    ) : WidgetSnapshot()

    data class Heatmap(
        val totalCompletions: Int,
        val streak: Int,
        val todayCount: Int,
        val dayCounts: Map<String, Int>
    ) : WidgetSnapshot()

    data class Transactions(
        val currencyCode: String,
        val income: Double,
        val spent: Double,
        val net: Double,
        val periodLabel: String
    ) : WidgetSnapshot()

    data class SpendCurve(
        val currencyCode: String,
        val total: Double,
        val periodLabel: String,
        val normalized: List<Float>,
        val labels: List<String>
    ) : WidgetSnapshot()

    data class Focus(
        val label: String,
        val todayCompletions: Int
    ) : WidgetSnapshot()
}
