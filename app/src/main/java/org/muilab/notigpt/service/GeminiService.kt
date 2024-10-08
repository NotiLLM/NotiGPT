package org.muilab.notigpt.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.FunctionType
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.muilab.notigpt.BuildConfig
import org.muilab.notigpt.R
import org.muilab.notigpt.model.notifications.NotiUnit
import org.muilab.notigpt.util.getDisplayTimeStr
import org.muilab.notigpt.util.getNotifications
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeminiService: Service() {

    private val modelName = "gemini-1.5-pro"
    private val apiKey = BuildConfig.apiKey

    // GPT Prompts
    private lateinit var summaryLeadPrompt: String
    private lateinit var summaryEndPrompt: String
    private lateinit var categoryLeadPrompt: String
    private lateinit var categoryEndPrompt: String
    private lateinit var sortLeadPrompt: String
    private lateinit var sortEndPrompt: String

    private val binder = GPTBinder()

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey
    )

    private val summaryGenerativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema(
                name = "notification_summaries",
                description = "List of summaries for each notification",
                type = FunctionType.ARRAY,
                items = Schema(
                    name = "notification_summary",
                    description = "Summary for each notification with their corresponding keys",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "id" to Schema(
                            name = "id",
                            description = "Key of notification",
                            type = FunctionType.INTEGER,
                            nullable = false
                        ),
                        "summary" to Schema(
                            name = "summary",
                            description = "Summary of notification with corresponding id",
                            type = FunctionType.STRING,
                            nullable = false
                        )
                    )
                )
            )
        }
    )

    private val sortingGenerativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = Schema(
                name = "notification_sorting_scores",
                description = "List of scores of each notification for reordering",
                type = FunctionType.ARRAY,
                items = Schema(
                    name = "notification_sorting_score",
                    description = "Summary for each notification with their corresponding keys",
                    type = FunctionType.OBJECT,
                    properties = mapOf(
                        "id" to Schema(
                            name = "id",
                            description = "Key of notification",
                            type = FunctionType.INTEGER,
                            nullable = false
                        ),
                        "timeSensitiveness" to Schema(
                            name = "time_sensitiveness",
                            description = "Time sensitiveness score of notification with corresponding id, with exactly two decimal places",
                            type = FunctionType.NUMBER,
                            nullable = false
                        ),
                        "senderAttractiveness" to Schema(
                            name = "sender_attractiveness",
                            description = "Sender attractiveness score of notification with corresponding id, with exactly two decimal places",
                            type = FunctionType.NUMBER,
                            nullable = false
                        ),
                        "contentAttractiveness" to Schema(
                            name = "content_attractiveness",
                            description = "Content attractiveness score of notification with corresponding id, with exactly two decimal places",
                            type = FunctionType.NUMBER,
                            nullable = false
                        ),
                    )
                )
            )
        }
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        summaryLeadPrompt = getString(R.string.summary_lead_prompt)
        summaryEndPrompt = getString(R.string.summary_end_prompt)
        categoryLeadPrompt = getString(R.string.category_lead_prompt)
        categoryEndPrompt = getString(R.string.category_end_prompt)
        sortLeadPrompt = getString(R.string.sort_lead_prompt)
        sortEndPrompt = getString(R.string.sort_end_prompt)
        return START_STICKY
    }

    private fun getPostContent(notifications: ArrayList<NotiUnit>): String {

        val notiDrawerSb = StringBuilder()

        notifications.forEach { noti ->

            val isPeople = noti.getIsPeople()
            val notiBody = noti.getNotiBody()
            val prevBody = noti.getPrevBody()

            val notiJson = JSONObject()
            notiJson.put("id", noti.getHashKey())
            notiJson.put("app", noti.getTitle())

            val titlesIdentical = (notiBody + prevBody)
                .map { it.title }
                .filter { it.isNotBlank() }
                .toSet().size == 1
            val notiType = if (isPeople) "message" else "info"
            val notiTypeTitle = if (isPeople) "sender" else "title"

            notiJson.put("overall_$notiTypeTitle", org.muilab.notigpt.util.replaceChars(noti.getTitle()))

            if (prevBody.isNotEmpty()) {
                val previousNotisArray = JSONArray()
                prevBody.forEach {
                    val prevNotiJson = JSONObject()
                    prevNotiJson.put("time", getDisplayTimeStr(it.time))
                    if (!titlesIdentical)
                        prevNotiJson.put(notiTypeTitle, org.muilab.notigpt.util.replaceChars(it.title))
                    prevNotiJson.put("content", org.muilab.notigpt.util.replaceChars(it.content))
                    previousNotisArray.put(prevNotiJson)
                }
                notiJson.put("previous_${notiType}s", previousNotisArray)

                val newNotisArray = JSONArray()

                notiBody.forEach {
                    val newNotiJson = JSONObject()
                    newNotiJson.put("time", getDisplayTimeStr(it.time))
                    if (!titlesIdentical)
                        newNotiJson.put(notiTypeTitle, org.muilab.notigpt.util.replaceChars(it.title))
                    newNotiJson.put("content", org.muilab.notigpt.util.replaceChars(it.content))
                    newNotisArray.put(newNotiJson)
                }
                notiJson.put("new_${notiType}s", newNotisArray)
            } else {
                val notiInfosArray = JSONArray()

                notiBody.forEach {
                    val notiInfoJson = JSONObject()
                    notiInfoJson.put("time", getDisplayTimeStr(it.time))
                    if (!titlesIdentical)
                        notiInfoJson.put(notiTypeTitle, org.muilab.notigpt.util.replaceChars(it.title))
                    notiInfoJson.put("content", org.muilab.notigpt.util.replaceChars(it.content))
                    notiInfosArray.put(notiInfoJson)
                }
                notiJson.put("${notiType}s", notiInfosArray)
            }

            // Convert the JSON object to a string
            val notiJsonStr = notiJson.toString(2)

            notiDrawerSb.append("$notiJsonStr,\n")
        }
        return notiDrawerSb.toString()
    }

    suspend fun makeRequest(requestType: String): String {

        return withContext(Dispatchers.IO) {

            val leadPrompt = when (requestType) {
                "sort" -> sortLeadPrompt
                "summarize" -> summaryLeadPrompt
                else -> ""
            }

            val getLocalTimePrompt = {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.getDefault())
                "The current time is ${sdf.format(Date())}, and the user's Locale is ${Locale.getDefault()}."
            }

            val notifications = getNotifications(applicationContext)
            val notificationsStr = getPostContent(notifications)

            val endPrompt = when (requestType) {
                "sort" -> sortEndPrompt
                "summarize" -> summaryEndPrompt
                else -> ""
            }

            val overallPrompt = "$leadPrompt\n${getLocalTimePrompt()}\n$notificationsStr\n$endPrompt"

            val generativeModel = when (requestType) {
                "sort" -> sortingGenerativeModel
                "summarize" -> summaryGenerativeModel
                else -> generativeModel
            }

            val response = generativeModel.generateContent(overallPrompt)

            response.text.toString()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class GPTBinder : Binder() {
        fun getService(): GeminiService {
            return this@GeminiService
        }
    }
}