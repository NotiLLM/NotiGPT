package org.muilab.notigpt.paging

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.muilab.notigpt.database.room.DrawerDao
import org.muilab.notigpt.model.NotiUnit

class NotiRepository(private val drawerDao: DrawerDao) {
    fun getAllPaged(): Flow<PagingData<NotiUnit>> {
        return Pager(
            PagingConfig(pageSize = 40, enablePlaceholders = false)
        ) {
            drawerDao.getAllVisiblePaged()
        }.flow
    }

    val notSeenCount: LiveData<Int> = drawerDao.getNotiNotSeenCount()
}