package org.muilab.notigpt.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.paging.PagingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.NotiUnit
import org.muilab.notigpt.paging.NotiRepository
import java.lang.ref.WeakReference

class DrawerViewModel(
    application: Application,
    notiRepository: NotiRepository
) : AndroidViewModel(application) {
    val allPaged: Flow<PagingData<NotiUnit>> = notiRepository.getAllPaged()
    val context = WeakReference(getApplication<Application>().applicationContext)

    fun deleteNoti(notiUnit: NotiUnit) {
        CoroutineScope(Dispatchers.IO).launch {
            val drawerDatabase = DrawerDatabase.getInstance(context.get()!!)
            val drawerDao = drawerDatabase.drawerDao()
            drawerDao.deleteBySbnKey(notiUnit.sbnKey)
        }
    }
}