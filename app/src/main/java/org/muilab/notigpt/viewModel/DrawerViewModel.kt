package org.muilab.notigpt.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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
import org.muilab.notigpt.util.postOngoingNotification
import org.muilab.notigpt.util.replaceChars

class DrawerViewModel(
    application: Application,
    notiRepository: NotiRepository
) : AndroidViewModel(application) {
    val allPaged: Flow<PagingData<NotiUnit>> = notiRepository.getAllPaged()
    val notSeenCount: LiveData<Int> = notiRepository.notSeenCount

    @SuppressLint("StaticFieldLeak")
    val context: Context = getApplication<Application>().applicationContext

    @RequiresApi(Build.VERSION_CODES.S)
    fun actOnNoti(notiUnit: NotiUnit, action: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerDatabase = DrawerDatabase.getInstance(context)
            val drawerDao = drawerDatabase.drawerDao()
            val existingNoti = drawerDao.getBySbnKey(notiUnit.sbnKey)
            if (existingNoti.isNotEmpty()) {
                when (action) {
                    "swipe_dismiss" -> existingNoti[0].hideNoti()
                    "click_dismiss" -> existingNoti[0].hideNoti()
                    "pin" -> existingNoti[0].flipNotiPin()
                }
                drawerDao.update(existingNoti[0])
            }
            if (action.contains("dismiss")) {
                postOngoingNotification(context)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun deleteAllNotis() {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerDatabase = DrawerDatabase.getInstance(context)
            val drawerDao = drawerDatabase.drawerDao()
            drawerDao.deleteAllNotPinned()
            postOngoingNotification(context)
        }
    }

    fun resetGPTValues() {
        viewModelScope.launch(Dispatchers.IO) {
            val drawerDatabase = DrawerDatabase.getInstance(context)
            val drawerDao = drawerDatabase.drawerDao()
            val notifications = drawerDao.getAllVisible()
            for (noti in notifications)
                noti.resetGPTValues()
            drawerDao.updateList(notifications)
        }
    }

    // For testing
    val notiPostContent = MutableLiveData<String>()
    fun getPostContent(includeContext: Boolean) {

        viewModelScope.launch(Dispatchers.IO) {
            val notifications = getNotifications(context)
            val sb = StringBuilder()
            notifications.forEach { noti ->

                sb.append("<whole_noti>\n\n")

                sb.append("<id>${noti.hashKey}</id>\n")
                // First Line: App & Title (If title is consistent)
                sb.append("<app>${noti.appName}</app>\n")

                val titlesIdentical = (noti.notiInfos + noti.prevNotiInfos)
                    .map { it.title }
                    .filter { it.isNotBlank() }
                    .toSet().size == 1
                val notiType = if (noti.isPeople) "message" else "info"
                val notiTypeTitle = if (noti.isPeople) "sender" else "title"

                sb.append("<overall_$notiTypeTitle>${replaceChars(noti.title)}</overall_$notiTypeTitle>\n\n")

                // Optional: Include previous notifications
                val prevNotiInfos = noti.prevNotiInfos

                if (includeContext && prevNotiInfos.size > 0) {
                    sb.append("<previous_${notiType}s\n")
                    prevNotiInfos.forEach {
                        sb.append("<$notiType>\n")
                        sb.append("<time>${getDisplayTimeStr(it.time)}</time>")
                        if (!titlesIdentical)
                            sb.append("<$notiTypeTitle>${replaceChars(it.title)}</$notiTypeTitle>")
                        sb.append("<content>${replaceChars(it.content)}</content>\n")
                        sb.append("<$notiType>\n")
                    }
                    sb.append("</previous_${notiType}s>\n\n")
                    sb.append("<new_${notiType}s>\n")
                }

                // Second Line Onwards: Time, (Title, ) Content
                val notiInfos = noti.notiInfos

                notiInfos.forEach {
                    sb.append("<$notiType>\n")
                    sb.append("<time>${getDisplayTimeStr(it.time)}</time>")
                    if (!titlesIdentical)
                        sb.append("<$notiTypeTitle>${replaceChars(it.title)}</$notiTypeTitle>")
                    sb.append("<content>${replaceChars(it.content)}</content>\n")
                    sb.append("<$notiType>\n")
                }
                if (includeContext && prevNotiInfos.size > 0)
                    sb.append("</new_${notiType}s>\n")
                sb.append("\n</whole_noti>\n\n\n")
            }
            notiPostContent.postValue(sb.toString())
        }
    }
}