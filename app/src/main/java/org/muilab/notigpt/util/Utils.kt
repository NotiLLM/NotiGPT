package org.muilab.notigpt.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.ULocale
import kotlinx.coroutines.Dispatchers
import org.muilab.notigpt.database.room.DrawerDatabase
import org.muilab.notigpt.model.NotiUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun getNotifications(context: Context): ArrayList<NotiUnit> = with(Dispatchers.IO) {
    val drawerDatabase = DrawerDatabase.getInstance(context)
    val drawerDao = drawerDatabase.drawerDao()
    return drawerDao.getAllVisible().toCollection(ArrayList())
}

fun getViewedNotifications(context: Context): ArrayList<NotiUnit> = with(Dispatchers.IO) {
    val drawerDatabase = DrawerDatabase.getInstance(context)
    val drawerDao = drawerDatabase.drawerDao()
    return drawerDao.getAllVisible().toCollection(ArrayList())
}

fun replaceChars(str: String): String {
    return str.replace("\n", " ").replace(",", " ")
}

fun getDisplayTimeStr(unixTime: Long, locale: Locale = Locale("zh", "TW")): String {
    val now = System.currentTimeMillis()
    val diffInMillis = now - unixTime
    val formatter = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))

    // Calculate differences in various units
    val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(abs(diffInMillis))
    val diffInHours = TimeUnit.MILLISECONDS.toHours(abs(diffInMillis))
    val diffInDays = TimeUnit.MILLISECONDS.toDays(abs(diffInMillis))

    return when {
        diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "現在"
        diffInMinutes < 60 -> formatter.format(diffInMinutes.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.MINUTES).toString()
        diffInHours < 3 -> formatter.format(diffInHours.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.HOURS).toString()
        diffInHours < 24 -> {
            val calNow = Calendar.getInstance()
            val calInput = Calendar.getInstance().apply { timeInMillis = unixTime}
            val dateFormat = if (calNow.get(Calendar.DATE) - calInput.get(Calendar.DATE) == 1) {
                SimpleDateFormat("'昨天' HH:mm", locale)
            } else {
                SimpleDateFormat("HH:mm", locale)
            }
            dateFormat.format(Date(unixTime))
        }
        diffInDays == 1L -> "昨天"
        diffInDays < 7 -> {
            val dayFormat = SimpleDateFormat("EEEE", locale)
            dayFormat.format(Date(unixTime))
        }
        else -> {
            val dateFormat = SimpleDateFormat("M'月' d'日'", Locale.getDefault())
            dateFormat.format(Date(unixTime))
        }
    }
}

fun getRelativeTimeStr(unixTime: Long, locale: Locale = Locale("en", "TW")): String {
    val now = System.currentTimeMillis()
    val diffInMillis = now - unixTime
    val formatter = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale))

    // Calculate differences in various units
    val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(abs(diffInMillis))
    val diffInHours = TimeUnit.MILLISECONDS.toHours(abs(diffInMillis))
    val diffInDays = TimeUnit.MILLISECONDS.toDays(abs(diffInMillis))

    return when {
        diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "Now"
        diffInMinutes < 60 -> formatter.format(diffInMinutes.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.MINUTES).toString()
        diffInHours < 3 -> formatter.format(diffInHours.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.HOURS).toString()
        diffInHours < 24 -> {
            val calNow = Calendar.getInstance()
            val calInput = Calendar.getInstance().apply { timeInMillis = unixTime}
            val dateFormat = if (calNow.get(Calendar.DATE) - calInput.get(Calendar.DATE) == 1) {
                SimpleDateFormat("'Yesterday' HH:mm", locale)
            } else {
                SimpleDateFormat("HH:mm", locale)
            }
            dateFormat.format(Date(unixTime))
        }
        diffInDays == 1L -> "Yesterday"
        diffInDays < 7 -> "$diffInDays days ago"
        diffInDays < 14 -> "Last week"
        else -> "${(diffInDays / 7).toInt()} weeks ago"
    }
}