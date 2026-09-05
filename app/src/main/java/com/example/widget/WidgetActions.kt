package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.MainActivity

object WidgetActions {
    const val EXTRA_ACTION = "pixido_widget_action"
    const val ACTION_OPEN_APP = "open_app"
    const val ACTION_ADD_TASK = "add_task"
    const val ACTION_OPEN_FOCUS = "open_focus"
    const val ACTION_OPEN_TASKS = "open_tasks"
    const val ACTION_OPEN_BUDGET = "open_budget"
    const val ACTION_OPEN_GOALS = "open_goals"

    fun openApp(context: Context, action: String = ACTION_OPEN_APP, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ACTION, action)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val app = context.applicationContext
        listOf(
            ContributionWidgetProvider::class.java,
            TodosWidgetProvider::class.java,
            FocusWidgetProvider::class.java,
            TransactionsWidgetProvider::class.java,
            GoalsWidgetProvider::class.java,
            SpendCurveWidgetProvider::class.java
        ).forEach { cls ->
            val ids = mgr.getAppWidgetIds(ComponentName(app, cls))
            if (ids.isNotEmpty()) {
                val intent = Intent(app, cls).apply {
                    this.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                app.sendBroadcast(intent)
            }
        }
    }
}
