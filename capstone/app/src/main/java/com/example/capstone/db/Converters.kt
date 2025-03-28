package com.example.capstone.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room 데이터베이스를 위한 타입 변환기 클래스
 * 복잡한 데이터 타입(리스트 등)을 DB에 저장 가능한 형태로 변환
 */
class Converters {
    private val gson = Gson()

    /**
     * 문자열 리스트를 JSON 문자열로 변환
     * @param list 변환할 문자열 리스트
     * @return 변환된 JSON 문자열 또는 null
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    /**
     * JSON 문자열을 문자열 리스트로 변환
     * @param json 변환할 JSON 문자열
     * @return 변환된 문자열 리스트 또는 빈 리스트
     */
    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json == null) {
            return ArrayList()
        }
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: ArrayList()
    }
} 
