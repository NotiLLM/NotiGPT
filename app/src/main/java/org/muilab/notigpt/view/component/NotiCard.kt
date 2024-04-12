package org.muilab.notigpt.view.component

import android.content.Context
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.lerp
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
import org.muilab.notigpt.util.getDisplayTimeStr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiCard(context: Context, notiUnit: NotiUnit, isScrolledToTop: Boolean) {
    val bitmap = notiUnit.getBitmap()
    val largeBitmap = notiUnit.getLargeBitmap()
    val emergencyRed = Color(145, 25, 17)
    val GRADIENT_TOP = 27
    val GRADIENT_BOTTOM = 18
    val gradientColor = when {
        notiUnit.score > GRADIENT_TOP -> emergencyRed
        notiUnit.score > GRADIENT_BOTTOM -> lerp(
            MaterialTheme.colorScheme.surfaceVariant,
            emergencyRed,
            ((notiUnit.score - GRADIENT_BOTTOM) / (GRADIENT_TOP - GRADIENT_BOTTOM)).toFloat()
        )
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    var expansionProgress by remember { mutableFloatStateOf(0f) }
    var collapsed by remember { mutableStateOf(true) }
    var maxContentHeight by remember { mutableFloatStateOf(0f) }
    var requiresExpansion by remember { mutableStateOf(notiUnit.content.size > 1) }
    var allowDragAnywhere by remember { mutableStateOf(true) }

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
            .fillMaxWidth()
            .let { modifier ->
                 if (isScrolledToTop && requiresExpansion && allowDragAnywhere)
                     modifier.draggable(
                         state = draggableState,
                         orientation = Orientation.Vertical,
                         onDragStopped = { velocity ->
                             expansionProgress = if (velocity > 0) 1f else 0F
                             if (expansionProgress == 1f)
                                 allowDragAnywhere = false
                         }
                     )
                else
                    modifier
            },
        shape = MaterialTheme.shapes.large,
        onClick = { NotiListenerService.getPendingIntent(context, notiUnit)?.send() },
        colors = CardDefaults.cardColors(
            containerColor = gradientColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
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
                        Image(
                            bitmap.asImageBitmap(),
                            "Notification Icon",
                            Modifier
                                .size(35.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            Column {
                Row {
                    if (notiUnit.summary.isNotBlank() && expansionProgress == 0f) {
                        Text(
                            modifier = Modifier
                                .background(Color.Transparent)
                                .padding(horizontal = 5.dp)
                                .align(Alignment.CenterVertically)
                                .weight(1F),
                            text = notiUnit.summary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                            color = Color.Cyan
                        )
                        requiresExpansion = true
                    } else {
                        Column(
                            Modifier
                                .padding(horizontal = 5.dp)
                                .weight(1f)
                        ) {
                            Row {
                                Text(text = notiUnit.appName, fontSize = 14.sp)
                                if (notiUnit.gptCategory.isNotBlank()) {
                                    Spacer(Modifier.padding(5.dp))
                                    Text(
                                        text = notiUnit.gptCategory,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Cyan
                                    )
                                }
                            }
                            if (notiUnit.score < 30)
                                ScoreDisplay(notiUnit)

                            Row {
                                val notiTitle = notiUnit.title.last()
                                Text(
                                    modifier = Modifier
                                        .background(Color.Transparent),
                                    text = if (notiTitle == "null") "" else notiTitle,
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
                                    val notiContent = notiUnit.content.last()
                                    Text(
                                        modifier = Modifier.background(Color.Transparent),
                                        text = if (notiContent == "null") "" else notiContent,
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
                    }

                    Column(
                        Modifier
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Vertical,
                                onDragStopped = { velocity ->
                                    expansionProgress = if (velocity > 0) 1f else 0F
                                    if (expansionProgress == 0f)
                                        allowDragAnywhere = true
                                }
                            )
                            .clickable {
                                expansionProgress =
                                    if (expansionProgress > 0f) 0f else 1F
                                if (expansionProgress == 0f)
                                    allowDragAnywhere = true
                            }
                    ) {
                        Text(text = notiUnit.getLastTime(), fontSize = 14.sp)
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
                                    .align(Alignment.End)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 5.dp))
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

                                var notiContents = notiUnit.content
                                var notiTimes = if (notiUnit.`when`.last() != 0L)
                                    notiUnit.`when`
                                else
                                    notiUnit.postTime
                                val expandLength = minOf(notiContents.size, notiTimes.size)
                                notiContents.reversed().zip(notiTimes.reversed()).reversed().forEach { (notiContent, notiTime) ->
                                    Row(Modifier.fillMaxWidth()) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 5.dp)
                                                .background(Color.Transparent),
                                            text = if (notiContent == "null") "" else notiContent,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 14.sp
                                            )
                                        )
                                        if (notiContents.size > 1) {
                                            Text(
                                                modifier = Modifier
                                                    .wrapContentWidth()
                                                    .background(Color.Transparent),
                                                text = getDisplayTimeStr(notiTime),
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
                            val yOffset =
                                currentHeight - maxContentHeight.toInt()  // Position content at the bottom
                            contentMeasures.forEach { it.place(0, yOffset) }
                        }
                    }
            }
        }
    }
}

@Composable
fun ScoreDisplay(notiUnit: NotiUnit) {
    Row {
        Text(
            text = String.format("%.2f", notiUnit.score),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Cyan
        )
        Spacer(Modifier.padding(5.dp))
        Text(
            text = "Time ${String.format("%.2f", notiUnit.scoreTime)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Cyan
        )
        Spacer(Modifier.padding(5.dp))
        Text(
            text = "Sender ${String.format("%.2f", notiUnit.scoreSender)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Cyan
        )
        Spacer(Modifier.padding(5.dp))
        Text(
            text = "Content ${String.format("%.2f", notiUnit.scoreContent)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Cyan
        )
    }
}