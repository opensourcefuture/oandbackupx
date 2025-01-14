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
package com.machiav3lli.backup.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.machiav3lli.backup.dbs.ODatabase
import com.machiav3lli.backup.dbs.entity.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PackageUnInstalledReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val db = ODatabase.getInstance(context)
        val packageName =
            intent.data?.let { if (it.scheme == "package") it.schemeSpecificPart else null }
        if (packageName != null) {
            when (intent.action.orEmpty()) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    context.packageManager.getPackageInfo(packageName, 0)?.let { packageInfo ->
                        val appInfo = AppInfo(context, packageInfo)
                        GlobalScope.launch(Dispatchers.IO) {
                            db.appInfoDao.insert(appInfo)
                        }
                    }
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        db.appInfoDao.deleteAllOf(packageName)
                    }
                }
            }
        }
    }
}