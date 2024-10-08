package org.muilab.notigpt.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.gemini.SortOutcome
import org.muilab.notigpt.model.gemini.SummaryOutcome
import org.muilab.notigpt.model.notifications.NotiUnit
import org.muilab.notigpt.service.GeminiService
import java.math.RoundingMode

class GeminiViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    @SuppressLint("StaticFieldLeak")
    private lateinit var geminiService: GeminiService

    private val _response = MutableLiveData<String>()
    val response: LiveData<String> = _response

    fun setService(geminiService: GeminiService) {
        this.geminiService = geminiService
    }

    fun sortNotis() {
        viewModelScope.launch {
            try {
                val responseStr = geminiService.makeRequest("sort")
                Log.d("Gemini", responseStr)
                _response.postValue(responseStr)

                val listType = object : TypeToken<List<SortOutcome>>() {}.type
                val outcomeList: List<SortOutcome> = Gson().fromJson(responseStr, listType)

                CoroutineScope(Dispatchers.IO).launch {
                    val drawerDatabase = DrawerDatabase.getInstance(context)
                    val drawerDao = drawerDatabase.drawerDao()
                    val updateNotis = mutableListOf<NotiUnit>()

                    outcomeList.forEach {
                        val hashKey = it.id
                        val score = (it.timeSensitiveness + it.senderAttractiveness + it.contentAttractiveness)
                            .toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
                        val existingNoti = drawerDao.getByHashKey(hashKey)
                        if (existingNoti.isNotEmpty())
                            updateNotis.add(
                                existingNoti[0].copy(
                                    outcome = existingNoti[0].outcome.copy(score = score)
                                )
                            )
                    }
                    drawerDao.updateList(updateNotis)
                }
                Toast.makeText(context, "Sort Complete", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun summarizeNotis() {
        viewModelScope.launch {
            try {
                val responseStr = geminiService.makeRequest("summarize")
                Log.d("Gemini", responseStr)
                _response.postValue(responseStr)

                val listType = object : TypeToken<List<SummaryOutcome>>() {}.type
                val outcomeList: List<SummaryOutcome> = Gson().fromJson(responseStr, listType)

                CoroutineScope(Dispatchers.IO).launch {
                    val drawerDatabase = DrawerDatabase.getInstance(context)
                    val drawerDao = drawerDatabase.drawerDao()
                    val updateNotis = mutableListOf<NotiUnit>()

                    outcomeList.forEach {
                        val hashKey = it.id
                        val summary = it.summary
                        val existingNoti = drawerDao.getByHashKey(hashKey)
                        if (existingNoti.isNotEmpty())
                            updateNotis.add(
                                existingNoti[0].copy(
                                    outcome = existingNoti[0].outcome.copy(summary = summary)
                                )
                            )
                    }
                    drawerDao.updateList(updateNotis)
                }
                Toast.makeText(context, "Summarization Complete", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}