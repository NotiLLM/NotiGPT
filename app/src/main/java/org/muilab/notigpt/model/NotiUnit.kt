package org.muilab.notigpt.model

import android.app.Notification
import android.app.PendingIntent
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
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Base64
import androidx.core.graphics.drawable.toBitmap
import androidx.room.Entity
import androidx.room.TypeConverters
import org.muilab.notigpt.service.NotiListenerService
import org.muilab.notigpt.util.Constants
import org.muilab.notigpt.util.getDisplayTimeStr
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Entity(tableName = "noti_drawer", primaryKeys = ["sbnKey"])
@TypeConverters(ArrayListTypeConverters::class)
data class NotiUnit(
    // Fixed (On Init)
    val pkgName: String,
    val category: String,
    val sbnKey: String,
    val hashKey: Int,
    val groupKey: String,
    val isAppGroup: Boolean,
    var appName: String = "Unknown App",
    var icon: String = "Unknown Icon",
    var largeIcon: String = "Unknown Icon",
    // Modifiable
    var notiVisible: Boolean = true,
    var sortKey: String,
    var ranking: Int = -1,
    var gptCategory: String = "",
    var summary: String = "",
    var score: Double = 30.0,
    var scoreTime: Double = 10.0,
    var scoreSender: Double = 10.0,
    var scoreContent: Double = 10.0,
    //  Accumulatable
    val `when`: ArrayList<Long>,
    val postTime: ArrayList<Long>,
    var title: ArrayList<String> = arrayListOf(),
    var content: ArrayList<String> = arrayListOf(),
    // Previous Accumulatables
    var prevWhen: ArrayList<Long> = arrayListOf(),
    var prevPostTime: ArrayList<Long> = arrayListOf(),
    var prevTitle: ArrayList<String> = arrayListOf(),
    var prevContent: ArrayList<String> = arrayListOf(),
) {

    constructor(context: Context, sbn: StatusBarNotification, rankingMap: RankingMap): this(
        pkgName = sbn.opPkg,
        category = sbn.notification?.category ?: "Unknown",
        sbnKey = sbn.key,
        hashKey = sbn.key.hashCode(),
        groupKey = sbn.notification?.group.toString(),
        isAppGroup = sbn.isGroup,
        sortKey = sbn.notification?.sortKey.toString(),
        ranking = rankingMap.orderedKeys.indexOf(sbn.key),
        `when` = arrayListOf(sbn.notification?.`when` as Long),
        postTime = arrayListOf(sbn.postTime)
    ) {
        contentInit(context, sbn)
        updateNoti(context, sbn, rankingMap)
    }

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
    }

    fun updateNoti(context: Context, sbn: StatusBarNotification, rankingMap: RankingMap, update: Boolean = false) {

        // sort variables
        sortKey = sbn.notification?.sortKey.toString()
        ranking = rankingMap.orderedKeys.indexOf(sbn.key)

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

        // title
        val notiTitle = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE).toString()
        if (!update)
            title.clear()
        title.add(notiTitle)

        // time
        if (!update)
            `when`.clear()
        `when`.add(sbn.notification?.`when` as Long)
        if (!update)
            postTime.clear()
        postTime.add(sbn.postTime)

        // content
        var notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        if (notiContent == "null" || notiContent.isBlank())
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT).toString()
        if (notiContent == "null" || notiContent.isBlank())
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString()
        if (notiContent == "null" || notiContent.isBlank())
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_INFO_TEXT).toString()
        if (notiContent == "null" || notiContent.isBlank())
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT).toString()
        if (!update)
            content.clear()
        content.add(notiContent)
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

    fun unixTimeToStr(unixTime: Long): String {
        val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(unixTime)
        return simpleDateFormat.format(date)
    }

    fun getLastTime(): String {
        return if (`when`.last() != 0L)
            getDisplayTimeStr(`when`.last())
        else
            getDisplayTimeStr(postTime.last())
    }

    fun resetGPTValues() {
        summary = ""
        gptCategory = ""
        score = 30.0
        scoreTime = 10.0
        scoreSender = 10.0
        scoreContent = 10.0
    }

    fun hideNoti() {
        prevWhen.addAll(`when`)
        `when`.clear()
        prevPostTime.addAll(postTime)
        postTime.clear()
        prevTitle.addAll(title)
        title.clear()
        prevContent.addAll(content)
        content.clear()
        notiVisible = false
    }

    fun isVisible(): Boolean {
        return notiVisible
    }

    fun makeVisible(keepHistory: Boolean) {
        notiVisible = true
        if (keepHistory) {
            keepHistoryByCount(Constants.HISTORY_NOTI_COUNT_THRESHOLD)
            dropHistoryByTime(Constants.HISTORY_NOTI_TIME_THRESHOLD)
        } else
            deleteHistory()
    }

    fun deleteHistory() {
        prevWhen.clear()
        prevPostTime.clear()
        prevTitle.clear()
        prevContent.clear()
    }

    private fun keepHistoryByCount(count: Int) {
        prevWhen = prevWhen.takeLast(count).toCollection(ArrayList())
        prevPostTime = prevPostTime.takeLast(count).toCollection(ArrayList())
        prevTitle = prevTitle.takeLast(count).toCollection(ArrayList())
        prevContent = prevContent.takeLast(count).toCollection(ArrayList())
    }

    private fun dropHistoryByTime(timeThreshold: Long) {
        val currentTime = System.currentTimeMillis()
        val dropCount = if (prevWhen.last() != 0L)
            prevWhen.indexOfFirst { currentTime - it < timeThreshold}
        else
            prevPostTime.indexOfFirst { currentTime - it < timeThreshold}
        prevWhen.drop(dropCount)
        prevPostTime.drop(dropCount)
        prevTitle.drop(dropCount)
        prevContent.drop(dropCount)
    }
}