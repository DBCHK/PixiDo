package com.example.widget

enum class WidgetKind {
    TASKS,
    GOALS,
    HEATMAP,
    TRANSACTIONS,
    SPEND_CURVE,
    FOCUS
}

data class WidgetTheme(
    val glass: Boolean,
    val dark: Boolean
) {
    val paper: Int
        get() = when {
            glass && dark -> 0xB81C1C1E.toInt()
            glass && !dark -> 0xB8FFFFFF.toInt()
            dark -> 0xFF1C1C1E.toInt()
            else -> 0xFFF7F7F8.toInt()
        }
    val ink: Int get() = if (dark) 0xFFF5F5F7.toInt() else 0xFF111111.toInt()
    val muted: Int get() = 0xFF9B9B9F.toInt()
    val mint: Int get() = 0xFF17C492.toInt()
    val mintSoft: Int get() = if (dark) 0xFF1C3A2C.toInt() else 0xFFD4F5C4.toInt()
    val coral: Int get() = 0xFFFF5C3A.toInt()
    val spend: Int get() = 0xFFFF7A8A.toInt()
    val purple: Int get() = 0xFF7B74F6.toInt()
    val yellow: Int get() = 0xFFFFE566.toInt()
    val income: Int get() = 0xFF16A34A.toInt()
    val chip: Int get() = if (dark) 0x33FFFFFF else 0x14FFFFFF
    val hairlineTop: Int get() = if (dark) 0x38FFFFFF else 0x8FFFFFFF.toInt()
    val hairlineBot: Int get() = if (dark) 0x14FFFFFF else 0x33FFFFFF
    val highlight: Int get() = if (dark) 0x22FFFFFF else 0x55FFFFFF
    val grain: Int get() = if (dark) 0x18FFFFFF else 0x14FFFFFF
    val track: Int get() = if (dark) 0x33FFFFFF else 0x14000000
}
