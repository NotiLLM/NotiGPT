package org.muilab.notigpt.database.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.muilab.notigpt.model.notifications.NotiTypeConverters
import org.muilab.notigpt.model.notifications.NotiUnit

@Database(entities = [NotiUnit::class], version = 1)
@TypeConverters(NotiTypeConverters::class)
abstract class DrawerDatabase : RoomDatabase() {

    abstract fun drawerDao(): DrawerDao

    companion object {
        @Volatile
        private var INSTANCE: DrawerDatabase? = null

        fun getInstance(context: Context): DrawerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext, DrawerDatabase::class.java, "noti_drawer")
            .build()
    }
}