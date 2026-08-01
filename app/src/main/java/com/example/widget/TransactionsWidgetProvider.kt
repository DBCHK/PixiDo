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

class TransactionsWidgetProvider : AppWidgetProvider() {

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
            val data = runCatching { WidgetDataLoader.loadTransactions(context) }.getOrNull()
            val views = RemoteViews(context.packageName, R.layout.widget_transactions)

            views.setOnClickPendingIntent(
                R.id.widget_root,
                WidgetActions.openApp(context, WidgetActions.ACTION_OPEN_BUDGET, 400 + widgetId)
            )

            if (data != null) {
                views.setTextViewText(R.id.txn_period, data.periodLabel)
                views.setTextViewText(
                    R.id.txn_income,
                    "+${WidgetDataLoader.formatMoney(data.income, data.currencyCode)}"
                )
                views.setTextViewText(
                    R.id.txn_spent,
                    "−${WidgetDataLoader.formatMoney(data.spent, data.currencyCode)}"
                )
                val netSign = if (data.net >= 0) "+" else "−"
                views.setTextViewText(
                    R.id.txn_net,
                    "$netSign${WidgetDataLoader.formatMoney(kotlin.math.abs(data.net), data.currencyCode)}"
                )
                views.setTextViewText(
                    R.id.txn_stats,
                    "${data.txnCount} txns · top ${data.topCategory}"
                )
            } else {
                views.setTextViewText(R.id.txn_period, "This month")
                views.setTextViewText(R.id.txn_stats, "Open PixiDo")
            }
            mgr.updateAppWidget(widgetId, views)
        }
    }
}
