package org.muilab.notigpt.model.notifications

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.TreeSet

class NotiTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromNotiInfoSet(notiInfoSet: TreeSet<NotiInfo>?): String? {
        if (notiInfoSet == null) {
            return null
        }
        val type = object : TypeToken<List<NotiInfo>>() {}.type
        return gson.toJson(notiInfoSet.toList(), type)
    }

    @TypeConverter
    fun toNotiInfoSet(notiInfoString: String?): TreeSet<NotiInfo>? {
        if (notiInfoString == null) {
            return null
        }
        val type = object : TypeToken<List<NotiInfo>>() {}.type
        return TreeSet(compareBy<NotiInfo> { it.time }).apply {
            addAll(gson.fromJson<List<NotiInfo>>(notiInfoString, type))
        }
    }

    @TypeConverter
    fun fromStringSet(stringSet: MutableSet<String>?): String? {
        if (stringSet == null) {
            return null
        }
        val type = object : TypeToken<MutableSet<String>>() {}.type
        return gson.toJson(stringSet, type)
    }

    @TypeConverter
    fun toStringSet(stringSetString: String?): MutableSet<String>? {
        if (stringSetString == null) {
            return null
        }
        val type = object : TypeToken<MutableSet<String>>() {}.type
        return gson.fromJson<MutableSet<String>>(stringSetString, type)
    }

//    @TypeConverter
//    fun fromMetadata(metadata: NotiMetadata): String {
//        return Gson().toJson(metadata)
//    }
//
//    @TypeConverter
//    fun toMetadata(json: String): NotiMetadata {
//        val type = object : TypeToken<NotiMetadata>() {}.type
//        return Gson().fromJson(json, type)
//    }
//
//    @TypeConverter
//    fun fromOutcome(metadata: NotiOutcome): String {
//        return Gson().toJson(metadata)
//    }
//
//    @TypeConverter
//    fun toOutcome(json: String): NotiOutcome {
//        val type = object : TypeToken<NotiOutcome>() {}.type
//        return Gson().fromJson(json, type)
//    }
}