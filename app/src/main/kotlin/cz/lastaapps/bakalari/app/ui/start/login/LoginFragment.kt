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

package cz.lastaapps.bakalari.app.ui.start.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import cz.lastaapps.bakalari.app.NavGraphRootDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentLoginBinding
import cz.lastaapps.bakalari.app.ui.user.CurrentUser
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.data.LoginInfo
import cz.lastaapps.bakalari.authentication.data.Profile
import cz.lastaapps.bakalari.authentication.data.getRawUrl
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.authentication.database.CanCreateAccount
import cz.lastaapps.bakalari.tools.TimeTools
import cz.lastaapps.bakalari.tools.normalizeID
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime


/**
 * User interface for login to server
 */
class LoginFragment : Fragment() {

    companion object {
        private val TAG = LoginFragment::class.java.simpleName

        private val IMAGE_REQUEST_CODE = R.id.request_code_login_image.normalizeID()
    }

    /**Contains apps ui data*/
    private val viewModel: LoginFragmentViewModel by loginViewModels()
    private lateinit var binding: FragmentLoginBinding
    private val args: LoginFragmentArgs by navArgs()

    private val accountsDatabase by lazy { AccountsDatabase.getDatabase(requireContext()) }
    private val accountsRepo by lazy { accountsDatabase.repository }
    private val editedAccount by lazy { args.account }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating login fragment")

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.setLifecycleOwner { lifecycle }
        binding.vm = viewModel

        //when enter pressed in password field
        binding.password.setOnEditorActionListener { _, actionId, event ->

            //ACTION_DOWN needs to be consumed for ACTION_UP to occur
            if (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_DOWN) return@setOnEditorActionListener true

            if (actionId == EditorInfo.IME_ACTION_DONE || (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_UP)) {
                login()
                return@setOnEditorActionListener true
            }

            false
        }

        binding.login.setOnClickListener {
            login()
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

        binding.townSelected.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginSearch(true))
        }
        binding.schoolSelected.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginSearch(false))
        }

        binding.savePasswordHelp.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginToSavePasswordHelp())
        }

        binding.profilePicture.setOnClickListener {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            startActivityForResult(chooserIntent, IMAGE_REQUEST_CODE)
        }

        binding.forgottenPassword.setOnClickListener {
            val url = binding.url.text?.toString()
            if (url != null && url != "") {
                findNavController().navigate(LoginFragmentDirections.actionForgottenPassword(url.getRawUrl()))
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.login_forgotten_password_select_school,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.cancel.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.setUpWithAccount(editedAccount)

        return binding.root
    }


    /**Tries to login to server with given data through LoginToServer class*/
    private fun login() = lifecycleScope.launch {
        Log.i(TAG, "Trying to log in")

        viewModel.let {
            val notEnoughDataToast by lazy {
                {
                    Log.e(TAG, "Not enough data was entered")
                    Toast.makeText(
                        requireContext(), R.string.login_error_not_enough_data, Toast.LENGTH_LONG
                    ).show()
                }
            }

            val selectedTown = it.selectedTown.value
            val selectedSchool = it.selectedSchool.value
            val profileImageUri = it.profilePictureUri.value
            val profileName = it.textProfileName.value!!.trim()
            val url = BakalariAccount.validateUrl(it.textUrl.value!!.trim()).getRawUrl()
            val userName = it.textUsername.value!!.trim()
            val password = it.textPassword.value!!.trim()
            val savePassword = it.savePassword.value == true


            //TODO kontrola validity URL
            //TODO nastavovat chyby na základě zpětné vazby z dalších lokací

            if (profileName == "") {
                it.errorProfileName.value = getString(R.string.login_field_required)
                notEnoughDataToast()
                return@launch
            }

            if (url == "") {
                it.errorUrl.value = getString(R.string.login_field_required)
                notEnoughDataToast()
                return@launch
            }

            if (userName == "") {
                it.errorUsername.value = getString(R.string.login_field_required)
                notEnoughDataToast()
                return@launch
            }

            if (password == "" && editedAccount == null) {
                //TODO if the password is already saved, ignore
                it.errorPassword.value = getString(R.string.login_field_required)
                notEnoughDataToast()
                return@launch
            }

            val uuid = editedAccount?.let { it.uuid } ?: accountsRepo.newUUID()
            val loginInfo = LoginInfo(
                uuid,
                userName,
                password,
                savePassword,
                url,
                selectedTown?.name,
                selectedSchool?.name,
            )

            val profile = Profile(
                uuid,
                ZonedDateTime.ofInstant(Instant.EPOCH, TimeTools.UTC),
                ZonedDateTime.now(),
                profileName,
                profileImageUri,
                0,
                false
            )

            if (editedAccount == null)
                when (accountsRepo.canCreateAccount(url, userName, profileName)) {
                    CanCreateAccount.ALLOWED -> {
                    }
                    CanCreateAccount.USERNAME_EXISTS -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.login_already_exists_credentials,
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    CanCreateAccount.PROFILE_NAME_EXISTS -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.login_already_exists_profile,
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                }

            findNavController().navigate(
                LoginFragmentDirections.actionDoLogin(
                    loginInfo,
                    profile,
                    editedAccount
                )
            )

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (IMAGE_REQUEST_CODE == requestCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (data?.data == null) return

                viewModel.processImage(data.data!!)
            }
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }
}

inline fun <reified T : ViewModel> Fragment.profilesViewModels() =
    navGraphViewModels<T>(R.id.nav_graph_profiles)

inline fun <reified T : ViewModel> Fragment.loginViewModels() =
    navGraphViewModels<T>(cz.lastaapps.bakalari.authentication.R.id.nav_graph_login)

