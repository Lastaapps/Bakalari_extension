/*
 *    Copyright 2020, Petr Laštovička as Lasta apps, All rights reserved
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

package cz.lastaapps.bakalariextension.ui.bottom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R

class BottomFragment : Fragment() {

    companion object {
        private val TAG = BottomFragment::class.java.simpleName

        //for the state variable
        const val MINIMIZED = 0
        const val EXPANDED = 1
        const val HIDDEN = 2

        //for the showSwitch variable
        const val UNKNOWN = -1
        const val HIDE = 0
        const val SHOW = 1

    }

    private lateinit var root: View
    private lateinit var switch: ImageButton
    private lateinit var layout: RecyclerView

    private var columnWidth = 0
    private var rowHeight = 0

    private lateinit var controller: NavController

    val items = ArrayList<BottomItem>()

    private var state = MINIMIZED
    private var showSwitch = UNKNOWN

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {

        columnWidth = resources.getDimensionPixelSize(R.dimen.bottom_column_width)
        rowHeight = resources.getDimensionPixelSize(R.dimen.bottom_row_height)

        controller = requireActivity().findNavController(R.id.nav_host_fragment)

        root = inflater.inflate(R.layout.fragment_bottom, container, false)
        switch = root.findViewById(R.id.show_more)
        layout = root.findViewById(R.id.layout)

        layout.adapter = BottomAdapter(items).apply {
            onClick = { id, position ->
                doNavigation(id)
            }
        }
        layout.layoutManager =
            AutoFitLayoutManager(
                requireContext(),
                resources.getDimensionPixelSize(R.dimen.bottom_column_width),
                RecyclerView.VERTICAL,
                true
            )
        layout.addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
            val width = right - left
            val height = bottom - top

            if (height != 0) {
                if (showSwitch == UNKNOWN) {
                    val itemsShown = width / columnWidth

                    showSwitch = if (itemsShown >= items.size) HIDE else SHOW

                    controller.currentDestination?.let {
                        autoManageDestinations(it.id)
                    }
                }
            }
        }

        controller.addOnDestinationChangedListener { _, destination, _ ->
            autoManageDestinations(destination.id)
        }

        switch.visibility = View.GONE
        switch.setOnClickListener {
            setState(if (state != MINIMIZED) MINIMIZED else EXPANDED)
        }

        return root
    }

    private fun autoManageDestinations(id: Int) {
        val hidden = arrayOf(R.id.nav_loading, R.id.nav_login)
        val expanded = arrayOf(R.id.nav_home)
        val doNothing = arrayOf(R.id.nav_about, R.id.nav_license)
        val minimized = arrayOf(0) //all zhe others

        when {
            hidden.contains(id) -> setState(HIDDEN)
            expanded.contains(id) -> setState(EXPANDED)
            doNothing.contains(id) -> {
            }
            else -> setState(MINIMIZED)
        }

        val hideSwitch = arrayOf(R.id.nav_home, R.id.nav_loading, R.id.nav_login)

        switchVisibility(!hideSwitch.contains(id))
    }

    private fun switchVisibility(canBeShown: Boolean) {
        switch.visibility =
            when (showSwitch) {
                HIDE -> View.GONE
                SHOW -> {
                    if (canBeShown)
                        View.VISIBLE
                    else
                        View.GONE
                }
                UNKNOWN -> View.GONE
                else -> View.GONE
            }
    }

    private fun setState(state: Int) {
        if (this.state != state)
            when (state) {
                MINIMIZED -> {
                    root.layoutParams = root.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    layout.layoutParams = layout.layoutParams.apply {
                        height = rowHeight
                    }
                }
                EXPANDED -> {
                    root.layoutParams = root.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    layout.layoutParams = layout.layoutParams.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
                HIDDEN -> {
                    root.layoutParams = root.layoutParams.apply {
                        height = 0
                    }
                    layout.layoutParams = layout.layoutParams.apply {
                        height = 0
                    }
                }
            }

        this.state = state
    }

    private fun doNavigation(id: Int) {
        val builder = NavOptions.Builder()
            .setLaunchSingleTop(true)

        builder.setPopUpTo(
            R.id.nav_home,
            false
        )

        val options = builder.build()

        controller.navigate(id, null, options)
    }

    fun dataUpdated() {
        if (this::layout.isInitialized)
            layout.adapter?.notifyDataSetChanged()
    }
}