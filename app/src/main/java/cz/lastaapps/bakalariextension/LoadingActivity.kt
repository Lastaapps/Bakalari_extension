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

package cz.lastaapps.bakalariextension

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import cz.lastaapps.bakalariextension.login.LoginActivity
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.BaseActivity
import cz.lastaapps.bakalariextension.tools.CheckInternet
import kotlinx.coroutines.*
import java.lang.Runnable

/**Checks if app has been ever started -> license, if user is logged in -> LoginActivity or -> MainActivity*/
class LoadingActivity : BaseActivity() {

    companion object {
        private val TAG = LoadingActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Creating LoadingActivity")

        if (!isChangingConfigurations)
            setContentView(R.layout.activity_loading)
    }

    override fun onResume() {
        super.onResume()

        if (!isChangingConfigurations) {
            decision()
        }
    }

    /**Decides if app should be started or user need to accept licence or log in*/
    private fun decision() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            if (LoginData.isLoggedIn() && Licence.check()) {

                Log.i(TAG, "Launching from saved data")

                withContext(Dispatchers.Main) {

                    //let oder work to be finished on UI Thread first, then co doer things
                    for (i in 0 until 10)
                        yield()

                    //starts main activity
                    startActivity(mainActivityIntent())

                    //let finish more important work on UI thread
                    for (i in 0 until 20)
                        yield()

                    //closes
                    finish()
                }
            } else {

                Log.i(TAG, "Not logged in yet")

                onNotLoggedIn()
            }
        }
    }

    /**What to do if user in not logged in*/
    private suspend fun onNotLoggedIn() {

        //checks if user has accepted the licence
        if (!Licence.check()) {
            withContext(Dispatchers.Main) {
                //shows licence dialog
                Licence.showDialog(this@LoadingActivity, Runnable {
                    decision()
                })
            }
            return
        }

        //check if there is internet connection, so if user can log in
        if (!CheckInternet.check()) {

            withContext(Dispatchers.Main) {
                Log.i(TAG, "Cannot connect to internet")

                //no internet, no log in
                Toast.makeText(
                    this@LoadingActivity,
                    R.string.error_no_internet,
                    Toast.LENGTH_LONG
                ).show()

                if (!(this@LoadingActivity.isDestroyed || this@LoadingActivity.isFinishing))
                    AlertDialog.Builder(this@LoadingActivity)
                        .setMessage(R.string.error_no_saved_data)
                        .setCancelable(false)
                        .setPositiveButton(R.string.close) { _: DialogInterface, _: Int -> finish() }
                        .create()
                        .show()
            }
        } else {
            //opens LoginActivity
            Log.i(TAG, "Internet working, opening login")
            this.startActivity(Intent(App.context, LoginActivity::class.java))
        }
    }

    /**@return Intent, which can start MainActivity, with extra about what fragment to show*/
    private fun mainActivityIntent(): Intent {
        //used for launching activity and setting default fragment, when i want for example show timetable
        return Intent(this, MainActivity::class.java).apply {
            //extra needs to be passed from local Intent
            if (intent.getIntExtra(MainActivity.NAVIGATE, -1) != -1)
                putExtra(
                    MainActivity.NAVIGATE,
                    intent.getIntExtra(MainActivity.NAVIGATE, -1)
                )
        }
    }
}
