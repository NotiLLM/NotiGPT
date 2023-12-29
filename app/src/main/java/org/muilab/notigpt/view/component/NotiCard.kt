package org.muilab.notigpt.view.component

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.muilab.notigpt.R
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.service.NotiListenerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiCard(context: Context, notiUnit: NotiUnit) {
    val bitmap = notiUnit.getBitmap()
    val largeBitmap = notiUnit.getLargeBitmap()

    var expansionProgress by remember { mutableFloatStateOf(0f) }
    var collapsed by remember { mutableStateOf(true) }
    var maxContentHeight by remember { mutableFloatStateOf(0f) }
    var requiresExpansion by remember { mutableStateOf(notiUnit.content.size > 1)}

    val coroutineScope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newProgress = (expansionProgress + delta / maxContentHeight).coerceIn(0f, 1f)
            expansionProgress = newProgress.coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(expansionProgress) {
        collapsed = expansionProgress == 0f
    }

    Card(
        modifier = Modifier
            .padding(vertical = 1.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = { NotiListenerService.getPendingIntent(context, notiUnit)?.send() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
            Spacer(modifier = Modifier.size(5.dp))
            if (bitmap != null)
                Column {
                    Spacer(modifier = Modifier.size(5.dp))
                    if (expansionProgress > 0f && largeBitmap != null) {
                        Image(
                            largeBitmap.asImageBitmap(),
                            "Notification Icon",
                            Modifier
                                .size(35.dp)
                                .padding(horizontal = 5.dp)
                        )
                        largeBitmap.asImageBitmap()
                    } else {
                        Icon(
                            bitmap.asImageBitmap(),
                            "Notification Icon",
                            Modifier
                                .size(35.dp)
                                .padding(horizontal = 5.dp)
                        )
                    }
                }
            Column {
                Row {
                    Column(
                        Modifier
                            .padding(horizontal = 5.dp)
                            .weight(1f)) {
                        Row {
                            Text(text = notiUnit.appName, fontSize = 14.sp)
                            Spacer(Modifier.padding(5.dp))
                            Text(text = notiUnit.getLastTime(), fontSize = 14.sp)
                        }
                        Row {
                            Text(
                                modifier = Modifier
                                    .background(Color.Transparent),
                                text = notiUnit.title.last(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp,
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
                                    },
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (requiresExpansion) {
                        Icon(
                            painter = if (collapsed)
                                painterResource(R.drawable.expand_more)
                            else
                                painterResource(R.drawable.expand_less),
                            "Expand",
                            Modifier
                                .width(30.dp)
                                .fillMaxHeight(1F)
                                .align(Alignment.CenterVertically)
                                .draggable(
                                    state = draggableState,
                                    orientation = Orientation.Vertical,
                                    onDragStopped = { velocity ->
                                        expansionProgress = if (velocity > 0) 1f else 0F
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
                                val notiContents = notiUnit.content
                                val notiTimes = if (notiUnit.`when`.last() != 0L)
                                    notiUnit.`when`
                                else
                                    notiUnit.postTime
                                notiContents.zip(notiTimes).forEach { (notiContent, notiTime) ->
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 5.dp)
                                                .background(Color.Transparent),
                                            text = notiContent,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)
                                        )
                                        Log.d("Time", notiTimes.size.toString())
                                        if (notiContents.size > 1) {
                                            Text(
                                                modifier = Modifier
                                                    .wrapContentWidth()
                                                    .background(Color.Transparent),
                                                text = notiUnit.getRelativeTimeStr(notiTime),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 14.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                                    }
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


