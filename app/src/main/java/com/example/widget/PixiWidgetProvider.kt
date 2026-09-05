package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class PixiWidgetProvider : AppWidgetProvider() {

    abstract val kind: WidgetKind
    abstract val openAction: String
    open val actionAction: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            scope.launch { push(context.applicationContext, appWidgetManager, id) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        scope.launch { push(context.applicationContext, appWidgetManager, appWidgetId) }
    }

    private suspend fun push(context: Context, mgr: AppWidgetManager, widgetId: Int) {
        val snapshot = runCatching { loadSnapshot(context, kind) }.getOrNull()
        val theme = currentTheme(context)
        val (w, h) = widgetSizePx(context, mgr, widgetId)
        val bmp = WidgetArt.render(context, kind, snapshot ?: dummy(kind), w, h, theme)
        val views = RemoteViews(context.packageName, R.layout.widget_canvas)
        views.setImageViewBitmap(R.id.widget_art, bmp)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            WidgetActions.openApp(context, openAction, kind.ordinal * 100 + widgetId)
        )
        val extra = actionAction
        if (extra != null) {
            views.setViewVisibility(R.id.widget_action, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_action,
                WidgetActions.openApp(context, extra, kind.ordinal * 100 + 50 + widgetId)
            )
        } else {
            views.setViewVisibility(R.id.widget_action, View.GONE)
        }
        withContext(Dispatchers.Main.immediate) {
            mgr.updateAppWidget(widgetId, views)
        }
    }

    companion object {
        suspend fun loadSnapshot(context: Context, kind: WidgetKind): WidgetSnapshot {
            return when (kind) {
                WidgetKind.TASKS -> WidgetDataLoader.loadTodos(context).let {
                    WidgetSnapshot.Tasks(it.todayLabel, it.openCount, it.doneCount, it.totalCount, it.titles)
                }
                WidgetKind.GOALS -> WidgetDataLoader.loadGoals(context).let {
                    WidgetSnapshot.Goals(it.habitsDone, it.habitsTotal, it.streak, it.progress, it.habitNames)
                }
                WidgetKind.HEATMAP -> WidgetDataLoader.loadHeatmap(context).let {
                    WidgetSnapshot.Heatmap(it.totalCompletions, it.streak, it.todayCount, it.dayCounts)
                }
                WidgetKind.TRANSACTIONS -> WidgetDataLoader.loadTransactions(context).let {
                    WidgetSnapshot.Transactions(it.currencyCode, it.income, it.spent, it.net, it.periodLabel)
                }
                WidgetKind.SPEND_CURVE -> WidgetDataLoader.loadSpendCurve(context).let {
                    WidgetSnapshot.SpendCurve(it.currencyCode, it.total, it.periodLabel, it.normalized, it.labels)
                }
                WidgetKind.FOCUS -> WidgetDataLoader.loadFocus(context).let {
                    WidgetSnapshot.Focus(it.label, it.todayCompletions)
                }
            }
        }

        suspend fun currentTheme(context: Context): WidgetTheme {
            val glass = runCatching {
                UserPreferencesRepository(context).currentProfile().glassEffectEnabled
            }.getOrDefault(true)
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            return WidgetTheme(glass = glass, dark = night)
        }

        fun widgetSizePx(
            context: Context,
            mgr: AppWidgetManager,
            widgetId: Int
        ): Pair<Int, Int> {
            val opts = mgr.getAppWidgetOptions(widgetId)
            val density = context.resources.displayMetrics.density
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH).takeIf { it > 40 } ?: 250
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).takeIf { it > 40 } ?: 110
            val w = (wDp * density).toInt().coerceIn(240, 1400)
            val h = (hDp * density).toInt().coerceIn(160, 900)
            return w to h
        }

        fun dummy(kind: WidgetKind): WidgetSnapshot = when (kind) {
            WidgetKind.TASKS -> WidgetSnapshot.Tasks("Today", 0, 0, 0, emptyList())
            WidgetKind.GOALS -> WidgetSnapshot.Goals(0, 0, 0, 0f, emptyList())
            WidgetKind.HEATMAP -> WidgetSnapshot.Heatmap(0, 0, 0, emptyMap())
            WidgetKind.TRANSACTIONS -> WidgetSnapshot.Transactions("USD", 0.0, 0.0, 0.0, "This month")
            WidgetKind.SPEND_CURVE -> WidgetSnapshot.SpendCurve("USD", 0.0, "This week", emptyList(), emptyList())
            WidgetKind.FOCUS -> WidgetSnapshot.Focus("Focus · 25 min", 0)
        }
    }
}
