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
package com.machiav3lli.backup.tasks

import android.content.Context
import android.content.Intent
import com.machiav3lli.backup.*
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.handler.LogsHandler
import com.machiav3lli.backup.handler.getPackageList
import com.machiav3lli.backup.items.Package
import com.machiav3lli.backup.utils.FileUtils
import com.machiav3lli.backup.utils.StorageLocationNotConfiguredException
import com.machiav3lli.backup.utils.getDefaultSharedPreferences
import timber.log.Timber
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

open class ScheduledActionTask(val context: Context, private val scheduleId: Long) :
    CoroutinesAsyncTask<Void?, String, Triple<String, List<String>, Int>>() {

    override fun doInBackground(vararg params: Void?): Triple<String, List<String>, Int>? {

        val database = ODatabase.getInstance(context)
        val scheduleDao = database.scheduleDao
        val blacklistDao = database.blocklistDao

        val schedule = scheduleDao.getSchedule(scheduleId)
            ?: return Triple("DbFailed", listOf(), MODE_UNSET)

        val name = schedule.name.toString()
        val filter = schedule.filter
        val specialFilter = schedule.specialFilter
        val customList = schedule.customList
        val customBlocklist = schedule.blockList
        val globalBlocklist = blacklistDao.getBlocklistedPackages(PACKAGES_LIST_GLOBAL_ID)
        val blockList = globalBlocklist.plus(customBlocklist)

        val unfilteredList: List<Package> = try {
            context.getPackageList(blockList, false)
        } catch (e: FileUtils.BackupLocationInAccessibleException) {
            Timber.e("Scheduled backup failed due to ${e.javaClass.simpleName}: $e")
            LogsHandler.logErrors(
                context,
                "Scheduled backup failed due to ${e.javaClass.simpleName}: $e"
            )
            return Triple(name, listOf(), MODE_UNSET)
        } catch (e: StorageLocationNotConfiguredException) {
            Timber.e("Scheduled backup failed due to ${e.javaClass.simpleName}: $e")
            LogsHandler.logErrors(
                context,
                "Scheduled backup failed due to ${e.javaClass.simpleName}: $e"
            )
            return Triple(name, listOf(), MODE_UNSET)
        }

        var launchableAppsList = listOf<String>()
        if (specialFilter == SPECIAL_FILTER_LAUNCHABLE) {
            val mainIntent =
                Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            launchableAppsList = context.packageManager.queryIntentActivities(mainIntent, 0)
                .map { it.activityInfo.packageName }
        }
        val inListed = { packageName: String ->
            customList.isEmpty() or customList.contains(packageName)
        }
        val predicate: (Package) -> Boolean = {
            (if (filter and MAIN_FILTER_SYSTEM == MAIN_FILTER_SYSTEM) it.isSystem and !it.isSpecial else false)
                    || (if (filter and MAIN_FILTER_USER == MAIN_FILTER_USER) !it.isSystem else false)
                    || (if (filter and MAIN_FILTER_SPECIAL == MAIN_FILTER_SPECIAL) it.isSpecial else false)
        }
        val days = context.getDefaultSharedPreferences().getInt(PREFS_OLDBACKUPS, 7)
        val specialPredicate: (Package) -> Boolean = when (specialFilter) {
            SPECIAL_FILTER_LAUNCHABLE -> { appInfo: Package ->
                launchableAppsList.contains(appInfo.packageName) and
                        inListed(appInfo.packageName)
            }
            SPECIAL_FILTER_NEW_UPDATED -> { appInfo: Package ->
                appInfo.isInstalled and
                        (!appInfo.hasBackups or appInfo.isUpdated) and
                        inListed(appInfo.packageName)
            }
            SPECIAL_FILTER_OLD -> {
                { appInfo: Package ->
                    if (appInfo.hasBackups) {
                        val lastBackup = appInfo.latestBackup?.backupDate
                        val diff = ChronoUnit.DAYS.between(lastBackup, LocalDateTime.now())
                        (diff >= days) and inListed(appInfo.packageName)
                    } else
                        false
                }
            }
            SPECIAL_FILTER_DISABLED -> { appInfo: Package ->
                appInfo.isDisabled and inListed(appInfo.packageName)
            }
            else -> { appInfo: Package -> inListed(appInfo.packageName) }
        }
        val selectedItems = unfilteredList
            .filter(predicate)
            .filter(specialPredicate)
            .sortedWith { m1: Package, m2: Package ->
                m1.packageLabel.compareTo(m2.packageLabel, ignoreCase = true)
            }
            .map(Package::packageName)
        return Triple(
            name,
            selectedItems,
            schedule.mode
        )
    }
}