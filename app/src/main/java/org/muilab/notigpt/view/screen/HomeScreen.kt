package org.muilab.notigpt.view.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.muilab.notigpt.view.component.NotiDrawer
import org.muilab.notigpt.viewModel.DrawerViewModel
import org.muilab.notigpt.viewModel.GPTViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(context: Context, drawerViewModel: DrawerViewModel, gptViewModel: GPTViewModel) {

    val notiPostContent by drawerViewModel.notiPostContent.observeAsState("")

    Column {
        Box (Modifier.weight(1F)) {
            NotiDrawer(context, drawerViewModel)
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.BottomCenter
//            ) {
//                TestCard(gptViewModel)
//            }
        }
        Column(Modifier.fillMaxWidth()) {
            Text("Control Panel", Modifier.align(Alignment.CenterHorizontally))
            FlowRow (
                Modifier.wrapContentHeight().fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
//                Button(onClick = {
//                    Toast.makeText(context, "Start Summarizing", Toast.LENGTH_SHORT).show()
//                    gptViewModel.summarizeNotis()
//                }) {
//                    Text("Summarize")
//                }
//                Button(onClick = {
//                    Toast.makeText(context, "Start Sorting", Toast.LENGTH_SHORT).show()
//                    gptViewModel.sortNotis()
//                }) {
//                    Text("Sort")
//                }
//                Button(onClick = {
//                    Toast.makeText(context, "Start Categorizing", Toast.LENGTH_SHORT).show()
//                    gptViewModel.getCategories()
//                }) {
//                    Text("Classify")
//                }
//                Button(onClick = {
//                    Toast.makeText(context, "Work In Progress", Toast.LENGTH_SHORT).show()
//                }) {
//                    Text("Extract Tasks")
//                }
                Button(onClick = {
                    drawerViewModel.getPostContent(true)
                }) {
                    Text("Copy Data with History")
                }
                Button(onClick = {
                    drawerViewModel.getPostContent(false)
                }) {
                    Text("Copy Data")
                }
                Button(onClick = {
                    drawerViewModel.resetGPTValues()
                    Toast.makeText(context, "GPT Values Reset", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Reset")
                }
                Button(onClick = {
                    drawerViewModel.deleteAllNotis()
                    Toast.makeText(context, "All notifications deleted", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Clear")
                }
            }
        }
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