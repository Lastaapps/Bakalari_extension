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

package cz.lastaapps.bakalari.app.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import cz.lastaapps.bakalari.tools.marshall
import cz.lastaapps.bakalari.tools.unmarshall
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class ComplexDeepLinkNavigator(private val navController: NavController) {

    companion object {
        private val TAG get() = ComplexDeepLinkNavigator::class.simpleName

        private const val KEY_ACTION = "ACTIONS"

        fun <T : Activity> createIntent(
            context: Context,
            component: Class<T>,
            list: List<NavDirections>
        ): Intent {
            return Intent(context, component).apply {
                updateIntent(this, list)
            }
        }

        fun updateIntent(intent: Intent, list: List<NavDirections>): Intent {

            val l = ArrayList<ByteArray>()

            for (item in list.map { it.toActionHolder() }) {
                l.add(item.marshall())
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            intent.putExtra(KEY_ACTION, l as Serializable)

            return intent
        }
    }

    fun handle(intent: Intent) {
        Log.i(TAG, "Handling navigation")

        val list = (intent.getSerializableExtra(KEY_ACTION) as ArrayList<ByteArray>?)?.map {
            it.unmarshall(ActionHolder.CREATOR)
        } ?: return

        navController.popBackStack(navController.graph.startDestination, false)

        for (item in list) {
            navController.navigate(item.id, item.arguments)
        }

        intent.removeExtra(KEY_ACTION)
    }

    @Parcelize
    class ActionHolder(val id: Int, val arguments: Bundle?) : Parcelable, Serializable {
        companion object {}
    }

    private val ActionHolder.Companion.CREATOR: Parcelable.Creator<ActionHolder>
        get() = ComplexDeepLinkNavigatorParcelableAccessor.getActionHolderCreator()

}

private fun NavDirections.toActionHolder() =
    ComplexDeepLinkNavigator.ActionHolder(this.actionId, this.arguments)


