package org.muilab.notigpt.database.room

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.muilab.notigpt.model.NotiUnit

@Dao
interface DrawerDao {

    @Query("SELECT * FROM noti_drawer ORDER BY ranking ")
    fun getAll(): List<NotiUnit>

    @Query("SELECT * FROM noti_drawer ORDER BY ranking")
    fun getAllPaged(): PagingSource<Int, NotiUnit>

    @Query("SELECT * FROM noti_drawer WHERE sbnKey = :sbnKey")
    fun getBySbnKey(sbnKey: String): List<NotiUnit>

    @Query("DELETE FROM noti_drawer WHERE sbnKey = :sbnKey")
    fun deleteBySbnKey(sbnKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(notiUnit: NotiUnit)

    @Update
    fun update(notiUnit: NotiUnit)

    @Query("DELETE FROM noti_drawer")
    fun deleteAll()
}