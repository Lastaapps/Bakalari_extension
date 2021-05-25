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

package cz.lastaapps.bakalari.app.ui.uitools

import android.util.Log
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import cz.lastaapps.bakalari.app.MainActivity
import cz.lastaapps.bakalari.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


inline fun <reified VM : ViewModel> Fragment.accountsViewModels() =
    navGraphViewModels<VM>(R.id.nav_graph_user)

inline fun <reified VM : ViewModel> Fragment.settingsViewModels() =
    navGraphViewModels<VM>(R.id.nav_graph_settings)

inline fun <reified VM : ViewModel> FragmentActivity.navGraphViewModels(
    @IdRes viewId: Int,
    @IdRes navGraphId: Int,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
    val backStackEntry by lazy { findNavController(viewId).getBackStackEntry(navGraphId) }
    val storeProducer: () -> ViewModelStore = { backStackEntry.viewModelStore }
    val factoryPromise = factoryProducer ?: { defaultViewModelProviderFactory }
    return ViewModelLazy(VM::class, storeProducer, factoryPromise)
}

inline fun <reified VM : ViewModel> MainActivity.accountsViewModels() =
    navGraphViewModels<VM>(R.id.nav_host_fragment, R.id.nav_graph_user)


fun observeForControllerGraphChanges(
    activity: MainActivity,
    user: suspend () -> Unit = {},
    neutral: suspend () -> Unit = {},
    root: suspend () -> Unit = {},
) = observeForControllerGraphChanges(
    activity.findNavController(), activity.lifecycleScope, user, neutral, root
)

fun observeForControllerGraphChanges(
    controller: NavController,
    scope: CoroutineScope,
    user: suspend () -> Unit = {},
    neutral: suspend () -> Unit = {},
    root: suspend () -> Unit = {},
) {
    controller.addOnDestinationChangedListener { _, destination, _ ->
        scope.launch(Dispatchers.Main) {

            val neutralGraphs = listOf(R.id.nav_graph_root, R.id.nav_graph_settings)
            when (destination.parent?.id) {
                R.id.nav_graph_user -> user()
                in neutralGraphs -> neutral()
                else -> root()
            }
        }
    }
}

fun NavController.backStackDebugPrinting(TAG: String) {
    //return //to make the disabling easier
    addOnDestinationChangedListener { controller, destination, arguments ->
        Log.i(TAG, "Printing back stack _________________________________________________")
        for (entry in controller.backStack) {
            val dest = entry.destination
            Log.i(TAG, "" + dest.navigatorName + " " + dest.displayName)
        }
        Log.i(TAG, "Printing end ________________________________________________________")
    }
}