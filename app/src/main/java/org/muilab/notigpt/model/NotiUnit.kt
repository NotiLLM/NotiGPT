package org.muilab.notigpt.model

import android.app.Notification
import android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.room.Entity
import org.muilab.notigpt.service.NotiListenerService
import org.muilab.notigpt.util.Constants
import org.muilab.notigpt.util.getDisplayTimeStr
import java.io.ByteArrayOutputStream
import java.util.TreeSet


@Entity(tableName = "noti_drawer", primaryKeys = ["sbnKey"])
data class NotiUnit(
    // Fixed (On Init)
    val pkgName: String,
    val category: String,
    val sbnKey: String,
    val hashKey: Int,
    val groupKey: String,
    val isAppGroup: Boolean,
    var appName: String = "Unknown App",
    val isGroupChat: Boolean,
    // Modifiable Visible
    var latestTime: Long = 0L,
    var title: String = "",
    var icon: String = "Unknown Icon",
    var largeIcon: String = "Unknown Icon",
    var gptCategory: String = "",
    var summary: String = "",
    // Modifiable Invisible
    var notiVisible: Boolean = true,
    var notiSeen: Boolean = false,
    var people: MutableSet<String> = mutableSetOf(),
    var isPeople: Boolean,
    var sortKey: String,
    var importance: Int = -1,
    var score: Double = 100.0,
    //  Accumulatable
    val notiInfos: TreeSet<NotiInfo> = TreeSet(compareBy<NotiInfo> { it.time }),
    val prevNotiInfos: TreeSet<NotiInfo> = TreeSet(compareBy<NotiInfo> { it.time }),
    // User Input
    var pinned: Boolean = false
) {

    companion object {
        @RequiresApi(Build.VERSION_CODES.S)
        fun fetchIsPeople(sbn: StatusBarNotification): Boolean {
            val notification = sbn.notification
            val messagingApps = setOf(
                "jp.naver.line.android"
            )
            return (notification.extras.get(Notification.EXTRA_MESSAGES) != null
                    || notification.extras.get(Notification.EXTRA_HISTORIC_MESSAGES) != null
                    || notification.extras.get(Notification.EXTRA_MESSAGING_PERSON) != null
                    || notification.extras.get(Notification.EXTRA_CALL_PERSON) != null
                    || notification.extras.get(Notification.EXTRA_PEOPLE_LIST)
                        .let { it != null && (it as ArrayList<*>).isNotEmpty() }
                    || notification.category == Notification.CATEGORY_MESSAGE
                    || notification.category == Notification.CATEGORY_CALL
                    || notification.category == Notification.CATEGORY_MISSED_CALL)
        }

        fun fetchImportance(context: Context, sbn: StatusBarNotification): Int {
            val notificationManager: NotificationManager = context.getSystemService(
                NotificationListenerService.NOTIFICATION_SERVICE
            ) as NotificationManager
            val channel = notificationManager.getNotificationChannel(sbn.notification.channelId)
            return channel?.importance ?: 0
        }

        private fun fetchTitle(sbn: StatusBarNotification, isPeople: Boolean): String {

            if (isPeople) {
                val conversationTitle = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                if (conversationTitle != null)
                    return conversationTitle.toString()
            }

            return sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE_BIG).let { bigTitle ->
                bigTitle?.toString() ?: sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE).let { title ->
                    title?.toString() ?: ""
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun fetchPeople(sbn: StatusBarNotification): List<String> {
            val fromExtras = sbn.notification?.extras?.get(Notification.EXTRA_PEOPLE_LIST).let { peopleList ->
                if (peopleList == null)
                    arrayListOf()
                else {
                    (peopleList as ArrayList<Person>)
                        .map { it.name.toString() }
                }
            }
            val fromMessages = sbn.notification?.extras?.getParcelableArray(Notification.EXTRA_MESSAGES).let { peopleParcelable ->
                if (peopleParcelable == null)
                    arrayListOf()
                else {
                    getMessagesFromBundleArray(peopleParcelable)
                        .mapNotNull { it.senderPerson?.name }
                        .map { it.toString() }
                }
            }
            val fromHistoricMessages = sbn.notification?.extras?.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES).let { peopleParcelable ->
                if (peopleParcelable == null)
                    arrayListOf()
                else {
                    getMessagesFromBundleArray(peopleParcelable)
                        .mapNotNull { it.senderPerson?.name }
                        .map { it.toString() }
                }
            }
            return fromExtras + fromMessages + fromHistoricMessages
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    constructor(
        context: Context,
        sbn: StatusBarNotification
    ): this(
        pkgName = sbn.opPkg,
        category = sbn.notification?.category ?: "Unknown",
        sbnKey = sbn.key,
        hashKey = sbn.key.hashCode(),
        groupKey = sbn.notification?.group.toString(),
        isAppGroup = sbn.isGroup,
        isGroupChat = sbn.notification?.extras?.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION) ?: false,
        sortKey = sbn.notification?.sortKey.toString(),
        importance = fetchImportance(context, sbn),
        isPeople = fetchIsPeople(sbn),
        latestTime = if (sbn.notification?.`when` as Long > 0)
            sbn.notification?.`when` as Long
        else
            sbn.postTime
    ) {
        contentInit(context, sbn)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun contentInit(context: Context, sbn: StatusBarNotification) {

        // appName
        val pm = context.packageManager
        val applicationInfo: ApplicationInfo? =
            sbn.packageName?.let {
                try {
                    if (Build.VERSION.SDK_INT >= TIRAMISU) {
                        pm.getApplicationInfo(it, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        pm.getApplicationInfo(it, 0)
                    }
                } catch(e: Exception) {
                    null
                }
            }
        appName = (if (applicationInfo != null) {
            pm.getApplicationLabel(applicationInfo).toString()
        } else {
            pkgName
        })

        updateNoti(context, sbn)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun updateNoti(context: Context, sbn: StatusBarNotification) {

        // sort variables
        this.isPeople = this.isPeople || fetchIsPeople(sbn)
        this.importance = fetchImportance(context, sbn)
        sortKey = sbn.notification?.sortKey.toString()

        // icon
        val pm = context.packageManager
        icon = iconToBase64(context, pm, sbn.notification.smallIcon)
        val bigiconObject = sbn.notification.getLargeIcon()
        largeIcon = iconToBase64(context, pm, bigiconObject)
        if (largeIcon == "null")
            largeIcon = icon

        // save pending intent
        val pendingIntent = sbn.notification?.contentIntent
        if (sbn.notification?.contentIntent != null)
            NotiListenerService.pendingIntents[sbnKey] = pendingIntent as PendingIntent

        title = fetchTitle(sbn, isPeople)
        people.addAll(fetchPeople(sbn))
        val notiInfo = NotiInfo(sbn)
        if (isPeople) {
            if (!updatePreviousMessagesSuccess(sbn))
                notiInfos.add(notiInfo)
        } else {
            notiInfos.clear()
            notiInfos.add(notiInfo)
        }
        updateLatestTime()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun updatePreviousMessagesSuccess(sbn: StatusBarNotification): Boolean {
        var hasExtra: Boolean = false

        val extraMessages = sbn.notification?.extras?.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (extraMessages != null) {
            getMessagesFromBundleArray(extraMessages)
                .forEach {
                    if (it.senderPerson != null) {
                        notiInfos.add(NotiInfo(it.timestamp, it.senderPerson?.name.toString(), it.text.toString()))
                        hasExtra = true
                    }
                }
        }
        val extraHistoricMessages = sbn.notification?.extras?.getParcelableArray(Notification.EXTRA_HISTORIC_MESSAGES)
        if (extraMessages != null) {
            getMessagesFromBundleArray(extraHistoricMessages)
                .forEach {
                    if (it.senderPerson != null) {
                        notiInfos.add(NotiInfo(it.timestamp, it.senderPerson?.name.toString(), it.text.toString()))
                        hasExtra = true
                    }
                }
        }

        return hasExtra
    }

    private fun iconToBitmap(context: Context, icon: Icon): Bitmap? {
        val drawable = icon.loadDrawable(context)
        if (drawable is BitmapDrawable)
            return drawable.bitmap

        val width = drawable!!.intrinsicWidth
        val height = drawable.intrinsicHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun iconToBase64(context: Context, pm: PackageManager, icon: Icon?): String {
        val bitmap = try {
            iconToBitmap(context, icon!!)
        } catch (e: Exception) {
            try {
                pm.getApplicationIcon(pkgName).toBitmap()
            } catch (e: Exception) {
                null
            }
        } ?: return "null"
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun base64ToBitmap(iconStr: String): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val byteArray = Base64.decode(iconStr, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
    }

    fun getLargeBitmap(): Bitmap? {
        return if (largeIcon != "null")
            base64ToBitmap(largeIcon)
        else {
            getBitmap()
        }
    }

    fun getBitmap(): Bitmap? {
        return if (icon != "null")
            base64ToBitmap(icon)
        else
            null
    }

    private fun updateLatestTime() {
        latestTime = notiInfos.last().time
    }

    fun getLatestTimeStr(): String {
        return getDisplayTimeStr(latestTime)
    }

    fun flipNotiPin() {
        pinned = !pinned
    }

    fun resetGPTValues() {
        summary = ""
        gptCategory = ""
        score = 30.0
    }

    fun hideNoti() {
        prevNotiInfos.addAll(notiInfos)
        notiInfos.clear()
        notiVisible = false
        notiSeen = false
    }

    fun isVisible(): Boolean {
        return notiVisible
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun makeVisible(sbn: StatusBarNotification) {
        notiVisible = true
        notiSeen = false
        this.isPeople = this.isPeople || fetchIsPeople(sbn)
        if (isPeople) {
            keepHistoryByCount(Constants.HISTORY_NOTI_COUNT_THRESHOLD)
            dropHistoryByTime(Constants.HISTORY_NOTI_TIME_THRESHOLD)
        } else
            deleteHistory()
    }

    fun deleteHistory() {
        prevNotiInfos.clear()
    }

    private fun keepHistoryByCount(limit: Int) {
        if (limit < 0)
            return

        val iterator = prevNotiInfos.iterator()
        var count = prevNotiInfos.size
        while (iterator.hasNext() && count > limit) {
            iterator.next()
            iterator.remove()
            count--
        }
    }

    private fun dropHistoryByTime(timeThreshold: Long) {
        val subSet = prevNotiInfos.headSet(NotiInfo(timeThreshold), true)
        prevNotiInfos.removeAll(subSet)
    }
}