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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import cz.lastaapps.bakalariextension.R
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import java.lang.Integer.min


/**Manages the bottom navigation*/
class BottomFragment : Fragment() {

    companion object {
        private val TAG = BottomFragment::class.java.simpleName

        //for the state variable
        const val MINIMIZED = 0
        const val EXPANDED = 1
        const val HIDDEN = 2

        //for the showSwitch variable
        const val UNKNOWN = -1
        const val DISABLED = 0
        const val ENABLED = 1

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

        Log.i(TAG, "Creating view")

        //loads resources
        columnWidth = resources.getDimensionPixelSize(R.dimen.bottom_column_width)
        rowHeight = resources.getDimensionPixelSize(R.dimen.bottom_row_height)

        //for the navigation
        controller = requireActivity().findNavController(R.id.nav_host_fragment)

        //finds views
        root = inflater.inflate(R.layout.fragment_bottom, container, false)
        switch = root.findViewById(R.id.show_more)
        layout = root.findViewById(R.id.layout)

        //RecyclerView setup
        layout.adapter = BottomAdapter(items).apply {
            onClick = { id, position ->
                doNavigation(id)
            }
        }
        layout.layoutManager = AutoFitLayoutManager(
            requireContext(),
            resources.getDimensionPixelSize(R.dimen.bottom_column_width),
            RecyclerView.VERTICAL,
            true
        )

        addPaddingObserver()

        controller.addOnDestinationChangedListener { _, destination, _ ->
            autoManageDestinations(destination.id)
        }

        //show more lines switch setup
        switch.visibility = View.GONE
        switch.setOnClickListener {
            setState(if (state != MINIMIZED) MINIMIZED else EXPANDED)
        }

        //TODO wait for listeners
        /*
        if (Build.VERSION.SDK_INT >= 20) {

            ViewCompat.setOnApplyWindowInsetsListener(root) { v: View, insets: WindowInsetsCompat ->
                val params = v.layoutParams as MarginLayoutParams
                params.topMargin = insets.systemWindowInsetTop
                insets.consumeSystemWindowInsets()
            }

            val v = WindowInsetsCompat.toWindowInsetsCompat(
                requireActivity().windowManager.,
                root
            )
            v.isVisible(WindowInsetsCompat.Type.ime())

        } else { }*/

        KeyboardVisibilityEvent.setEventListener(
            requireActivity(),
            LifecycleOwner { lifecycle },
            object : KeyboardVisibilityEventListener {
                override fun onVisibilityChanged(isOpen: Boolean) {

                    root.visibility = if (isOpen) View.GONE else View.VISIBLE
                }
            })


        return root
    }

    /**shows appropriate state for fragment given*/
    private fun autoManageDestinations(id: Int) {
        //no option to expand
        val hidden = arrayOf(
            R.id.nav_loading, R.id.nav_login,
            R.id.nav_about, R.id.nav_license,
            R.id.nav_attachment, R.id.nav_attachment_downloaded, R.id.nav_attachment_file_exists,
            R.id.nav_teacher_info
        )
        //always expanded
        val expanded = arrayOf(R.id.nav_home)
        //retains last state
        val doNothing = arrayOf(0)
        //animalized with the option to expand and minimize again
        val minimized = arrayOf(0) //all the others

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

    /**shows state switch if it is required and available*/
    private fun switchVisibility(canBeShown: Boolean) {
        switch.visibility =
            when (showSwitch) {
                DISABLED -> View.GONE
                ENABLED -> {
                    if (canBeShown)
                        View.VISIBLE
                    else
                        View.GONE
                }
                UNKNOWN -> View.GONE
                else -> View.GONE
            }
    }

    /**Sets the state - hidden, minimized, expanded*/
    private fun setState(state: Int) {
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

    /**Navigates to selected destination*/
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


    //padding to center the items
    private val paddingListener: (() -> Boolean) = {
        layout.run {
            applyPadding(left, top, right, bottom)
        }
    }

    /**centers items and sets if state switch can be used at all (disabled for wide enough screens)*/
    private fun applyPadding(
        left: Int, top: Int, right: Int, bottom: Int
    ): Boolean {

        var drawnUnchanged = true

        //used to center items
        val marginLeft = root.findViewById<View>(R.id.margin_left)
        val marginRight = root.findViewById<View>(R.id.margin_right)

        val width = right - left + marginLeft.width * 2
        val height = bottom - top

        if (width != 0) {

            (layout.adapter as? BottomAdapter)?.let { adapter ->

                //computes the number of items based of the new size
                val itemsShown = min(width / columnWidth, adapter.list.size)
                val itemsSize = itemsShown * columnWidth
                val margin = (width - itemsSize) / 2

                //true if the centering of the items is required
                if (marginLeft.width != margin) {

                    Log.i(TAG, "Updating padding")

                    drawnUnchanged = false

                    val setWidth: ((view: View) -> Unit) = {
                        it.layoutParams = it.layoutParams.also { view ->
                            view.width = margin
                        }
                    }
                    setWidth(marginLeft)
                    setWidth(marginRight)

                }

                //stops observing after a successful attempt
                removePaddingObserver()

                //wasn't working
                /*layout.layoutParams =
                    (layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                        setMargins(margin, 0, margin, 0)
                    }*/
                //sometimes overlapped items
                //layout.setPadding(margin, 0, margin, 0)

                //determinate if the state switch should be shows
                val old = showSwitch
                showSwitch = if (items.size <= itemsShown) DISABLED else ENABLED

                if (showSwitch != old) {

                    controller.currentDestination?.apply {
                        autoManageDestinations(id)
                    }
                }
            }
        }

        return drawnUnchanged
    }

    //just to make methods available inside the paddingListener
    private fun addPaddingObserver() {
        //layout.addOnLayoutChangeListener(paddingListener)
        layout.viewTreeObserver.addOnPreDrawListener(paddingListener)
    }

    private fun removePaddingObserver() {
        //layout.removeOnLayoutChangeListener(paddingListener)
        layout.viewTreeObserver.removeOnPreDrawListener(paddingListener)
    }

    /**updates adapter that the data has been changed*/
    fun dataUpdated() {
        if (this::layout.isInitialized) {
            addPaddingObserver()
            layout.adapter?.notifyDataSetChanged()
        }
    }
}