package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocusWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            scope.launch { update(context, appWidgetManager, id) }
        }
    }

    companion object {
        suspend fun update(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val data = runCatching { WidgetDataLoader.loadFocus(context) }.getOrNull()
            val views = RemoteViews(context.packageName, R.layout.widget_focus)

            views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetActions.openApp(context, WidgetActions.ACTION_OPEN_FOCUS, 300 + widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.btn_start_focus,
                WidgetActions.openApp(context, WidgetActions.ACTION_OPEN_FOCUS, 350 + widgetId)
            )

            if (data != null) {
                views.setTextViewText(R.id.focus_time, "25:00")
                views.setTextViewText(R.id.focus_label, data.label)
                views.setTextViewText(
                    R.id.focus_stats,
                    "Today ${data.todayCompletions} tasks · ${data.userXp} XP"
                )
                views.setTextViewText(
                    R.id.focus_hint,
                    "Tap Start for a calm focus session"
                )
            } else {
                views.setTextViewText(R.id.focus_time, "25:00")
                views.setTextViewText(R.id.focus_stats, "Open PixiDo")
            }
            mgr.updateAppWidget(widgetId, views)
        }
    }
}
