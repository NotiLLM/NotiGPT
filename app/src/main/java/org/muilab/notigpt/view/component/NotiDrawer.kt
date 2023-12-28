package org.muilab.notigpt.view.component

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import org.muilab.notigpt.viewModel.DrawerViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun NotiDrawer(context: Context, drawerViewModel: DrawerViewModel) {

    val lazyPagingItems = drawerViewModel.allPaged.collectAsLazyPagingItems()

    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey {notiUnit -> "${notiUnit.ranking}_${notiUnit.sbnKey}"},
            contentType = lazyPagingItems.itemContentType { "Notifications" }
        ) { idx ->
            val notiUnit = lazyPagingItems[idx]
            notiUnit?.let { noti ->

                val dismissState = rememberDismissState(
                    confirmStateChange = {
                        if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                            drawerViewModel.deleteNoti(noti)
                            lazyPagingItems.refresh()
                        }
                        true
                    }
                )

                SwipeToDismiss(
                    state = dismissState,
                    directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                    dismissThresholds = { FractionalThreshold(0.9f) },
                    background = { Background(dismissState = dismissState) },
                    dismissContent = {
                        AnimatedVisibility(
                            visible = dismissState.currentValue != DismissValue.DismissedToEnd && dismissState.currentValue != DismissValue.DismissedToStart,
                            exit = slideOutVertically() + fadeOut(),  // Customize this for smoother exit
                        ) {
                            Box(Modifier
                                .animateItemPlacement(tween(durationMillis = 200))
                                .fillMaxWidth()) {
                                NotiCard(context, noti)
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Background(dismissState: DismissState) {
    val color = when (dismissState.dismissDirection) {
        DismissDirection.StartToEnd -> Color.Green
        DismissDirection.EndToStart -> Color.Red
        null -> Color.LightGray
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
    )
}