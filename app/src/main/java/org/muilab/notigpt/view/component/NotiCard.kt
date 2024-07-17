package org.muilab.notigpt.view.component

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.muilab.notigpt.R
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.service.NotiListenerService
import org.muilab.notigpt.util.getDisplayTimeStr
import org.muilab.notigpt.util.hasTransparentPixels
import org.muilab.notigpt.util.replaceChars
import org.muilab.notigpt.view.utils.NotiExpandState
import org.muilab.notigpt.viewModel.DrawerViewModel
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NotiCard(context: Context, notiUnit: NotiUnit, drawerViewModel: DrawerViewModel) {
    val bitmap = notiUnit.getBitmap()
    val largeBitmap = notiUnit.getLargeBitmap()

    val backgroundColor = when {
        notiUnit.pinned -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val timeColor = when {
        !notiUnit.notiSeen -> MaterialTheme.colorScheme.error
        notiUnit.pinned -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val notiInfos = notiUnit.notiInfos.toList()

    var requiresExpansion by remember { mutableStateOf(
        notiInfos.size > 1 ||
                (notiInfos.size == 1 && notiInfos[0].getTitle(notiUnit.pkgName, notiUnit.isPeople)
                    .let { notiUnit.title.isNotBlank() && notiUnit.title != it }))
    }

    var maxContentHeight by remember { mutableFloatStateOf(0f) }
    val expansionProgress: (Float, Float) -> Float = { offset, maxHeight ->
        offset.coerceIn(0F, maxHeight) / maxOf(maxHeight, 1F)
    }
    val COLLAPSE_THRESHOLD = 20f

    val coroutineScope = rememberCoroutineScope()

    val anchors = DraggableAnchors {
        NotiExpandState.Collapsed at 0f
        NotiExpandState.Opened at maxContentHeight
    }
    val density = LocalDensity.current
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = NotiExpandState.Collapsed,
            anchors = anchors,
            positionalThreshold = {distance: Float -> distance * 0.5f},
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            velocityThreshold = { with(density) { 80.dp.toPx()} }
        )
    }

    var isInitialDragDownward by remember { mutableStateOf(false) }
    var hasStartedDragging by remember { mutableStateOf(false) }

    val customDragHandler = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                isInitialDragDownward = false
                hasStartedDragging = false
            },
            onDrag = { change, dragAmount ->
                if (!hasStartedDragging) {
                    hasStartedDragging = true
                    isInitialDragDownward = if (abs(dragAmount.y) > abs(dragAmount.x)) {
                        dragAmount.y > 0
                    } else {
                        false
                    }
                }

                if (isInitialDragDownward)
                    anchoredDraggableState.dispatchRawDelta(dragAmount.y)
                change.consume()
            },
            onDragEnd = {
                if (hasStartedDragging && isInitialDragDownward) {
                    coroutineScope.launch {
                        anchoredDraggableState.animateTo(
                            if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD)
                                NotiExpandState.Opened
                            else
                                NotiExpandState.Collapsed
                        )
                    }
                }
                hasStartedDragging = false
            }
        )
    }



    Card(
        modifier = Modifier
            .padding(vertical = 1.dp, horizontal = 16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        onClick = {
            if (!notiUnit.pinned)
                drawerViewModel.actOnNoti(notiUnit, "click_dismiss")
            NotiListenerService.getPendingIntent(context, notiUnit)?.send()
        },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        val progress = expansionProgress(anchoredDraggableState.offset, maxContentHeight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {
            Column(
                Modifier.padding(start = 2.dp, end = 3.dp),
            ) {
                if (bitmap != null) {
                    if (largeBitmap != null && bitmap != largeBitmap)
                        requiresExpansion = true
                    if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD && largeBitmap != null) {
                        Image(
                            largeBitmap.asImageBitmap(),
                            "Notification Icon",
                            Modifier
                                .size((35 + 15 * progress).dp)
                                .padding(vertical = 3.dp, horizontal = 6.dp)
                        )
                    } else {
                        if (hasTransparentPixels(bitmap, 0.1f)) {
                            Icon(
                                bitmap.asImageBitmap(),
                                "Notification Icon",
                                Modifier
                                    .size((35 + 15 * progress).dp)
                                    .padding(vertical = 3.dp, horizontal = 6.dp),
                                tint = contentColorFor(backgroundColor)
                            )
                        } else {
                            Image(
                                bitmap.asImageBitmap(),
                                "Notification Icon",
                                Modifier
                                    .size((35 + 15 * progress).dp)
                                    .padding(vertical = 3.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
            Column {
                Row {

                    Column(
                        Modifier
                            .padding(horizontal = 5.dp)
                            .weight(1f)
                    ) {
                        val notiTitle = notiUnit.title
                        Row(Modifier.fillMaxWidth()) {
                            if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD) {
                                Text(
                                    text = notiUnit.appName,
                                    fontSize = (10 + progress * 4).sp
                                )
                                Spacer(Modifier.weight(1F))
                            } else {
                                Text(
                                    modifier = Modifier
                                        .background(Color.Transparent)
                                        .weight(1F),
                                    text = if (notiTitle == "null") notiUnit.appName else notiTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.hasVisualOverflow)
                                            requiresExpansion = true
                                    }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(timeColor, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    modifier = Modifier.padding(horizontal = 5.dp),
                                    text = notiUnit.getLatestTimeStr(),
                                    maxLines = 1,
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = contentColorFor(timeColor)
                                )
                            }
                        }

                        Row {
                            if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD) {
                                Text(
                                    modifier = Modifier
                                        .background(Color.Transparent),
                                    text = if (notiTitle == "null") "" else notiTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    maxLines = if (anchoredDraggableState.offset < COLLAPSE_THRESHOLD) 1 else Int.MAX_VALUE,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = (14 + progress * 2).sp,
                                    onTextLayout = { textLayoutResult ->
                                        if (textLayoutResult.hasVisualOverflow)
                                            requiresExpansion = true
                                    }
                                )
                            } else {
                                val notiContent = notiInfos.last().content
                                Text(
                                    modifier = Modifier.background(Color.Transparent),
                                    text = if (notiContent == "null") "" else replaceChars(
                                        notiContent
                                    ),
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

                    Column {
                        if (notiUnit.pinned) {
                            Icon(
                                painter = painterResource(R.drawable.pin),
                                "Pin",
                                Modifier
                                    .size(15.dp)
                                    .align(Alignment.End)
                                    .rotate(45f)
                            )
                        } else {
                            Spacer(Modifier.size(15.dp))
                        }
                        if (requiresExpansion) {
                            Icon(
                                painter = if (progress < 0.5f)
                                    painterResource(R.drawable.expand_circle_down)
                                else
                                    painterResource(R.drawable.expand_circle_up),
                                "Expand",
                                Modifier
                                    .size(25.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .then(customDragHandler)
                                    .clickable {
                                        if (!hasStartedDragging) {
                                            coroutineScope.launch {
                                                anchoredDraggableState.animateTo(
                                                    if (anchoredDraggableState.offset < COLLAPSE_THRESHOLD)
                                                        NotiExpandState.Opened
                                                    else
                                                        NotiExpandState.Collapsed
                                                )
                                            }
                                        }
                                    }
                            )
                        } else {
                            Spacer(Modifier.size(25.dp))
                        }
                    }
                    Spacer(modifier = Modifier.padding(5.dp))
                }

                if (requiresExpansion)
                    SubcomposeLayout(Modifier.clipToBounds()) { constraints ->
                        val content = subcompose("content") {

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 5.dp),
                                thickness = 1.dp,
                                color = Color.White
                            )

                            val isGroup = (listOf(notiUnit.title)
                                    + notiInfos.map { it.getTitle(notiUnit.pkgName, notiUnit.isPeople) })
                                .filter { it.isNotBlank() }
                                .toSet().size > 1

                            val listState = rememberLazyListState(
                                initialFirstVisibleItemIndex = maxOf(
                                    notiInfos.size - 1,
                                    0
                                )
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(0.dp, 200.dp),
                                state = listState
                            ) {

                                items(notiInfos.size) { i ->

                                    val notiTitle = notiInfos[i].getTitle(
                                        notiUnit.pkgName,
                                        notiUnit.isPeople
                                    )
                                    val prevTitle = if (i == 0)
                                        notiUnit.title
                                    else
                                        notiInfos[i - 1].getTitle(
                                            notiUnit.pkgName,
                                            notiUnit.isPeople
                                        )
                                    val notiTime = notiInfos[i].time
                                    val notiContent = notiInfos[i].content
                                    val newTitle = (notiTitle != prevTitle && notiTitle.isNotBlank() && prevTitle.isNotBlank())
                                    val showTitle = isGroup && newTitle

                                    if (newTitle)
                                        Spacer(modifier = Modifier.height(4.dp))

                                    Row(Modifier.fillMaxWidth()) {
                                        Text(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 5.dp)
                                                .background(Color.Transparent),
                                            text = if (showTitle) {
                                                if (notiTitle == "null") "" else notiTitle
                                            } else {
                                                if (notiContent == "null") "" else notiContent
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 14.sp,
                                                fontWeight = if (showTitle)
                                                    FontWeight.Bold
                                                else
                                                    FontWeight.Normal
                                            )
                                        )
                                        if (!showTitle) {
                                            Text(
                                                modifier = Modifier
                                                    .wrapContentWidth()
                                                    .background(Color.Transparent),
                                                text = getDisplayTimeStr(notiTime),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 14.sp,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.padding(horizontal = 5.dp))
                                    }

                                    if (showTitle) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Text(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 5.dp)
                                                    .background(Color.Transparent),
                                                text = if (notiContent == "null") "" else notiContent,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 14.sp
                                                )
                                            )
                                            Text(
                                                modifier = Modifier
                                                    .wrapContentWidth()
                                                    .background(Color.Transparent),
                                                text = getDisplayTimeStr(notiTime),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 14.sp,
                                                    fontStyle = FontStyle.Italic
                                                )
                                            )
                                            Spacer(
                                                modifier = Modifier.padding(
                                                    horizontal = 5.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val contentMeasures = content.map { it.measure(constraints) }
                        if (contentMeasures.maxOf { it.height }
                                .toFloat() > maxContentHeight) {
                            maxContentHeight =
                                contentMeasures.maxOf { it.height }.toFloat()
                            anchoredDraggableState.updateAnchors(
                                DraggableAnchors {
                                    NotiExpandState.Collapsed at 0f
                                    NotiExpandState.Opened at maxContentHeight
                                }
                            )
                        }
                        val currentHeight =
                            anchoredDraggableState.offset.coerceIn(0F, maxContentHeight)
                                .toInt()
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
    }
}