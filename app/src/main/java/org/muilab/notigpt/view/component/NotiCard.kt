package org.muilab.notigpt.view.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.muilab.notigpt.R
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.service.NotiListenerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiCard(context: Context, notiUnit: NotiUnit) {
    val bitmap = notiUnit.getBitmap()

    var expansionProgress by remember { mutableFloatStateOf(0f) }
    var maxContentHeight by remember { mutableFloatStateOf(0f) }
    var requiresExpansion by remember { mutableStateOf(notiUnit.content.size > 1)}

    val coroutineScope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newProgress = (expansionProgress + delta / maxContentHeight).coerceIn(0f, 1f)
            expansionProgress = newProgress.coerceIn(0f, 1f)
        }
    }

    Card(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = { NotiListenerService.getPendingIntent(context, notiUnit)?.send() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.size(5.dp))
            if (bitmap != null)
                Column {
                    Spacer(modifier = Modifier.size(5.dp))
                    Icon(
                        bitmap.asImageBitmap(),
                        "Notification Icon",
                        Modifier
                            .size(40.dp)
                            .padding(horizontal = 5.dp)
                    )
                }
            Column {
                Row {
                    Column(
                        Modifier
                            .padding(horizontal = 5.dp)
                            .weight(1f)) {
                        Row {
                            Text(text = notiUnit.appName)
                            Spacer(Modifier.padding(5.dp))
                            Text(text = notiUnit.getLastTime())
                        }
                        Row {
                            Text(
                                modifier = Modifier
                                    .background(Color.Transparent),
                                text = notiUnit.title.last(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { textLayoutResult ->
                                    if (textLayoutResult.hasVisualOverflow)
                                        requiresExpansion = true
                                }
                            )
                            if (expansionProgress == 0f) {
                                Spacer(Modifier.padding(5.dp))
                                Text(
                                    modifier = Modifier.background(Color.Transparent),
                                    text = notiUnit.content.last(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.hasVisualOverflow)
                                            requiresExpansion = true
                                    }
                                )
                            }
                        }
                    }

                    if (requiresExpansion) {
                        Icon(
                            painter = if (expansionProgress > 0f)
                                painterResource(R.drawable.expand_less)
                            else
                                painterResource(R.drawable.expand_more),
                            "Expand",
                            Modifier
                                .width(30.dp)
                                .fillMaxHeight(1F)
                                .align(Alignment.CenterVertically)
                                .draggable(
                                    state = draggableState,
                                    orientation = Orientation.Vertical,
                                    onDragStopped = {
                                        expansionProgress = if (expansionProgress > 0.3f) 1f else 0F
                                    }
                                )
                                .clickable {
                                    expansionProgress = if (expansionProgress > 0f) 0f else 1F
                                }
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                    }
                }
                if (requiresExpansion)
                    SubcomposeLayout(Modifier.clipToBounds()) { constraints ->
                        val content = subcompose("content") {

                            Divider(
                                color = Color.White,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 5.dp)
                            )

                            Column {
                                notiUnit.content.forEach { notiContent ->
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 5.dp)
                                            .background(Color.Transparent),
                                        text = notiContent
                                    )
                                }
                            }
                        }

                        val contentMeasures = content.map { it.measure(constraints) }
                        maxContentHeight = contentMeasures.maxOf { it.height }.toFloat()
                        val currentHeight = (expansionProgress * maxContentHeight).toInt()
                        layout(constraints.maxWidth, currentHeight) {
                            val yOffset = currentHeight - maxContentHeight.toInt()  // Position content at the bottom
                            contentMeasures.forEach { it.place(0, yOffset) }
                        }
                    }
            }
        }
    }
}


