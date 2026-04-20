package com.dkgs.innerpulse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dkgs.innerpulse.data.local.dao.SleepDao
import com.dkgs.innerpulse.data.local.dao.CoachDao
import com.dkgs.innerpulse.data.local.dao.StreakDao
import com.dkgs.innerpulse.data.local.dao.MoodDao
import com.dkgs.innerpulse.data.local.dao.JournalDao
import com.dkgs.innerpulse.data.local.entity.SleepEntry
import com.dkgs.innerpulse.data.local.entity.CoachMessageEntity
import com.dkgs.innerpulse.data.local.entity.StreakEntry
import com.dkgs.innerpulse.data.local.entity.MoodEntry
import com.dkgs.innerpulse.data.local.entity.JournalEntry

import com.dkgs.innerpulse.data.local.dao.DailyFitnessDao
import com.dkgs.innerpulse.data.local.entity.DailyFitnessRecord

@Database(entities = [SleepEntry::class, CoachMessageEntity::class, StreakEntry::class, MoodEntry::class, JournalEntry::class, DailyFitnessRecord::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun coachDao(): CoachDao
    abstract fun streakDao(): StreakDao
    abstract fun moodDao(): MoodDao
    abstract fun journalDao(): JournalDao
    abstract fun dailyFitnessDao(): DailyFitnessDao
}
