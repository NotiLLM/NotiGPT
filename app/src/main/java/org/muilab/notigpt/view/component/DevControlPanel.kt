package org.muilab.notigpt.view.component

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.muilab.notigpt.viewModel.DrawerViewModel
import org.muilab.notigpt.viewModel.GeminiViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevControlPanel(context: Context, drawerViewModel: DrawerViewModel, geminiViewModel: GeminiViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Text("Control Panel", Modifier.align(Alignment.CenterHorizontally))
        FlowRow (
            Modifier.wrapContentHeight().fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
                Button(onClick = {
                    Toast.makeText(context, "Start Summarizing", Toast.LENGTH_SHORT).show()
                    geminiViewModel.summarizeNotis()
                }) {
                    Text("Summarize")
                }
                Button(onClick = {
                    Toast.makeText(context, "Start Sorting", Toast.LENGTH_SHORT).show()
                    geminiViewModel.sortNotis()
                }) {
                    Text("Sort")
                }
//                Button(onClick = {
//                    Toast.makeText(context, "Start Categorizing", Toast.LENGTH_SHORT).show()
//                    geminiViewModel.getCategories()
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
//            Button(onClick = {
//                drawerViewModel.resetGPTValues()
//                Toast.makeText(context, "GPT Values Reset", Toast.LENGTH_SHORT).show()
//            }) {
//                Text("Reset")
//            }
        }
    }
}