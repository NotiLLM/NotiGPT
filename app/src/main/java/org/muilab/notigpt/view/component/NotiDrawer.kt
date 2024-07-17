package org.muilab.notigpt.view.component

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.notigpt.R
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.util.postOngoingNotification
import org.muilab.notigpt.view.utils.LifecycleObserver
import org.muilab.notigpt.viewModel.DrawerViewModel
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterialApi::class
)
@Composable
fun NotiDrawer(context: Context, drawerViewModel: DrawerViewModel) {

    val listState = rememberLazyListState()
    val notSeenCount by drawerViewModel.notSeenCount.observeAsState(0)
    val coroutineScope = rememberCoroutineScope()

    val lazyPagingItems = drawerViewModel.allPaged.collectAsLazyPagingItems()
    val notiToKey: (NotiUnit) -> String = {
        val priority = when {
            (!it.notiSeen) -> "A"
            else -> "B"
        }
        "${priority}_${(6 - it.importance).toString()[0]}_${it.sbnKey}"
    }
    val seenItems = remember { mutableSetOf<String>() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 8.dp),
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { notiToKey(it) },
            contentType = lazyPagingItems.itemContentType { "Notifications" }
        ) { idx ->

            lazyPagingItems[idx]?.let { notiUnit ->

                BoxWithConstraints {

                    val density = LocalDensity.current
                    val swipeableState = rememberSwipeableState(initialValue = 0)
                    val interactionSource = remember { MutableInteractionSource() }
                    val resistanceLimit = with(density) { 32.dp.toPx() }  // Convert to pixels
                    val dismissLimit = -constraints.maxWidth.toFloat()
                    var pinningChanged by remember { mutableStateOf(false) }

                    // Setup anchors for swipeable states: 0 for initial, right for resistance, left for dismiss
                    val anchors = if (notiUnit.pinned)
                        mapOf(
                            0f to 0,
                            resistanceLimit to 1
                        )
                    else
                        mapOf(
                            0f to 0,
                            resistanceLimit to 1,
                            dismissLimit to -1
                        )

                    // A flag to prevent re-triggering the snap back
                    var isBackAnimationTriggered by remember { mutableStateOf(false) }

                    // Reset the flag when the user starts dragging again
                    if (swipeableState.isAnimationRunning) {
                        isBackAnimationTriggered = false
                    }

                    LaunchedEffect(swipeableState, interactionSource, isBackAnimationTriggered) {
                        Log.d("Swipe", "${notiUnit.title}: $isBackAnimationTriggered")
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is DragInteraction.Stop -> {
                                    if ((swipeableState.offset.value >= 32 || swipeableState.currentValue == 1) && !pinningChanged) {
                                        pinningChanged = true
                                        swipeableState.snapTo(0)
                                        drawerViewModel.actOnNoti(notiUnit, "pin")
                                        pinningChanged = false
                                        lazyPagingItems.refresh()
                                    }
                                    if (swipeableState.offset.value <= dismissLimit * 0.9f) {
                                        drawerViewModel.actOnNoti(notiUnit, "swipe_dismiss")
                                        lazyPagingItems.refresh()
                                    }
                                    if (swipeableState.currentValue != -1 && !swipeableState.isAnimationRunning && !isBackAnimationTriggered) {
                                        swipeableState.animateTo(0)
                                        isBackAnimationTriggered = true  // Set flag to prevent re-triggering
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(swipeableState) {
                        snapshotFlow { swipeableState.currentValue }
                            .collect { currentValue ->
                                when (currentValue) {
                                    1 -> {
                                        if (!pinningChanged) {
                                            pinningChanged = true
                                            swipeableState.snapTo(0)
                                            drawerViewModel.actOnNoti(notiUnit, "pin")
                                            pinningChanged = false
                                            lazyPagingItems.refresh()
                                        }
                                    }
                                    -1 -> {
                                        drawerViewModel.actOnNoti(notiUnit, "swipe_dismiss")
                                        lazyPagingItems.refresh()
                                    }
                                }
                            }
                        snapshotFlow { swipeableState.offset.value }
                            .collect { offset ->
                                when {
                                    offset >= resistanceLimit * 0.9f -> {
                                        if (!pinningChanged) {
                                            pinningChanged = true
                                            swipeableState.snapTo(0)
                                            drawerViewModel.actOnNoti(notiUnit, "pin")
                                            pinningChanged = false
                                            lazyPagingItems.refresh()
                                        }
                                    }
                                    offset <= dismissLimit * 0.6f -> {
                                        drawerViewModel.actOnNoti(notiUnit, "swipe_dismiss")
                                        lazyPagingItems.refresh()
                                    }
                                }
                            }
                    }

                    Box(
                        modifier = Modifier
                            .swipeable(
                                state = swipeableState,
                                anchors = anchors,
                                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                                orientation = Orientation.Horizontal,
                                interactionSource = interactionSource,
                                resistance = SwipeableDefaults.resistanceConfig(
                                    anchors = anchors.keys,
                                    factorAtMin = if (notiUnit.pinned) Float.POSITIVE_INFINITY else 0f,
                                    factorAtMax = 10f
                                )
                            )
                            .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
                    ) {
                        NotiCard(context, notiUnit, drawerViewModel)
                    }
                }

                if (listState.layoutInfo.visibleItemsInfo.any { it.key == notiToKey(notiUnit) }) {
                    LaunchedEffect(notiUnit.sbnKey) {
                        if (!notiUnit.notiSeen)
                            seenItems.add(notiUnit.sbnKey)
                    }
                }
            }
        }
    }

    val notiCount =  listState.firstVisibleItemIndex
    + if (listState.firstVisibleItemScrollOffset > 0) 1 else 0

    if (notiCount > 0 && listState.firstVisibleItemIndex > 0) {

        val displayText = if (notSeenCount > 0) {
            "${minOf(notiCount, notSeenCount)} new notifications above"
        } else {
            "$notiCount notifications above"
        }

        ElevatedButton(
            onClick = {
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.reach_top),
                "Reach Top",
                Modifier
                    .size(25.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                displayText,
                textAlign = TextAlign.Center
            )
        }
    }

    var isForeground by remember { mutableStateOf(false) }
    LifecycleObserver(
        onResume = { isForeground = true },
        onPause = { isForeground = false }
    )

    if (isForeground) {
        LaunchedEffect(listState) {
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo
                    .mapNotNull { itemInfo ->
                        if (itemInfo.index < lazyPagingItems.itemCount)
                            lazyPagingItems.peek(itemInfo.index)
                        else
                            null
                    }
                    .filter { notiUnit -> !notiUnit.notiSeen }
                    .map { notiUnit ->
                        notiUnit.sbnKey
                    }
            }.collect { seenIds ->
                seenItems.addAll(seenIds)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val leaveAppEvents = setOf(
            Lifecycle.Event.ON_PAUSE
        )
        val observer = LifecycleEventObserver { _, event ->
            if (event in leaveAppEvents) {
                updateSeenNotifications(context, seenItems)
                lazyPagingItems.refresh()
                postOngoingNotification(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            updateSeenNotifications(context, seenItems)
            lazyPagingItems.refresh()
            postOngoingNotification(context)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun updateSeenNotifications(context: Context, seenItems: Set<String>) {
    CoroutineScope(Dispatchers.IO).launch {
        val drawerDatabase = DrawerDatabase.getInstance(context)
        val drawerDao = drawerDatabase.drawerDao()
        val newSeenNotis = drawerDao.getBySbnKeys(seenItems.toList())
        newSeenNotis.forEachIndexed { idx, _ -> newSeenNotis[idx].notiSeen = true }
        drawerDao.updateList(newSeenNotis)
        postOngoingNotification(context)
    }
}