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
package com.machiav3lli.backup.fragments

import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import com.machiav3lli.backup.activities.MainActivityX
import com.machiav3lli.backup.dbs.entity.AppExtras
import com.machiav3lli.backup.items.Package

abstract class NavigationFragment : Fragment(), ProgressViewController {
    protected var sheetSortFilter: SortFilterSheet? = null
    val packageList: MediatorLiveData<MutableList<Package>>
        get() = requireMainActivity().viewModel.packageList
    var appExtrasList: MutableList<AppExtras>
        get() = requireMainActivity().viewModel.appExtrasList
        set(value) {
            requireMainActivity().viewModel.appExtrasList = value
        }

    abstract fun setupViews()
    abstract fun setupOnClicks()

    override fun onResume() {
        super.onResume()
        requireMainActivity().setProgressViewController(this)
    }

    fun requireMainActivity(): MainActivityX = super.requireActivity() as MainActivityX
}

interface RefreshViewController {
    fun refreshView(list: MutableList<Package>?)
}

interface ProgressViewController {
    fun updateProgress(progress: Int, max: Int)
    fun hideProgress()
}