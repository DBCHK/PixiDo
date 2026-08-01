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

class ContributionWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            scope.launch {
                update(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        suspend fun update(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val data = runCatching { WidgetDataLoader.loadHeatmap(context) }.getOrNull()
            val views = RemoteViews(context.packageName, R.layout.widget_contribution)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetActions.openApp(context, WidgetActions.ACTION_OPEN_TASKS, 100 + widgetId)
            )
            if (data != null) {
                views.setImageViewBitmap(R.id.widget_heatmap_image, data.bitmap)
                views.setTextViewText(
                    R.id.widget_heatmap_stats,
                    "${data.totalCompletions} done · ${data.streak}d streak · today ${data.todayCount}"
                )
                views.setTextViewText(
                    R.id.widget_heatmap_sub,
                    "${data.activeDays} active days · best ${data.bestDay}/day"
                )
            } else {
                views.setTextViewText(R.id.widget_heatmap_stats, "Activity")
                views.setTextViewText(R.id.widget_heatmap_sub, "Open PixiDo to sync")
            }
            mgr.updateAppWidget(widgetId, views)
        }
    }
}
