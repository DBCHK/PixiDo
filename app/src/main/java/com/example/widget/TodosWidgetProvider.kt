package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodosWidgetProvider : AppWidgetProvider() {

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
        private val lineIds = listOf(
            R.id.todo_line_1,
            R.id.todo_line_2,
            R.id.todo_line_3,
            R.id.todo_line_4,
            R.id.todo_line_5
        )
        private val checkIds = listOf(
            R.id.todo_check_1,
            R.id.todo_check_2,
            R.id.todo_check_3,
            R.id.todo_check_4,
            R.id.todo_check_5
        )

        suspend fun update(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val data = runCatching { WidgetDataLoader.loadTodos(context) }.getOrNull()
            val views = RemoteViews(context.packageName, R.layout.widget_todos)

            views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetActions.openApp(context, WidgetActions.ACTION_OPEN_TASKS, 200 + widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.btn_quick_add,
                WidgetActions.openApp(context, WidgetActions.ACTION_ADD_TASK, 250 + widgetId)
            )

            if (data != null) {
                views.setTextViewText(R.id.todo_date, data.todayLabel)
                views.setTextViewText(
                    R.id.todo_stats,
                    "${data.doneCount}/${data.totalCount} done · ${data.openCount} open"
                )
                lineIds.forEachIndexed { i, id ->
                    val item = data.titles.getOrNull(i)
                    if (item != null) {
                        views.setViewVisibility(id, View.VISIBLE)
                        views.setViewVisibility(checkIds[i], View.VISIBLE)
                        val mark = if (item.second) "✓" else "○"
                        views.setTextViewText(checkIds[i], mark)
                        views.setTextViewText(id, item.first)
                    } else {
                        views.setViewVisibility(id, View.GONE)
                        views.setViewVisibility(checkIds[i], View.GONE)
                    }
                }
                if (data.titles.isEmpty()) {
                    views.setViewVisibility(R.id.todo_line_1, View.VISIBLE)
                    views.setViewVisibility(R.id.todo_check_1, View.GONE)
                    views.setTextViewText(R.id.todo_line_1, "No todos yet — tap + to add")
                }
            } else {
                views.setTextViewText(R.id.todo_date, "Today")
                views.setTextViewText(R.id.todo_stats, "Open PixiDo")
            }
            mgr.updateAppWidget(widgetId, views)
        }
    }
}
