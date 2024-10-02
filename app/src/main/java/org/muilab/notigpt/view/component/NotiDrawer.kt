package org.muilab.notigpt.view.component

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.notigpt.R
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.util.postOngoingNotification
import org.muilab.notigpt.view.utils.LifecycleObserver
import org.muilab.notigpt.viewModel.DrawerViewModel
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotiDrawer(context: Context, drawerViewModel: DrawerViewModel, category: String) {

    val listState = rememberLazyListState()
    val notSeenCount by drawerViewModel.notSeenCount.observeAsState(0)
    val coroutineScope = rememberCoroutineScope()

    val notifications by drawerViewModel.getFilteredFlow(category).collectAsState()
    val seenItems = remember { mutableSetOf<String>() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 8.dp),
    ) {
        items(notifications, key = { it.notiKey }) { notiUnit ->

            BoxWithConstraints {

                val density = LocalDensity.current
                val interactionSource = remember { MutableInteractionSource() }
                val resistanceLimit = with(density) { 64.dp.toPx() }  // Convert to pixels
                val dismissLimit = -0.5f * constraints.maxWidth.toFloat()
                var pinningChanged by remember { mutableStateOf(false) }

                val anchoredDraggableState = remember {
                    AnchoredDraggableState(
                        initialValue = 0,
                        anchors = DraggableAnchors {
                            0 at 0f
                            1 at resistanceLimit
                        },
                        positionalThreshold = { d -> d * 0.5f },
                        velocityThreshold = { Float.POSITIVE_INFINITY },
                        snapAnimationSpec = tween(),
                        decayAnimationSpec = exponentialDecay(0.00001f, 0.00001f)
                    )
                }

                // A flag to prevent re-triggering the snap back
                var isBackAnimationTriggered by remember { mutableStateOf(false) }

                // Reset the flag when the user starts dragging again
                if (anchoredDraggableState.isAnimationRunning) {
                    isBackAnimationTriggered = false
                }

                LaunchedEffect(anchoredDraggableState.currentValue) {
                    when (anchoredDraggableState.currentValue) {
                        1 -> {
                            if (!pinningChanged) {
                                pinningChanged = true
                                drawerViewModel.actOnNoti(notiUnit, "pin")
                                anchoredDraggableState.snapTo(0)
                            }
                        }

                        -1 -> {
                            drawerViewModel.actOnNoti(notiUnit, "swipe_dismiss")
                        }
                    }
                }

                LaunchedEffect(notiUnit.getPinned()) {
                    pinningChanged = false
                    if (notiUnit.getPinned()) {
                        anchoredDraggableState.updateAnchors(
                            DraggableAnchors {
                                0 at 0f
                                1 at resistanceLimit
                            }
                        )
                    } else {
                        anchoredDraggableState.updateAnchors(
                            DraggableAnchors {
                                (-1) at dismissLimit
                                0 at 0f
                                1 at resistanceLimit
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = anchoredDraggableState,
                            orientation = Orientation.Horizontal,
                            interactionSource = interactionSource,
                        )
                        .animateItem()
                        .offset {
                            IntOffset(
                                anchoredDraggableState
                                    .requireOffset()
                                    .roundToInt(), 0
                            )
                        }
                ) {
                    NotiCard(context, notiUnit, drawerViewModel)
                }
            }
        }
    }

    val firstVisibleIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    val scrollOffset by remember {
        derivedStateOf { listState.firstVisibleItemScrollOffset }
    }
    if (firstVisibleIndex > 0) {
        val notiCount = firstVisibleIndex + if (scrollOffset > 0) 1 else 0

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
                        if (itemInfo.index < notifications.size)
                            notifications[itemInfo.index]
                        else
                            null
                    }
                    .filter { notiUnit -> !notiUnit.getWholeNotiRead() }
                    .map { notiUnit ->
                        notiUnit.notiKey
                    }
            }.collect { seenIds ->
                seenItems.addAll(seenIds)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val leaveAppEvents = setOf(
            Lifecycle.Event.ON_PAUSE
        )
        val observer = LifecycleEventObserver { _, event ->
            if (event in leaveAppEvents) {
                updateSeenNotifications(context, seenItems)
                postOngoingNotification(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val visibleItems = remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo } }
    LaunchedEffect(visibleItems) {
        seenItems.addAll(
            visibleItems.value
                .filter { it.offset >= 0 && (it.offset + it.size) <= listState.layoutInfo.viewportEndOffset }
                .map { it.key.toString() }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            updateSeenNotifications(context, seenItems)
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
        newSeenNotis.forEachIndexed { idx, _ -> newSeenNotis[idx].markAsRead() }
        drawerDao.updateList(newSeenNotis)
        postOngoingNotification(context)
    }
}