package org.muilab.notigpt.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.paging.NotiRepository
import org.muilab.notigpt.util.getDisplayTimeStr
import org.muilab.notigpt.util.getNotifications

class DrawerViewModel(
    application: Application,
    notiRepository: NotiRepository
) : AndroidViewModel(application) {
    val allPaged: Flow<PagingData<NotiUnit>> = notiRepository.getAllPaged()

    @SuppressLint("StaticFieldLeak")
    val context: Context = getApplication<Application>().applicationContext

    fun deleteNoti(notiUnit: NotiUnit) {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerDatabase = DrawerDatabase.getInstance(context)
            val drawerDao = drawerDatabase.drawerDao()
            drawerDao.deleteBySbnKey(notiUnit.sbnKey)
        }
    }

    fun deleteAllNotis() {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerDatabase = DrawerDatabase.getInstance(context)
            val drawerDao = drawerDatabase.drawerDao()
            drawerDao.deleteAll()
        }
    }

    fun resetGPTValues() {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerDatabase = DrawerDatabase.getInstance(context)
            val drawerDao = drawerDatabase.drawerDao()
            val notifications = drawerDao.getAll()
            for (noti in notifications)
                noti.resetGPTValues()
            drawerDao.updateList(notifications)
        }
    }

    // For testing
    val notiPostContent = MutableLiveData<String>()
    fun getPostContent() {

        fun replaceChars(str: String): String {
            return str.replace("\n", " ").replace(",", " ")
        }

        viewModelScope.launch(Dispatchers.IO) {
            val notifications = getNotifications(context)
            val sb = StringBuilder()
            notifications.forEach { noti ->
                sb.append("[App] ${noti.appName}")
                val titlesIdentical = noti.title.toSet().size == 1
                if (titlesIdentical)
                    sb.append(" [Title] ${replaceChars(noti.title.last())}")
                sb.append("\n")
                val threadLength = if (titlesIdentical)
                    minOf(noti.content.size, noti.`when`.size, noti.postTime.size)
                else
                    minOf(noti.title.size, noti.content.size, noti.`when`.size, noti.postTime.size)
                val notiTitle = noti.title.takeLast(threadLength)
                val notiContent = noti.content.takeLast(threadLength)
                val notiTime = if (noti.`when`.last() == 0L)
                    noti.postTime.takeLast(threadLength)
                else
                    noti.`when`.takeLast(threadLength)
                if (titlesIdentical)
                    for (i in 0..<threadLength)
                        sb.append("[Time] ${getDisplayTimeStr(notiTime[i])} [Content] ${replaceChars(notiContent[i])}\n")
                    else
                        for (i in 0..<threadLength)
                            sb.append("[Time] ${getDisplayTimeStr(notiTime[i])} [Title] ${replaceChars(notiTitle[i])} [Content] ${replaceChars(notiContent[i])}\n")
                sb.append("\n")
            }
            notiPostContent.postValue(sb.toString())
        }
    }
}