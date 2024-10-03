package org.muilab.notigpt.view.component

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import kotlinx.coroutines.delay
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
fun NotiCard(context: Context, notiUnit: NotiUnit, drawerViewModel: DrawerViewModel, notiViewed: MutableState<Boolean>, viewedInfos: MutableSet<Long>) {

    var notiTopViewed by remember { mutableStateOf(false) }
    var notiBottomViewed by remember { mutableStateOf(false) }

    notiUnit.notiKey
    val pinned = notiUnit.getPinned()
    val wholeNotiRead = notiUnit.getWholeNotiRead()
    val notiOverallTitle = notiUnit.getTitle()
    val isPeople = notiUnit.getIsPeople()
    val pkgName = notiUnit.getPkgName()
    val appName = notiUnit.getAppName()
    val bitmap = notiUnit.getBitmap()
    val largeBitmap = notiUnit.getLargeBitmap()

    val backgroundColor = when {
        pinned -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val timeColor = when {
        !wholeNotiRead -> MaterialTheme.colorScheme.error
        pinned -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val notiBody = notiUnit.getNotiBody().toCollection(ArrayList())
    var requiresExpansion by remember { mutableStateOf(
        notiBody.size > 1 ||
                (notiBody.size == 1 && notiBody[0].getTitle(pkgName, isPeople)
                    .let { notiOverallTitle.isNotBlank() && notiOverallTitle != it }))
    }


    var contentHeightPx by remember { mutableIntStateOf(0) }
    var latestMessageHeightPx by remember { mutableIntStateOf(0) }
    var maxContentHeightPx by remember { mutableFloatStateOf(0f) }
    val maxHeightDp = 200.dp
    val notiInfoGapDp = 4.dp

    val expansionProgress: (Float, Float) -> Float = { offset, maxHeight ->
        offset.coerceIn(0F, maxHeight) / maxOf(maxHeight, 1F)
    }
    val COLLAPSE_THRESHOLD = 20f

    val coroutineScope = rememberCoroutineScope()

    val density = LocalDensity.current
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = NotiExpandState.Collapsed,
            anchors = DraggableAnchors {
                NotiExpandState.Collapsed at 0f
                NotiExpandState.Opened at maxContentHeightPx
            },
            positionalThreshold = { distance: Float -> distance * 0.5f },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            decayAnimationSpec = exponentialDecay(),
            velocityThreshold = { with(density) { 80.dp.toPx() } }
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
                            if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD) {
                                NotiExpandState.Opened
                            } else {
                                NotiExpandState.Collapsed
                            }
                        )
                    }
                }
                hasStartedDragging = false
            }
        )
    }

    var isDropdownMenuExpanded by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // 下拉式選單當鍵盤開出時就可以自己跳到上面來，不需要等到按下換行按鍵才重新佈局
    var isKeyboardVisible = isKeyboardVisible()
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && isDropdownMenuExpanded) {
            inputText = "\n"
            delay(5)
            inputText = inputText.dropLast(1)
        }
    }

    Card(
        modifier = Modifier
            .padding(vertical = 1.dp, horizontal = 20.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (!pinned)
                        drawerViewModel.actOnNoti(notiUnit, "click_dismiss")
                    NotiListenerService
                        .getPendingIntent(context, notiUnit)
                        ?.send()
                },
                onLongClick = {
                    isDropdownMenuExpanded = true
                }
            )
            .onGloballyPositioned { coordinates ->
                val windowBounds = coordinates.boundsInWindow()
                notiTopViewed = notiTopViewed || windowBounds.top >= 0 && windowBounds.top < windowBounds.height
            }
            .onGloballyPositioned { coordinates ->
                val windowBounds = coordinates.boundsInWindow()
                notiBottomViewed = notiBottomViewed || windowBounds.bottom > 0 && windowBounds.bottom <= windowBounds.height
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        //**
        DropdownMenu(
            expanded = isDropdownMenuExpanded,
            onDismissRequest = { isDropdownMenuExpanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Spacer(modifier = Modifier.weight(0.7f))
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("填寫意見") },
                    modifier = Modifier.weight(8f)
                )
                Spacer(modifier = Modifier.weight(0.7f))
                Button(
                    onClick = {
                        getComment(inputText,notiUnit.getHashKey())
                        isDropdownMenuExpanded = false
                    },
                    modifier = Modifier.weight(3f)
                ){
                    Text("送出")
                }
                Spacer(modifier = Modifier.weight(0.7f))
            }
        }
        //**

        val progress = expansionProgress(anchoredDraggableState.offset, maxContentHeightPx)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        ) {
            Column(
                Modifier.padding(start = 2.dp, end = 3.dp),
            ) {
                // Use remember to optimize bitmap selection logic
                val imageToDisplay = remember(bitmap, largeBitmap, anchoredDraggableState.offset) {
                    if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD && largeBitmap != null) {
                        largeBitmap.asImageBitmap()
                    } else {
                        bitmap?.asImageBitmap()
                    }
                }

                // Use remember to optimize transparent pixel check
                val hasTransparency = remember(bitmap) {
                    bitmap != null && hasTransparentPixels(bitmap, 0.1f)
                }

                if (imageToDisplay != null) {
                    if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD && largeBitmap != null) {
                        // Render the large image when expanded
                        Image(
                            bitmap = imageToDisplay,
                            contentDescription = "Notification Icon",
                            modifier = Modifier
                                .size((35 + 15 * progress).dp)
                                .padding(vertical = 3.dp, horizontal = 6.dp)
                        )
                    } else {
                        // Render the icon if bitmap has transparency, otherwise render the image
                        if (hasTransparency) {
                            Icon(
                                bitmap = imageToDisplay,
                                contentDescription = "Notification Icon",
                                modifier = Modifier
                                    .size((35 + 15 * progress).dp)
                                    .padding(vertical = 3.dp, horizontal = 6.dp),
                                tint = contentColorFor(backgroundColor)
                            )
                        } else {
                            Image(
                                bitmap = imageToDisplay,
                                contentDescription = "Notification Icon",
                                modifier = Modifier
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
                        Row(Modifier.fillMaxWidth()) {
                            if (anchoredDraggableState.offset > COLLAPSE_THRESHOLD) {
                                Text(
                                    text = appName,
                                    fontSize = (10 + progress * 4).sp
                                )
                                Spacer(Modifier.weight(1F))
                            } else {
                                Text(
                                    modifier = Modifier
                                        .background(Color.Transparent)
                                        .weight(1F),
                                    text = if (notiOverallTitle == "null") appName else notiOverallTitle,
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
                                    text = if (notiOverallTitle == "null") "" else notiOverallTitle,
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
                                val notiContent = notiBody.last().content
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
                        if (pinned) {
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
                                                    if (anchoredDraggableState.offset < COLLAPSE_THRESHOLD) {
                                                        NotiExpandState.Opened
                                                    } else {
                                                        NotiExpandState.Collapsed
                                                    }
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

                if (requiresExpansion) {

                    // Update maxContentHeightPx based on contentHeightPx
                    LaunchedEffect(contentHeightPx) {
                        val maxHeightPx = with(density) { maxHeightDp.toPx() }
                        maxContentHeightPx = minOf(contentHeightPx.toFloat(), maxHeightPx)
                        anchoredDraggableState.updateAnchors(
                            DraggableAnchors {
                                NotiExpandState.Collapsed at 0f
                                NotiExpandState.Opened at maxContentHeightPx
                            }
                        )
                    }

                    val currentHeightPx = anchoredDraggableState.offset.coerceIn(0f, maxContentHeightPx)

                    Column(
                        modifier = Modifier
                            .height(with(density) { currentHeightPx.toDp() })
                            .clipToBounds()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 5.dp),
                            thickness = 1.dp,
                            color = Color.White
                        )

                        // Scrollable content
                        val scrollState = rememberScrollState()

                        val isGroup = (listOf(notiOverallTitle)
                                + notiBody.map { it.getTitle(pkgName, isPeople) })
                                    .filter { it.isNotBlank() }
                                    .toSet().size > 1

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                                .onSizeChanged { size ->
                                    contentHeightPx = size.height
                                }
                        ) {
                            notiBody.forEachIndexed { idx, noti ->

                                val notiTitle = noti.getTitle(pkgName, isPeople)
                                val prevTitle = if (idx == 0)
                                       notiOverallTitle
                                    else
                                        notiBody[idx - 1].getTitle(pkgName, isPeople)
                                val notiTime = noti.time
                                val notiContent = noti.content
                                val notiSeen = noti.notiSeen
                                val newTitle = (notiTitle != prevTitle && notiTitle.isNotBlank() && prevTitle.isNotBlank())
                                val showTitle = isGroup && newTitle

                                val infoTimeColor = when {
                                    !notiSeen -> MaterialTheme.colorScheme.error
                                    pinned -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }

                                if (showTitle)
                                    Spacer(modifier = Modifier.height(notiInfoGapDp))

                                if (idx == notiBody.size - 1) {
                                    Box(
                                        modifier = Modifier.onSizeChanged { size -> latestMessageHeightPx = size.height }
                                    ) {
                                        ExpandedNotiInfo(
                                            notiTitle,
                                            notiTime,
                                            notiContent,
                                            notiSeen,
                                            showTitle,
                                            infoTimeColor,
                                            viewedInfos
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(notiInfoGapDp))
                                } else {
                                    ExpandedNotiInfo(
                                        notiTitle,
                                        notiTime,
                                        notiContent,
                                        notiSeen,
                                        showTitle,
                                        infoTimeColor,
                                        viewedInfos
                                    )
                                }
                            }
                        }

                        // Scroll to the appropriate position based on requirements
                        LaunchedEffect(contentHeightPx, maxContentHeightPx, latestMessageHeightPx) {
                            val maxHeightPx = with(density) { maxHeightDp.toPx() }
                            val notiInfoGapPx = with(density) { notiInfoGapDp.toPx() }
                            if (contentHeightPx > maxHeightPx) {
                                val targetScroll = if (latestMessageHeightPx >= maxHeightPx) {
                                    contentHeightPx - latestMessageHeightPx - notiInfoGapPx
                                } else {
                                    contentHeightPx - maxHeightPx - notiInfoGapPx
                                }
                                scrollState.scrollTo(maxOf(targetScroll.toInt(), 0))
                            } else {
                                scrollState.scrollTo(0)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(notiTopViewed, notiBottomViewed) {
        if (notiTopViewed && notiBottomViewed && !notiUnit.getWholeNotiRead() && !requiresExpansion) {
            notiViewed.value = true
            for (noti in notiBody)
                noti.notiSeen = true
            viewedInfos.clear()
        }
    }
}

@Composable
fun ExpandedNotiInfo(
    notiTitle: String,
    notiTime: Long,
    notiContent: String,
    notiSeen: Boolean,
    showTitle: Boolean,
    infoTimeColor: Color,
    viewedInfos: MutableSet<Long>
) {

    var infoTopViewed by remember { mutableStateOf(false) }
    var infoBottomViewed by remember { mutableStateOf(false) }

    Column(
        Modifier
            .onGloballyPositioned { coordinates ->
                val windowBounds = coordinates.boundsInWindow()
                infoTopViewed = infoTopViewed || windowBounds.top >= 0 && windowBounds.top < windowBounds.height
            }
            .onGloballyPositioned { coordinates ->
                val windowBounds = coordinates.boundsInWindow()
                infoBottomViewed = infoBottomViewed || windowBounds.bottom > 0 && windowBounds.bottom <= windowBounds.height
            }
    ) {
        ConstraintLayout (Modifier.fillMaxWidth()) {
            val (leftText, rightText) = createRefs()
            val leftTextModifier = Modifier
                .constrainAs(leftText) {
                    start.linkTo(parent.start)
                    end.linkTo(rightText.start)
                    top.linkTo(parent.top)
                    width = Dimension.fillToConstraints
                }
            val rightTextModifier = Modifier
                .constrainAs(rightText) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                }
            if (showTitle)
                NotiInfoTitle(notiTitle, leftTextModifier)
            else
                NotiInfoContent(notiContent, leftTextModifier)
            NotiInfoTime(notiTime, infoTimeColor, rightTextModifier)
        }
        if (showTitle)
            NotiInfoContent(notiContent)
    }

    LaunchedEffect(infoTopViewed, infoBottomViewed) {
        if (infoTopViewed && infoBottomViewed && !notiSeen) {
            viewedInfos.add(notiTime)  // Mark the item as viewed once fully revealed
        }
    }
}

@Composable
fun NotiInfoTitle(notiTitle: String, modifier: Modifier = Modifier) {
    Text(
        modifier = Modifier
            .padding(start = 5.dp)
            .then(modifier),
        text = if (notiTitle == "null") "" else notiTitle,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
fun NotiInfoTime(notiTime: Long, infoTimeColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .background(
                infoTimeColor,
                RoundedCornerShape(16.dp)
            )
            .then(modifier)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 5.dp),
            text = getDisplayTimeStr(notiTime),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            ),
            color = contentColorFor(infoTimeColor)
        )
    }
}

@Composable
fun NotiInfoContent(notiContent: String, modifier: Modifier = Modifier) {
    Text(
        modifier = Modifier
            .padding(horizontal = 5.dp)
            .then(modifier),
        text = if (notiContent == "null") "" else notiContent,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 14.sp
        )
    )
}

@Composable
fun ScoreDisplay(notiUnit: NotiUnit) {
    Row {
        Text(
            text = String.format("%.2f", notiUnit.getScore()),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Cyan
        )
        Spacer(Modifier.padding(5.dp))
    }
}

fun getComment(input: String, notiHashkey: Int) {
    Log.d("MyApp", "用戶對通知$notiHashkey 輸入意見:$input")
}

@Composable
fun isKeyboardVisible(): Boolean {
    val view = LocalView.current
    val rootView = (LocalContext.current as Activity).window.decorView
    var isKeyboardVisible by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            isKeyboardVisible = keypadHeight > screenHeight * 0.15
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return isKeyboardVisible
}