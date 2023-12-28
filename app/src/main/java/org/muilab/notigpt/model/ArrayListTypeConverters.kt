package org.muilab.notigpt.model

import androidx.room.TypeConverter

class ArrayListTypeConverters {

    private val separator = "\t\t\t\t\t\t\t\t"

    @TypeConverter
    fun fromStringList(value: String?): ArrayList<String> {
        return value?.split(separator)?.toCollection(ArrayList()) ?: ArrayList()
    }

    @TypeConverter
    fun toStringList(list: ArrayList<String>?): String {
        return list?.joinToString(separator) ?: ""
    }

    @TypeConverter
    fun fromLongList(value: String?): ArrayList<Long> {
        return value?.split(separator)?.mapNotNull { it.toLongOrNull() }?.toCollection(ArrayList()) ?: ArrayList()
    }

    @TypeConverter
    fun toLongList(list: ArrayList<Long>?): String {
        return list?.joinToString(separator) { it.toString() } ?: ""
    }

}