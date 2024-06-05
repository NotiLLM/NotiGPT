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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import org.muilab.notigpt.util.replaceChars
import org.muilab.notigpt.view.utils.NotiExpandState
import org.muilab.notigpt.viewModel.DrawerViewModel

@Composable
fun DynamicContentColor(
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    val contentColor = contentColorFor(backgroundColor)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(modifier = Modifier.background(backgroundColor)) {
            content()
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun NotiCard(context: Context, notiUnit: NotiUnit, drawerViewModel: DrawerViewModel, isScrolledToTop: () -> Boolean) {
    val bitmap = notiUnit.getBitmap()
    val largeBitmap = notiUnit.getLargeBitmap()

    val iconColor = when {
        !notiUnit.notiSeen -> MaterialTheme.colorScheme.onError
        notiUnit.pinned -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when {
        notiUnit.pinned -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val notiInfos = notiUnit.notiInfos.toList()

    var requiresExpansion by remember { mutableStateOf(
        notiInfos.size > 1 ||
                (notiInfos.size == 1
                && notiInfos[0].title.isNotBlank()
                && notiInfos[0].person.isNotBlank()
                && notiInfos[0].title != notiInfos[0].person)
    ) }
    var allowDragAnywhere by remember { mutableStateOf(true) }

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
                stiffness = Spring.StiffnessVeryLow,
            ),
            velocityThreshold = { with(density) { 80.dp.toPx()} }
        )
    }

    Card(
        modifier = Modifier
            .padding(vertical = 1.dp, horizontal = 16.dp)
            .fillMaxWidth()
            .let { modifier ->
                if (isScrolledToTop() && requiresExpansion && allowDragAnywhere && maxContentHeight < with(
                        density
                    ) { 200.dp.toPx() }
                )
                    modifier.anchoredDraggable(anchoredDraggableState, Orientation.Vertical)
                else
                    modifier
            },
        shape = MaterialTheme.shapes.large,
        onClick = {
            if (!notiUnit.pinned)
                drawerViewModel.actOnNoti(notiUnit, "click_dismiss")
            NotiListenerService.getPendingIntent(context, notiUnit)?.send()
        }
    ) {
        BoxWithConstraints {
            val constraints = this@BoxWithConstraints.constraints
            val progress = expansionProgress(anchoredDraggableState.offset, maxContentHeight)
            val iconSideWidthDp = (40 + 15 * progress).dp
            val iconSideWidthPx = with(density) { iconSideWidthDp.toPx() }
            val horizontalBrush = Brush.horizontalGradient(
                0.0f to iconColor,
                iconSideWidthPx / constraints.maxWidth.toFloat() to iconColor,
                iconSideWidthPx / constraints.maxWidth.toFloat() to textColor,
                1.0f to textColor
            )

            Box(modifier = Modifier.background(horizontalBrush)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    DynamicContentColor(iconColor) {
                        Column(
                            Modifier.padding(start = 2.dp, end = 3.dp),
                        ) {
                            if (bitmap != null)
                                if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD && largeBitmap != null) {
                                    Image(
                                        largeBitmap.asImageBitmap(),
                                        "Notification Icon",
                                        Modifier
                                            .size((35 + 15 * progress).dp)
                                            .padding(vertical = 3.dp, horizontal = 6.dp)
                                    )
                                } else {
                                    Icon(
                                        bitmap.asImageBitmap(),
                                        "Notification Icon",
                                        Modifier
                                            .size((35 + 15 * progress).dp)
                                            .padding(vertical = 3.dp, horizontal = 6.dp)
                                    )
                                }
                        }
                    }
                    DynamicContentColor(textColor) {
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
                                        Text(
                                            text = notiUnit.getLatestTimeStr(),
                                            maxLines = 1,
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic
                                        )
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
                                                .anchoredDraggable(
                                                    anchoredDraggableState,
                                                    Orientation.Vertical
                                                )
                                                .clickable {
                                                    if (!anchoredDraggableState.isAnimationRunning) {
                                                        coroutineScope.launch {
                                                            anchoredDraggableState.animateTo(
                                                                if (anchoredDraggableState.offset < COLLAPSE_THRESHOLD)
                                                                    NotiExpandState.Opened
                                                                else
                                                                    NotiExpandState.Collapsed
                                                            )
                                                        }
                                                    }
                                                    if (anchoredDraggableState.offset < COLLAPSE_THRESHOLD)
                                                        allowDragAnywhere = true
                                                }
                                        )
                                    }
                                    Spacer(
                                        Modifier
                                            .weight(1F)
                                            .padding(bottom = 15.dp)
                                    )
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

                                        val isThread = notiInfos.size > 1
                                        val isGroup = notiInfos.map { it.title }.toSet().size > 1

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
                                                val notiTime = notiInfos[i].time
                                                val notiContent = notiInfos[i].content
                                                val newTitle =
                                                    (i == 0 || notiTitle != notiInfos[i - 1].title)
                                                val showTitle = isThread && isGroup && newTitle

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
                                                    if (!(isGroup && newTitle) && isThread) {
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