package com.example.widget

class TodosWidgetProvider : PixiWidgetProvider() {
    override val kind = WidgetKind.TASKS
    override val openAction = WidgetActions.ACTION_OPEN_TASKS
    override val actionAction = WidgetActions.ACTION_ADD_TASK
}
