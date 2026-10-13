package com.example.calorietracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Profile::class, WeightEntry::class, Meal::class, WaterIntake::class,
        PlanInfo::class, PlanDay::class, PlanExercise::class, ExerciseCompletion::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calorieDao(): CalorieDao
    abstract fun trainingDao(): TrainingDao

    companion object {
        /** Version 2 added water tracking. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `water_intake` " +
                        "(`epochDay` INTEGER NOT NULL, `ml` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))"
                )
            }
        }

        /** Version 3 added the training plan. SQL copied from Room's generated schema. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plan_info` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                        "`prompt` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plan_days` (`dayOfWeek` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                        "`isRest` INTEGER NOT NULL, PRIMARY KEY(`dayOfWeek`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plan_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`dayOfWeek` INTEGER NOT NULL, `position` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`sets` INTEGER, `target` TEXT NOT NULL, `notes` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_exercises_dayOfWeek` ON `plan_exercises` (`dayOfWeek`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_completions` (`epochDay` INTEGER NOT NULL, " +
                        "`exerciseId` INTEGER NOT NULL, PRIMARY KEY(`epochDay`, `exerciseId`), " +
                        "FOREIGN KEY(`exerciseId`) REFERENCES `plan_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_completions_exerciseId` ON `exercise_completions` (`exerciseId`)"
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "calorie_tracker.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
