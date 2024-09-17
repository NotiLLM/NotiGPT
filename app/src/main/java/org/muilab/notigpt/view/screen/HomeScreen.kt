package org.muilab.notigpt.view.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.muilab.notigpt.view.component.DevControlPanel
import org.muilab.notigpt.view.component.NotiDrawer
import org.muilab.notigpt.view.component.UserControlPanel
import org.muilab.notigpt.viewModel.DrawerViewModel
import org.muilab.notigpt.viewModel.GPTViewModel

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HomeScreen(context: Context, drawerViewModel: DrawerViewModel, gptViewModel: GPTViewModel, category:String) {

    val notiPostContent by drawerViewModel.notiPostContent.observeAsState("")

    Column {
        Box (Modifier.weight(1F), contentAlignment = Alignment.TopCenter) {
            NotiDrawer(context, drawerViewModel, category)
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.BottomCenter
//            ) {
//                TestCard(gptViewModel)
//            }
        }
        UserControlPanel(drawerViewModel)
        DevControlPanel(context, drawerViewModel, gptViewModel)
    }

    LaunchedEffect(notiPostContent) {
        if (notiPostContent.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", notiPostContent)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}