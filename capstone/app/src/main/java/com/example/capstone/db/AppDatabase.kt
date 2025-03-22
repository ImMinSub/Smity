package com.example.capstone.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.capstone.data.Server
import com.example.capstone.data.User
import com.example.capstone.data.UserPreferences

@Database(entities = [User::class, UserPreferences::class, Server::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun serverDao(): ServerDao
}