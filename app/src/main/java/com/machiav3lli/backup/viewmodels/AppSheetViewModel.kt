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
package com.machiav3lli.backup.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.machiav3lli.backup.activities.MainActivityX
import com.machiav3lli.backup.dbs.entity.Backup
import com.machiav3lli.backup.handler.LogsHandler
import com.machiav3lli.backup.handler.ShellCommands
import com.machiav3lli.backup.handler.showNotification
import com.machiav3lli.backup.items.Package
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppSheetViewModel(
    app: Package,
    var shellCommands: ShellCommands,
    private val appContext: Application
) : AndroidViewModel(appContext) {

    var appInfo = MediatorLiveData<Package>()

    private var notificationId: Int

    val refreshNow = MutableLiveData<Boolean>()

    init {
        appInfo.value = app
        notificationId = System.currentTimeMillis().toInt()
    }

    fun uninstallApp() {
        viewModelScope.launch {
            uninstall()
            refreshNow.value = true
        }
    }

    private suspend fun uninstall() {
        withContext(Dispatchers.IO) {
            appInfo.value?.let {
                Timber.i("uninstalling: ${appInfo.value?.packageLabel}")
                try {
                    shellCommands.uninstall(
                        appInfo.value?.packageName, appInfo.value?.apkPath,
                        appInfo.value?.dataPath, appInfo.value?.isSystem == true
                    )
                    showNotification(
                        appContext,
                        MainActivityX::class.java,
                        notificationId++,
                        appInfo.value?.packageLabel,
                        appContext.getString(com.machiav3lli.backup.R.string.uninstallSuccess),
                        true
                    )
                } catch (e: ShellCommands.ShellActionFailedException) {
                    showNotification(
                        appContext,
                        MainActivityX::class.java,
                        notificationId++,
                        appInfo.value?.packageLabel,
                        appContext.getString(com.machiav3lli.backup.R.string.uninstallFailure),
                        true
                    )
                    e.message?.let { message -> LogsHandler.logErrors(appContext, message) }
                }
            }
        }
    }

    fun enableDisableApp(users: MutableList<String>, enable: Boolean) {
        viewModelScope.launch {
            enableDisable(users, enable)
            refreshNow.value = true
        }
    }

    @Throws(ShellCommands.ShellActionFailedException::class)
    private suspend fun enableDisable(users: MutableList<String>, enable: Boolean) {
        withContext(Dispatchers.IO) {
            shellCommands.enableDisablePackage(appInfo.value?.packageName, users, enable)
        }
    }

    fun getUsers(): Array<String> {
        return shellCommands.getUsers()?.toTypedArray() ?: arrayOf()
    }

    fun deleteBackup(backup: Backup) {
        viewModelScope.launch {
            delete(backup)
            refreshNow.value = true
        }
    }

    private suspend fun delete(backup: Backup) {
        withContext(Dispatchers.IO) {
            appInfo.value?.let {
                if (it.backupHistory.size > 1) {
                    it.delete(backup)
                } else {
                    it.deleteAllBackups()
                }
            }
        }
    }

    fun deleteAllBackups() {
        viewModelScope.launch {
            deleteAll()
            refreshNow.value = true
        }
    }

    private suspend fun deleteAll() {
        withContext(Dispatchers.IO) {
            appInfo.value?.deleteAllBackups()
        }
    }

    class Factory(
        private val app: Package,
        private val shellCommands: ShellCommands,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppSheetViewModel::class.java)) {
                return AppSheetViewModel(app, shellCommands, application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}