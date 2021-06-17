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

package cz.lastaapps.bakalari.app.ui.start.profiles

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import cz.lastaapps.bakalari.app.NavGraphRootDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentProfileBinding
import cz.lastaapps.bakalari.app.ui.start.login.profilesViewModels
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import java.util.*

class ProfilesFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfilesViewModel by profilesViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.viewModel = viewModel

        binding.list.adapter = ProfileAdapter(viewModel, onClick = onClick)

        viewModel.accounts.observe({ lifecycle }) { list ->
            updateData(list)
            //navigates to login fragment when there is no account presented
            if (list.isEmpty()) {
                findNavController().apply {
                    //somehow called randomly twice causing crash
                    if (currentDestination?.id == R.id.nav_profiles)
                        navigate(ProfilesFragmentDirections.actionLogin(null))
                }
            }
        }

        binding.add.setOnClickListener {
            findNavController().navigate(ProfilesFragmentDirections.actionLogin(null))
        }

        binding.edit.setOnClickListener {
            viewModel.editingMode.value = viewModel.editingMode.value != true
        }

        //opens settings
        binding.openSettings.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment)
                .navigate(R.id.nav_graph_settings)
        }

        //opens bug report
        binding.reportIssue.setOnClickListener {
            val args = bundleOf("uuid" to null)
            findNavController().navigate(cz.lastaapps.bakalari.report.R.id.report_navigation, args)
        }

        //shows the description on the auto launch function
        binding.launchDefaultHelp.setOnClickListener {
            findNavController().navigate(ProfilesFragmentDirections.actionProfilesToLaunchHelp())
        }

        return binding.root
    }

    private val onClick = { view: View, account: BakalariAccount, position: Int, state: Boolean ->
        when (view.id) {
            R.id.edit -> {
                findNavController().navigate(ProfilesFragmentDirections.actionLogin(account))
            }
            R.id.delete -> {
                findNavController().navigate(
                    ProfilesFragmentDirections.actionConfirmDeleteAccount(account)
                )
            }
            else -> {
                openUser(account.uuid)
            }
        }
    }

    private fun updateData(list: List<BakalariAccount>) {
        (binding.list.adapter as ProfileAdapter).update(list)
    }

    private fun openUser(uuid: UUID) {
        findNavController().navigate(NavGraphRootDirections.actionLoading(uuid, false))
    }
}