package com.theveloper.pixelplay.ui.glancewidget.sizing

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WidgetSize {
    TINY, // 1x1
    SMALL, // 2x2
    MEDIUM, // 3x3
    LARGE, // 4x4
    EXTRA_LARGE // 5x5
}

fun getWidgetSize(size: DpSize): WidgetSize {
    return when {
        size.width < 100.dp && size.height < 100.dp -> WidgetSize.TINY
        size.width < 180.dp && size.height < 180.dp -> WidgetSize.SMALL
        size.width < 250.dp && size.height < 250.dp -> WidgetSize.MEDIUM
        size.width < 320.dp && size.height < 320.dp -> WidgetSize.LARGE
        else -> WidgetSize.EXTRA_LARGE
    }
}
