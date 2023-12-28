package org.muilab.notigpt.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import java.text.SimpleDateFormat
import java.util.Locale

fun getDateTime(unixTime: Long): String {
    val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.TAIWAN)
    return simpleDateFormat.format(unixTime)
}

fun Modifier.matchRowSize() : Modifier {
    return layout { measurable, constraints ->
        if (constraints.maxHeight == Constraints.Infinity) {
            layout(0, 0) {}
        } else {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}