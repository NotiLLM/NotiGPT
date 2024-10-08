package org.muilab.notigpt.model.notifications

import android.app.Notification
import android.app.Notification.MessagingStyle.Message.getMessagesFromBundleArray
import android.app.Person
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi

data class NotiInfo (
    val time: Long,
    val title: String,
    val person: String,
    val content: String,
    var notiSeen: Boolean = false,
) {

    companion object {
        private fun fetchTime(sbn: StatusBarNotification): Long {
            val `when` = sbn.notification?.`when` as Long
            val postTime = sbn.postTime
            return if (`when` != 0L)
                `when`
            else
                postTime
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun fetchPerson(sbn: StatusBarNotification): String {
            val messages = sbn.notification?.extras?.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null) {
                getMessagesFromBundleArray(messages).lastOrNull()?.senderPerson?.name.let {
                    if (it != null)
                        return it.toString()
                }
            }

            val callPerson = sbn.notification?.extras?.get(Notification.EXTRA_CALL_PERSON)
            if (callPerson != null && !(callPerson as Person).name.isNullOrBlank())
                return callPerson.name.toString()

            return ""
        }

        private fun fetchTitle(sbn: StatusBarNotification): String {
            return sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE_BIG).let { bigTitle ->
                bigTitle?.toString() ?: sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TITLE).let { title ->
                    title?.toString() ?: ""
                }
            }
        }

        fun fetchContent(sbn: StatusBarNotification): String {
            var content = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
            if (content == null || content.toString().isBlank())
                content = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)
            if (content == null || content.toString().isBlank())
                content = sbn.notification?.extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).let { textLines ->
                    textLines?.mapNotNull { it.toString() }?.joinToString(separator = "\n")
                }
            if (content == null || content.toString().isBlank())
                content = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            if (content == null || content.toString().isBlank())
                content = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)
            if (content == null || content.toString().isBlank())
                content = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)
            return content?.toString() ?: ""
        }

        val senderInTitle = setOf<String>()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    constructor (sbn: StatusBarNotification): this(
        time = fetchTime(sbn),
        title = fetchTitle(sbn),
        person = fetchPerson(sbn),
        content = fetchContent(sbn)
    )

    constructor(time: Long): this(
        time = time,
        title = "",
        person = "",
        content = ""
    )

    constructor(time: Long, person: String, content: String): this(
        time = time,
        title = person,
        person = person,
        content = content
    )

    fun getTitle(packageName: String, isPerson: Boolean): String {
        if (isPerson && packageName !in senderInTitle && person.isNotBlank())
            return person
        return title
    }
}