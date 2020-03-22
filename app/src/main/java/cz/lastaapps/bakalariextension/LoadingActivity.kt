package cz.lastaapps.bakalariextension

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.DialogInterface
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import cz.lastaapps.bakalariextension.api.User
import cz.lastaapps.bakalariextension.api.timetable.TTNotifiService
import cz.lastaapps.bakalariextension.api.timetable.TTStorage
import cz.lastaapps.bakalariextension.login.LoginActivity
import cz.lastaapps.bakalariextension.login.LoginData
import cz.lastaapps.bakalariextension.tools.CheckInternet
import cz.lastaapps.bakalariextension.tools.MyToast
import cz.lastaapps.bakalariextension.tools.TimeTools
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity

/**Checks if app has been ever started -> license, if user is logged in -> LoginActivity or -> MainActivity*/
class LoadingActivity : AppCompatActivity() {

    companion object {
        private val TAG = LoadingActivity::class.java.simpleName

        private const val SHOULD_RUN = "should_run"
    }

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //onCreate was called multiple times because of recreation
        // caused by theme change
        if (savedInstanceState == null ||
            savedInstanceState.getBoolean(SHOULD_RUN, true)
        ) {
            SettingsActivity.updateDarkTheme()
            SettingsActivity.initSettings()
            SettingsActivity.updateLanguage(this)

            setContentView(R.layout.activity_loading)

            //deletes old timetables
            TTStorage.deleteOld(
                TimeTools.previousWeek(
                    TimeTools.cal))

            initNotificationChannels()

            //firebase init
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

            LoadingTask().execute()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHOULD_RUN, false)
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.timetable_chanel_name)
            val descriptionText = getString(R.string.timetable_chanel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(
                TTNotifiService.NOTIFICATION_CHANEL_ID,
                name,
                importance
            )
            mChannel.description = descriptionText
            mChannel.setShowBadge(false)
            mChannel.setSound(null, null)
            mChannel.enableVibration(false)
            mChannel.enableLights(false)
            mChannel.importance = NotificationManager.IMPORTANCE_HIGH

            val notificationManager =
                getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            //notificationManager.deleteNotificationChannel(mChannel.id)
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    /**checks for internet, token and loads data or redirects to needed activity*/
    private inner class LoadingTask : AsyncTask<Any, Any, Intent?>() {

        override fun doInBackground(vararg params: Any?): Intent? {

            if (!Licence.check()) {
                Handler(Looper.getMainLooper()).post {
                    Licence.showDialog(this@LoadingActivity, Runnable {
                        Handler(Looper.getMainLooper()).postDelayed(
                            { LoadingTask().execute() },
                            100
                        )
                    })
                }
                return null
            }

            //checks, if there are at least some data saved
            if (!CheckInternet.check()) {

                Log.i(TAG, "Cannot connect to server")

                MyToast.makeText(
                    this@LoadingActivity,
                    R.string.error_no_internet,
                    MyToast.LENGTH_LONG
                ).show()

                return if (User.get(User.NAME) == "") {

                    Log.i(TAG, "No data saved yet")

                    Handler(Looper.getMainLooper()).post {
                        if (!(this@LoadingActivity.isDestroyed || this@LoadingActivity.isFinishing))
                            AlertDialog.Builder(this@LoadingActivity)
                                .setMessage(R.string.error_no_saved_data)
                                .setCancelable(false)
                                .setPositiveButton(R.string.close) { _: DialogInterface, _: Int -> finish() }
                                .create()
                                .show()
                    }
                    null
                } else {
                    Log.i(TAG, "Launching from saved data")

                    launchServices()

                    mainActivityIntent()
                }
            }
            Log.i(TAG, "Internet is working")

            val token = LoginData.accessToken
            if (token == "") {
                //not logged in
                Log.i(TAG, "No token yet")

                return Intent(this@LoadingActivity, LoginActivity::class.java)

            } else {

                launchServices()

                return mainActivityIntent()
            }
        }

        override fun onPostExecute(intent: Intent?) {
            if (intent != null) {
                startActivity(intent)
                finish()
            }
        }
    }

    private fun launchServices() {
        Handler(Looper.getMainLooper()).post {
            TTNotifiService.startService(this@LoadingActivity)
        }
    }

    private fun mainActivityIntent(): Intent {
        //used for launching activity and setting default fragment, when i want for example show timetable
        return Intent(this@LoadingActivity, MainActivity::class.java).apply {
            if (intent.getIntExtra(MainActivity.NAVIGATE, -1) != -1)
                putExtra(
                    MainActivity.NAVIGATE,
                    intent.getIntExtra(MainActivity.NAVIGATE, -1)
                )
        }
    }
}
