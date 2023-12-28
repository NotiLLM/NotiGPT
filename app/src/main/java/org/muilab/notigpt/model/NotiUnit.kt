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
    val groupKey: String,
    val isAppGroup: Boolean,
    var appName: String = "Unknown App",
    var icon: String = "Unknown Icon",
    // Modifiable
    var sortKey: String,
    var ranking: Int = -1,
    //  Accumulatable
    val `when`: ArrayList<Long>,
    val postTime: ArrayList<Long>,
    var title: ArrayList<String> = arrayListOf(),
    var content: ArrayList<String> = arrayListOf(),
) {

    constructor(context: Context, sbn: StatusBarNotification, rankingMap: RankingMap): this(
        pkgName = sbn.opPkg,
        category = sbn.notification?.category ?: "Unknown",
        sbnKey = sbn.key,
        groupKey = sbn.notification?.group.toString(),
        isAppGroup = sbn.isGroup,
        sortKey = sbn.notification?.sortKey.toString(),
        ranking = rankingMap.orderedKeys.indexOf(sbn.key),
        `when` = arrayListOf(sbn.notification?.`when` as Long),
        postTime = arrayListOf(sbn.postTime)
    ) {
        contentInit(context, sbn)
        updateNoti(sbn, rankingMap)
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

        // icon
        val iconObject = sbn.notification?.getLargeIcon()?: sbn.notification.smallIcon
        icon = iconToBase64(context, pm, iconObject)
    }

    fun updateNoti(sbn: StatusBarNotification, rankingMap: RankingMap, update: Boolean = false) {

        fun replaceChars(str: String): String {
            // return str.replace("\n", " ").replace(",", " ")
            return str
        }

        // sort variables
        sortKey = sbn.notification?.sortKey.toString()
        ranking = rankingMap.orderedKeys.indexOf(sbn.key)

        // time
        `when`.add(sbn.notification?.`when` as Long)
        if (!update)
            postTime.clear()
        postTime.add(sbn.postTime)

        // title
        val notiTitle = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE).toString()
        if (!update)
            title.clear()
        title.add(replaceChars(notiTitle))

        // content
        var notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        if (notiContent == "null")
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT).toString()
        if (notiContent == "null")
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString()
        if (notiContent == "null")
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_INFO_TEXT).toString()
        if (notiContent == "null")
            notiContent = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT).toString()
        if (!update)
            content.clear()
        content.add(replaceChars(notiContent))

        // save pending intent
        val pendingIntent = sbn.notification?.contentIntent
        if (sbn.notification?.contentIntent != null)
            NotiListenerService.pendingIntents[sbnKey] = pendingIntent as PendingIntent
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
        val byteArray = Base64.decode(iconStr, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun getBitmap(): Bitmap? {
        return if (icon != "null")
            base64ToBitmap(icon)
        else
            null
    }

    private fun unixTimeToStr(unixTime: Long): String {
        val simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(unixTime)
        return simpleDateFormat.format(date)
    }

    fun getLastTime(): String {
        return if (`when`.last() != 0L)
            unixTimeToStr(`when`.last())
        else
            unixTimeToStr(postTime.last())
    }
}