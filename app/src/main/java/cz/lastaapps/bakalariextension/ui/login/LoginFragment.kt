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

package cz.lastaapps.bakalariextension.ui.login

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.FragmentLoginBinding
import cz.lastaapps.bakalariextension.send.ReportIssueActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

/**
 * User interface for login to server
 */
class LoginFragment : Fragment() {

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }

    /**Contains apps ui data*/
    private val viewModel: LoginViewModel by activityViewModels()
    private lateinit var binding: FragmentLoginBinding
    private var wasAppBarShown = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity() as MainActivity).supportActionBar?.let {
            wasAppBarShown = it.isShowing
            it.hide()
        }

        viewModel.townList.observe({ lifecycle }) { onTownsLoaded() }
        viewModel.loadTownsIfNeeded()

        viewModel.selectedTown.observe({ lifecycle }) { onTownSelected() }

        viewModel.selectedSchool.observe({ lifecycle }) { it?.let { onSchoolSelected(it) } }
    }

    override fun onDestroy() {
        super.onDestroy()

        (requireActivity() as MainActivity).supportActionBar?.let {
            if (wasAppBarShown)
                it.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating login fragment")

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.viewmodel = viewModel

        binding.url.setText(LoginData.url)
        binding.username.setText(LoginData.username)

        //when enter pressed in password field
        binding.password.setOnEditorActionListener { _, _, _ ->
            login()
            true
        }

        binding.login.setOnClickListener {
            login()
        }

        //opens settings
        binding.openSettings.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.nav_settings)
        }

        binding.townSelected.setOnClickListener {
            SearchFragment.initialize(true).show(childFragmentManager, TAG + "_town")
        }
        binding.schoolSelected.setOnClickListener {
            SearchFragment.initialize(false).show(childFragmentManager, TAG + "_school")
        }

        //opens bug report
        binding.reportIssue.setOnClickListener {
            val intent = Intent(requireContext(), ReportIssueActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    //selects default school if there isn't any
    private fun onTownsLoaded() {
        if (viewModel.selectedTown.value == null) {

            val savedName = LoginData.town
            val list = viewModel.townList.value!!

            if (savedName != "") {
                for (town in list) {
                    if (town.name == savedName) {
                        viewModel.selectedTown.value = town
                        return
                    }
                }
            }

            //chooses the first town as default
            viewModel.selectedTown.value = list[0]
        }
    }

    private fun onTownSelected() {

        viewModel.selectedSchool.value = null

        val data = viewModel.getSchoolList(viewModel.selectedTown.value!!)
        if (data.value != null) {
            onSchoolLoaded(data.value!!)
        } else {
            data.observe({ lifecycle }) { onSchoolLoaded(it) }
        }
    }

    private fun onSchoolLoaded(list: List<School>) {
        if (viewModel.selectedTown.value == list[0].town) {

            val savedName = LoginData.school

            if (savedName != "") {
                for (school in list) {
                    if (school.name == savedName) {
                        viewModel.selectedSchool.value = school
                        return
                    }
                }
            }

            //chooses the first town as default
            viewModel.selectedSchool.value = list[0]
        }
    }

    private fun onSchoolSelected(school: School) {
        binding.url.setText(school.url)
    }

    /**Tries to login to server with given data through LoginToServer class*/
    private fun login() {
        Log.i(TAG, "Trying to log in")
        val dialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setMessage(R.string.login_connecting)
            .create()

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            val loginToServer = binding.run {
                LoginToServer(
                    username.text.toString(),
                    password.text.toString(),
                    url.text.toString(),
                    viewModel.selectedTown.value?.name ?: "",
                    viewModel.selectedSchool.value?.name ?: ""
                )
            }

            //actual login
            val result = loginToServer.run()

            Log.i(TAG, "Request done with code $result")

            withContext(Dispatchers.Main) {

                //login done
                dialog.dismiss()

                when (result) {
                    //opens MainActivity
                    LoginToServer.VALID_TOKEN -> {
                        Toast.makeText(
                            requireContext(), R.string.login_succeeded, Toast.LENGTH_LONG
                        ).show()
                        startActivity(
                            Intent(requireContext(), MainActivity::class.java)
                        )
                        requireActivity().finish()
                    }
                    //url, username and password needed
                    LoginToServer.NOT_ENOUGH_DATA -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.login_error_not_enough_data,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    //cannot connect to server
                    LoginToServer.NO_INTERNET -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.login_error_server_unavailable,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    //wrong username or password
                    LoginToServer.WRONG_LOGIN -> {
                        Toast.makeText(
                            requireContext(), R.string.login_error_wrong_input, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        //shows dialog until login is finished
        dialog.show()
    }


    class Town(
        var name: String,
        var schoolNumber: Int
    ) : Comparable<Town> {
        var schools: List<School>? = null

        override fun equals(other: Any?): Boolean {
            if (other !is Town) return false
            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun toString(): String {
            return "$name ($schoolNumber)"
        }

        override fun compareTo(other: Town): Int {
            val collator = Collator.getInstance(Locale.getDefault())

            return collator.compare(name, other.name)
        }
    }

    class School(
        var town: Town,
        var id: String,
        var name: String,
        var url: String
    ) : Comparable<School> {
        override fun equals(other: Any?): Boolean {
            if (other !is School) return false
            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun toString(): String {
            return name
        }

        override fun compareTo(other: School): Int {
            val collator = Collator.getInstance(Locale.getDefault())

            return collator.compare(name, other.name)
        }
    }
}


