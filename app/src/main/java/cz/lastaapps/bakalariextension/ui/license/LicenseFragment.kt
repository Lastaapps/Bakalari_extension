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

package cz.lastaapps.bakalariextension.ui.license

import android.app.backup.BackupManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.databinding.FragmentLicenseBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.ZoneId
import java.time.ZonedDateTime

class LicenseFragment : Fragment() {

    private lateinit var binding: FragmentLicenseBinding
    private var wasAppBarShown = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (requireActivity() as MainActivity).supportActionBar?.let {
            wasAppBarShown = it.isShowing
            it.hide()
        }
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

        Log.i(TAG, "Creating view")

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_license, container, false)
        binding.license = this

        loadPackages()
        loadLicenseContent()

        return binding.root
    }

    /**Loads packages manes*/
    private fun loadPackages() {
        val licensePackages = arrayOf(
            R.array.license_gnu3,
            R.array.license_apache,
            R.array.license_mit,
            R.array.license_mozilla
        )
        val licenseViews = arrayOf(
            binding.usagesGnu3,
            binding.usagesApache,
            binding.usagesMit,
            binding.usagesMozilla
        )

        licensePackages.forEachIndexed { i: Int, it: Int ->
            val stringBuilder = StringBuilder()
            resources.getStringArray(it).forEach {
                stringBuilder.append(it)
                stringBuilder.append('\n')
            }

            licenseViews[i].text = stringBuilder.toString()
        }
    }

    /**Loads licence content*/
    private fun loadLicenseContent() {
        val licenseViews = arrayOf(
            binding.textGnu3,
            binding.textApache,
            binding.textMit,
            binding.textMozilla
        )
        val assetNames = arrayOf(
            "gpl-3.0.txt",
            "apache-2.0.txt",
            "mit.txt",
            "mozilla-2.0.txt"
        )

        for (i in licenseViews.indices) {
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(
                    InputStreamReader(
                        requireActivity().assets.open("license/${assetNames[i]}"),
                        "UTF-8"
                    )
                )

                licenseViews[i].text = reader.readText()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**Shows full licence or hides the most of it*/
    fun onShowHideText(button: TextView, textView: TextView) {
        if (textView.visibility == View.GONE) {
            button.setText(R.string.license_hide)
            textView.visibility = View.VISIBLE
        } else {
            button.setText(R.string.license_show)
            textView.visibility = View.GONE
        }
    }

    companion object {
        private val TAG = LicenseFragment::class.java.simpleName

        private const val SP_KEY = "LICENSE"
        private const val SP_AGREED = "1.0"
        private const val SP_TIME = "1.0_date"

        /**@return if user has agreed to license*/
        fun check(): Boolean {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getBoolean(SP_AGREED, false)
        }

        /**called when user has agreed to license*/
        private fun agreed() {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putBoolean(SP_AGREED, true)
                putLong(SP_TIME, ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli())
                apply()
            }

            //notifies that backup should be made
            BackupManager.dataChanged(App.context.packageName)
        }

        /**Shows dialog, which asks user to agree the license*/
        fun showDialog(activity: FragmentActivity, run: Runnable) {
            AlertDialog.Builder(activity)
                .setCancelable(false)
                .setMessage(R.string.license_agreement)
                .setPositiveButton(R.string.license_agree)
                { _: DialogInterface, _: Int ->
                    agreed()
                    run.run()
                }
                .setNegativeButton(R.string.license_view) { _: DialogInterface, _: Int ->
                    viewLicense(
                        activity
                    )
                }
                .setTitle(R.string.license)
                .create()
                .show()
        }

        /**Shows GNU license to user*/
        fun viewLicense(activity: FragmentActivity) {
            activity.findNavController(R.id.nav_host_fragment).navigate(R.id.nav_license)
        }
    }
}