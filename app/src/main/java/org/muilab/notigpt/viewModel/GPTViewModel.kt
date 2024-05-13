package org.muilab.notigpt.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.service.GPTService

class GPTViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    @SuppressLint("StaticFieldLeak")
    private lateinit var gptService: GPTService

    private val _response = MutableLiveData<String>()
    val response: LiveData<String> = _response

    fun setService(gptService: GPTService) {
        this.gptService = gptService
    }
    fun getCategories() {
        viewModelScope.launch {
            val responseStr = gptService.makeRequest("categorize").trimIndent()
            val notiCategories = responseStr.split("\n")
            _response.postValue(responseStr)

            CoroutineScope(Dispatchers.IO).launch {
                val drawerDatabase = DrawerDatabase.getInstance(context)
                val drawerDao = drawerDatabase.drawerDao()
                val updateNotis = mutableListOf<NotiUnit>()
                for (notiCategory in notiCategories) {
                    val segments = notiCategory.trim().split(" ")
                    if (segments.size == 2) {
                        try {
                            val hashKey = segments[0].toInt()
                            val newGPTCategory = segments[1]
                            val existingNoti = drawerDao.getByHashKey(hashKey)
                            if (existingNoti.isNotEmpty())
                                updateNotis.add(existingNoti[0].copy(gptCategory = newGPTCategory))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                drawerDao.updateList(updateNotis)
            }
            Toast.makeText(context, "Categorize Complete", Toast.LENGTH_SHORT).show()
        }
    }

    fun sortNotis() {
        viewModelScope.launch {
            val responseStr = gptService.makeRequest("sort").trimIndent()
            val notiCategories = responseStr.split("\n")
            _response.postValue(responseStr)

            CoroutineScope(Dispatchers.IO).launch {
                val drawerDatabase = DrawerDatabase.getInstance(context)
                val drawerDao = drawerDatabase.drawerDao()
                val updateNotis = mutableListOf<NotiUnit>()
                for (notiCategory in notiCategories) {
                    val segments = notiCategory.trim().split(" ")
                    if (segments.size == 2) {
                        try {
                            val hashKey = segments[0].toInt()
                            val newScore = segments[1].toDouble()
                            val existingNoti = drawerDao.getByHashKey(hashKey)
                            if (existingNoti.isNotEmpty())
                                updateNotis.add(
                                    existingNoti[0].copy(
                                        score = newScore
                                    )
                                )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                drawerDao.updateList(updateNotis)
            }
            Toast.makeText(context, "Sort Complete", Toast.LENGTH_SHORT).show()
        }
    }

    fun summarizeNotis() {
        viewModelScope.launch {
            val responseStr = gptService.makeRequest("summarize").trimIndent()
            val notiCategories = responseStr.split("\n")
            _response.postValue(responseStr)

            CoroutineScope(Dispatchers.IO).launch {
                val drawerDatabase = DrawerDatabase.getInstance(context)
                val drawerDao = drawerDatabase.drawerDao()
                val updateNotis = mutableListOf<NotiUnit>()
                for (notiCategory in notiCategories) {
                    val segments = notiCategory.trim().split(" ", limit = 2)
                    try {
                        val hashKey = segments[0].toInt()
                        val newSummary = segments[1]
                        val existingNoti = drawerDao.getByHashKey(hashKey)
                        if (existingNoti.isNotEmpty())
                            updateNotis.add(existingNoti[0].copy(summary = newSummary))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                drawerDao.updateList(updateNotis)
            }
            Toast.makeText(context, "Summarization Complete", Toast.LENGTH_SHORT).show()
        }
    }
}