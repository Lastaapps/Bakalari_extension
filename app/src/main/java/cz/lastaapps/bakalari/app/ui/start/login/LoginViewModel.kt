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

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.api.repo.absence.absenceRepository
import cz.lastaapps.bakalari.api.repo.events.eventsRepository
import cz.lastaapps.bakalari.api.repo.homework.homeworkRepository
import cz.lastaapps.bakalari.api.repo.marks.marksRepository
import cz.lastaapps.bakalari.api.repo.subjects.subjectTeacherRepository
import cz.lastaapps.bakalari.api.repo.themes.themesRepository
import cz.lastaapps.bakalari.api.repo.timetable.timetableRepository
import cz.lastaapps.bakalari.app.ui.start.login.impl.LoginService
import cz.lastaapps.bakalari.authentication.TokensAPI
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.data.LoginInfo
import cz.lastaapps.bakalari.authentication.data.Profile
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.tools.TimeTools
import kotlinx.coroutines.*
import java.time.LocalDate
import java.util.*
import kotlin.coroutines.resume
import kotlin.math.round

class LoginViewModel(val app: Application) : AndroidViewModel(app) {
    private val context = app.applicationContext

    companion object {
        private val TAG = LoginViewModel::class.simpleName
    }

    private var loginJob: Job? = null
    val loginCancelable = MutableLiveData<Boolean>()
    val loginResult = MutableLiveData<LoginResult>()

    fun cancelLogin(uuid: UUID) {
        loginJob?.cancel()
        loginJob = null
        LoginService.stopService(context, uuid)
    }

    @UiThread
    fun doLogIn(loginInfo: LoginInfo, profile: Profile, editedAccount: BakalariAccount?) {

        println("" + (loginJob != null) + " " + (loginJob?.isActive) + " " + (loginJob?.isActive == true))

        if (loginJob != null && loginJob?.isActive == true) return

        loginCancelable.value = false
        loginResult.value = LoginResult.UNSPECIFIED

        loginJob = viewModelScope.launch {
            loginImpl(loginInfo, profile, editedAccount)
            loginJob = null
        }
    }

    private suspend fun loginImpl(
        loginInfo: LoginInfo,
        profile: Profile,
        editedAccount: BakalariAccount?
    ) {
        val repo = AccountsDatabase.getDatabase(context).repository

        var editingAccount = false

        if (editedAccount != null)
            loginInfo.let { li ->
                editedAccount.let { ac ->

                    if (li.url != ac.url
                        || li.userName != ac.userName
                        || li.password != ac.password
                    ) {

                        editingAccount = true
                    } else {
                        editedAccount.let {
                            Log.i(TAG, "Updating save account")
                            loginResult.postValue(LoginResult.UPDATED_SIMPLE)
                            repo.updateAccount(
                                context, it,
                                BakalariAccount.of(loginInfo, it.toTokens(), profile)
                            )
                            return
                        }
                    }
                }
            }

        //actual login
        val result = TokensAPI(context).getNewAccessToken(loginInfo)

        Log.i(TAG, "Request done with code $result")

        loginCancelable.postValue(true)

        when (result.first) {
            //opens MainActivity
            TokensAPI.SUCCESS -> {

                val account = BakalariAccount.of(loginInfo, result.second!!, profile)

                val state = loginSuccessful(account)

                loginResult.postValue(
                    if (state) {
                        if (editingAccount)
                            LoginResult.UPDATED_IMPORTANT
                        else
                            LoginResult.SUCCESS
                    } else {
                        LoginResult.FAIL_INTERNAL_APP_LOGIN
                    }
                )
            }
            //url, username and password needed
            TokensAPI.SERVER_ERROR -> {
                loginResult.postValue(LoginResult.FAIL_WRONG_LOGIN)
            }
            //cannot connect to server
            TokensAPI.INTERNET_ERROR -> {
                loginResult.postValue(LoginResult.FAIL_INTERNET)
            }
        }
    }

    private suspend fun loginSuccessful(account: BakalariAccount) =
        suspendCancellableCoroutine<Boolean> { continuation ->

            val filter = IntentFilter(LoginService.broadcastId(account.uuid))
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    continuation.resume(intent.getBooleanExtra(LoginService.KEY_STATE, false))
                }
            }
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

            LoginService.startService(context, account)

            continuation.invokeOnCancellation {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            }
        }

    val defaultDownloadProgress = MutableLiveData(LoginFragmentViewModel.PROGRESS_ZERO)
    private var downloadJob: Job? = null
    fun downloadDefault(uuid: UUID) {

        if (downloadJob != null) return

        defaultDownloadProgress.postValue(0)

        downloadJob = viewModelScope.launch(Dispatchers.Default) {

            try {
                val database = APIBase.getDatabase(context, uuid) ?: throw IllegalArgumentException(
                    "User not found!"
                )

                val tasks = listOf<suspend (() -> Unit)>(
                    { database.absenceRepository.refreshDataAndWait() },
                    { database.eventsRepository.refreshDataAndWait() },
                    { database.homeworkRepository.refreshDataAndWait() },
                    { database.marksRepository.refreshDataAndWait() },
                    { database.subjectTeacherRepository.refreshDataAndWait() },
                    { database.themesRepository.refreshAll() },
                    {
                        database.timetableRepository.getRepositoryForDate(
                            LocalDate.now()
                        ).refreshDataAndWait()
                    },
                    {
                        database.timetableRepository.getRepositoryForDate(
                            LocalDate.now().plusWeeks(1)
                        ).refreshDataAndWait()
                    },
                    {
                        database.timetableRepository.getRepositoryForDate(
                            LocalDate.now().minusWeeks(1)
                        ).refreshDataAndWait()
                    },
                    {
                        database.timetableRepository.getRepositoryForDate(
                            TimeTools.PERMANENT
                        ).refreshDataAndWait()
                    },
                )
                yield()

                tasks.forEachIndexed { index, task ->
                    Log.i(TAG, "Working on task $index")
                    task()
                    val progress = round(100.0 * index / tasks.lastIndex).toInt()
                    defaultDownloadProgress.postValue(progress)
                    yield()
                }

                defaultDownloadProgress.postValue(LoginFragmentViewModel.PROGRESS_DONE)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                defaultDownloadProgress.postValue(LoginFragmentViewModel.PROGRESS_ERROR)
                return@launch
            }

            downloadJob = null
        }
    }
}

enum class LoginResult {
    UNSPECIFIED,

    //only the unimportant data like profile name or image have been changed
    UPDATED_SIMPLE,

    //the important data like password or username have been changed
    UPDATED_IMPORTANT,
    SUCCESS,
    FAIL_WRONG_LOGIN,
    FAIL_INTERNET,
    FAIL_INTERNAL_APP_LOGIN,
}

