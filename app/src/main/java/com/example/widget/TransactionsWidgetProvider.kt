package com.example.widget

class TransactionsWidgetProvider : PixiWidgetProvider() {
    override val kind = WidgetKind.TRANSACTIONS
    override val openAction = WidgetActions.ACTION_OPEN_BUDGET
}
