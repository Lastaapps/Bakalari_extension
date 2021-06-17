/*
 *    Copyright 2021, Petr Laštovička as Lasta apps, All rights reserved
 *
 *     This file is part of Bakalari extension.
 *
 *     Bakalari extension is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Bakalari extension is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Bakalari extension.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package cz.lastaapps.bakalari.app.ui.settings.preferences

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.platform.withAppContext
import cz.lastaapps.bakalari.settings.MySettings
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import cz.lastaapps.bakalari.settings.R as SettR

class UserPreference : MyListPreference<UserPreference.AdapterDataHolder> {
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    companion object {
        private val TAG get() = UserPreference::class.simpleName
    }

    val activity = context as FragmentActivity
    val sett = MySettings.withAppContext()

    init {
        setDialogTitle(context.getString(SettR.string.sett_timetable_notification_dialog_title))

        val database = AccountsDatabase.getDatabase(context)
        val repo = database.repository

        setSummary(SettR.string.sett_timetable_notification_loading)

        val itemSelected: ((UUID?) -> Unit) = {
            sett.timetableNotificationAccountUUID = it

            activity.lifecycleScope.launch {
                when {
                    (it == null) -> {
                        setSummary(SettR.string.sett_timetable_notification_disabled)
                    }
                    (!repo.exitsUUID(it)) -> {
                        setSummary(SettR.string.sett_timetable_notification_user_not_found)
                    }
                    else -> {
                        val account = repo.getByUUID(it)!!

                        summary = account.profileName
                    }
                }
            }
        }

        itemSelected(sett.timetableNotificationAccountUUID)

        activity.lifecycleScope.launch {

            val accounts = repo.getAll()
            val dataList = ArrayList<AdapterDataHolder>()
            dataList.add(
                AdapterDataHolder(
                    context.getString(SettR.string.sett_timetable_notification_disabled),
                    null
                )
            )
            dataList.addAll(accounts.map { AdapterDataHolder(it.profileName, it) })
            val entries = dataList.map { it.string }.toTypedArray()

            setUp(
                dataList, { it.string },
                { newValue ->
                    Log.i(TAG, "Timetable notification changed to $newValue")

                    val index = entries.indexOf(newValue.string)

                    itemSelected(dataList[index].account?.uuid)
                },
            )
        }
    }

    class AdapterDataHolder(val string: String, val account: BakalariAccount?)
}