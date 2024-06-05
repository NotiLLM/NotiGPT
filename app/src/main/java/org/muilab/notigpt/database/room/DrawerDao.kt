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

    @Query("SELECT COUNT(*) FROM noti_drawer WHERE notiVisible = 1")
    fun getAllNotiCount(): Int

    @Query("SELECT COUNT(*) FROM noti_drawer WHERE notiVisible = 1 AND notiSeen = 0")
    fun getNotiNotSeenCount(): Int

    @Query("SELECT COUNT(*) FROM noti_drawer WHERE notiVisible = 1 AND notiSeen = 1 AND pinned = 1")
    fun getNotiPinnedSeenCount(): Int

    @Query("""
        SELECT * FROM noti_drawer WHERE notiVisible = 1 
        ORDER BY 
            CASE 
                /* old logic
                WHEN pinned = 1 AND notiSeen = 1 THEN 1
                WHEN pinned = 1 AND notiSeen = 0 THEN 2
                WHEN pinned = 0 AND notiSeen = 0 THEN 3
                WHEN pinned = 0 AND notiSeen = 1 THEN 4
                */
                WHEN notiSeen = 0 THEN 1
                WHEN notiSeen = 1 THEN 2
            END,
            score DESC,
            importance DESC,
            latestTime DESC
    """)
    fun getAllVisible(): List<NotiUnit>

    @Query("""
        SELECT * FROM noti_drawer WHERE notiVisible = 1 
        ORDER BY 
            CASE 
                /* old logic
                WHEN pinned = 1 AND notiSeen = 1 THEN 1
                WHEN pinned = 1 AND notiSeen = 0 THEN 2
                WHEN pinned = 0 AND notiSeen = 0 THEN 3
                WHEN pinned = 0 AND notiSeen = 1 THEN 4
                */
                WHEN notiSeen = 0 THEN 1
                WHEN notiSeen = 1 THEN 2
            END,
            score DESC,
            importance DESC,
            latestTime DESC
    """)
    fun getAllVisiblePaged(): PagingSource<Int, NotiUnit>

    @Query(
        """
        SELECT * FROM noti_drawer WHERE notiVisible = 1 AND notiSeen = 0
        ORDER BY isPeople DESC, importance DESC, LENGTH(notiInfos) DESC, latestTime DESC
    """
    )
    fun getnotReadNotis(): List<NotiUnit>

    @Query(
        """
        SELECT * FROM noti_drawer WHERE notiVisible = 1 AND isPeople = 1
        ORDER BY appName, isPeople DESC, importance DESC, LENGTH(notiInfos) DESC, latestTime DESC
    """
    )
    fun getNotiWithSenders(): List<NotiUnit>

    @Query("SELECT * FROM noti_drawer WHERE sbnKey = :sbnKey")
    fun getBySbnKey(sbnKey: String): List<NotiUnit>

    @Query("SELECT * FROM noti_drawer WHERE sbnKey in (:sbnKeys)")
    fun getBySbnKeys(sbnKeys: List<String>): List<NotiUnit>

    @Query("SELECT * FROM noti_drawer WHERE hashKey = :hashKey")
    fun getByHashKey(hashKey: Int): List<NotiUnit>

    @Query("DELETE FROM noti_drawer WHERE sbnKey = :sbnKey")
    fun deleteBySbnKey(sbnKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(notiUnit: NotiUnit)

    @Update
    fun update(notiUnit: NotiUnit)

    @Update
    fun updateList(notiUnit: List<NotiUnit>)

    @Query("DELETE FROM noti_drawer")
    fun deleteAll()

    @Query("DELETE FROM noti_drawer WHERE pinned <> 1")
    fun deleteAllNotPinned()
}