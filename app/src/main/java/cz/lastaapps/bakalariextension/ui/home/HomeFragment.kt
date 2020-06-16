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

package cz.lastaapps.bakalariextension.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.databinding.FragmentHomeBinding
import cz.lastaapps.bakalariextension.ui.WhatsNew

class HomeFragment : Fragment() {

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }

    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //What's new - shown only once per version
        if (WhatsNew(requireContext()).shouldShow()) {
            WhatsNew(requireContext()).showDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating HomeFragment")

        //inflates layout
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        //sets up info about user
        binding.apply {
            name.text = User.get(User.NAME)
            type.text = User.getClassAndRole()
            school.text = User.get(User.SCHOOL)
        }

        return binding.root
    }
}