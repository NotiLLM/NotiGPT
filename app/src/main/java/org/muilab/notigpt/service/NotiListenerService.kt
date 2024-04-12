package org.muilab.notigpt.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.NotiUnit

class NotiListenerService: NotificationListenerService() {

    companion object {
        val pendingIntents = mutableMapOf<String, PendingIntent>()

        fun getPendingIntent(context: Context, notiUnit: NotiUnit): PendingIntent? {
            val sbnKey = notiUnit.sbnKey
            if (sbnKey in pendingIntents)
                return pendingIntents[sbnKey]

            val launchIntent = context.packageManager.getLaunchIntentForPackage(notiUnit.pkgName)
            return if (launchIntent != null)
                PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
            else
                null
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications.forEach {
            addNotification(it, currentRanking, true)
        }
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, NotiListenerService::class.java))
        try {

        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onListenerDisconnected()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        val restartServiceIntent = Intent(applicationContext, NotiListenerService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        getSystemService(Context.ALARM_SERVICE)
        val alarmService: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + 2000, restartServicePendingIntent)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        addNotification(sbn, rankingMap, false)
    }

    private fun addNotification(sbn: StatusBarNotification, rankingMap: RankingMap, isInit: Boolean) {
        fun isMessageNotification(sbn: StatusBarNotification): Boolean {
            val notification = sbn.notification
            return (notification.extras.get(Notification.EXTRA_MESSAGES) != null
                    || notification.extras.get(Notification.EXTRA_HISTORIC_MESSAGES) != null
                    || notification.extras.get(Notification.EXTRA_MESSAGING_PERSON) != null
                    || notification.category == Notification.CATEGORY_MESSAGE)
        }

        if ((sbn.notification?.flags as Int and Notification.FLAG_GROUP_SUMMARY) > 0)
            return
        CoroutineScope(Dispatchers.IO).launch {
            val drawerDatabase = DrawerDatabase.getInstance(applicationContext)
            val drawerDao = drawerDatabase.drawerDao()
            val existingNoti = drawerDao.getBySbnKey(sbn.key)
            if (existingNoti.isEmpty()) {
                drawerDao.insert(NotiUnit(applicationContext, sbn, rankingMap))
            } else if (!isInit) {
                if (!existingNoti[0].isVisible())
                    existingNoti[0].makeVisible(isMessageNotification(sbn))
                existingNoti[0].updateNoti(applicationContext, sbn, rankingMap, isMessageNotification(sbn))
                drawerDao.update(existingNoti[0])
            }
            rankingMap.orderedKeys.forEachIndexed { idx, key ->
                val noti = drawerDao.getBySbnKey(key)
                if (noti.isEmpty())
                    return@forEachIndexed
                noti[0].ranking = idx
                drawerDao.update(noti[0])
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d("RemoveNoti", sbn.packageName)
        CoroutineScope(Dispatchers.IO).launch {
            val drawerDatabase = DrawerDatabase.getInstance(applicationContext)
            val drawerDao = drawerDatabase.drawerDao()
            val existingNoti = drawerDao.getBySbnKey(sbn.key)
            if (existingNoti.isNotEmpty()) {
                existingNoti[0].hideNoti()
                drawerDao.update(existingNoti[0])
            }
        }
    }

}