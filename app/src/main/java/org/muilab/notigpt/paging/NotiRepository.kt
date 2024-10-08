package org.muilab.notigpt.paging

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import org.muilab.notigpt.database.room.DrawerDao
import org.muilab.notigpt.model.notifications.NotiUnit

class NotiRepository(private val drawerDao: DrawerDao) {

    fun getNotificationsFlow(): Flow<List<NotiUnit>> {
        return drawerDao.getAllVisibleFlow()
    }

    val notSeenCount: LiveData<Int> = drawerDao.getNotiNotSeenCount()
}