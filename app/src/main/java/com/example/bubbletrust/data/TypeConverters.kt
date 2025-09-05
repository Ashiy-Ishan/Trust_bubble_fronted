package com.yourdomain.bubbletrust.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromDecisionNode(node: DecisionNode?): String? {
        return gson.toJson(node)
    }

    @TypeConverter
    fun toDecisionNode(json: String?): DecisionNode? {
        if (json == null) return null
        val type = object : TypeToken<DecisionNode>() {}.type
        return gson.fromJson(json, type)
    }
}