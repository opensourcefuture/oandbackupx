/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.dbs

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.machiav3lli.backup.SCHEDULES_DB_NAME
import com.machiav3lli.backup.dbs.dao.*
import com.machiav3lli.backup.dbs.entity.*

@Database(
    entities = [
        Schedule::class,
        Blocklist::class,
        AppExtras::class,
        AppInfo::class,
        SpecialInfo::class,
        Backup::class],
    version = 4,
    exportSchema = true,
    autoMigrations = []
)
@TypeConverters(Converters::class)
abstract class ODatabase : RoomDatabase() {
    abstract val scheduleDao: ScheduleDao
    abstract val blocklistDao: BlocklistDao
    abstract val appExtrasDao: AppExtrasDao
    abstract val backupDao: BackupDao
    abstract val appInfoDao: AppInfoDao
    abstract val specialInfoDao: SpecialInfoDao

    companion object {
        @Volatile
        private var INSTANCE: ODatabase? = null

        fun getInstance(context: Context): ODatabase {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = Room
                        .databaseBuilder(
                            context.applicationContext, ODatabase::class.java,
                            SCHEDULES_DB_NAME
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                }
                return INSTANCE!!
            }
        }
    }
}