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

package cz.lastaapps.bakalari.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cz.lastaapps.bakalari.settings.LocaleManager
import cz.lastaapps.bakalari.settings.MySettings

/**The parent of all activities
 * changed language
 * manages permissions*/
open class BaseActivity : AppCompatActivity() {

    companion object {
        private val TAG get() = BaseActivity::class.java.simpleName
    }

    override fun attachBaseContext(newBase: Context) {
        //sets up with context with changed language
        super.attachBaseContext(LocaleManager.updateLocale(newBase, MySettings.withAppContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetTitles()
    }

    //TODO move into a fragment
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //manages WRITE_STORAGE permission
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        if (permissions.contains(storagePermission)) {
            if (grantResults[permissions.indexOf(storagePermission)] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Write external storage permission granted")
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_LONG).show()
            } else {
                Log.i(TAG, "Write external storage permission denied")
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**Updates navigation appbar's titles with new language*/
    private fun resetTitles() {
        try {
            val info = packageManager.getActivityInfo(
                componentName,
                PackageManager.GET_META_DATA
            )
            if (info.labelRes != 0) {
                setTitle(info.labelRes)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    /**https://stackoverflow.com/a/58004553/12537995*/
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            val uiMode = overrideConfiguration.uiMode
            overrideConfiguration.setTo(baseContext.resources.configuration)
            overrideConfiguration.uiMode = uiMode
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }
}