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

package cz.lastaapps.bakalariextension.ui.about

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import cz.lastaapps.bakalariextension.BuildConfig
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.ui.WhatsNew
import cz.lastaapps.bakalariextension.ui.license.LicenseActivity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AboutFragment : Fragment() {

    companion object {
        private val TAG = AboutFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view for AboutFragment")

        val root = inflater.inflate(R.layout.fragment_about, container, false)

        //sets for example Lasta apps 2020
        root.findViewById<TextView>(R.id.author).text = "${getString(R.string.author)} " +
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(BuildConfig.BUILD_TIME), ZoneId.of("UTC")
                    ).format(DateTimeFormatter.ofPattern("yyyy"))

        //info about current app version
        root.findViewById<TextView>(R.id.version).text =
            "${BuildConfig.VERSION_NAME} ${BuildConfig.VERSION_CODE}"

        //share app
        root.findViewById<ImageButton>(R.id.share).setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message) + " https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        //rate app
        root.findViewById<ImageButton>(R.id.rate).setOnClickListener {
            val url = "https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        //view Facebook page
        root.findViewById<ImageButton>(R.id.facebook).setOnClickListener {
            val url = "https://www.facebook.com/lastaapps/"
            var uri = Uri.parse(url)
            try {
                val applicationInfo =
                    requireActivity().packageManager.getApplicationInfo("com.facebook.katana", 0)
                if (applicationInfo.enabled) {
                    uri = Uri.parse("fb://facewebmodal/f?href=$url")
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        //show all my apps
        root.findViewById<ImageButton>(R.id.google_play).setOnClickListener {
            //TODO add play store link
            val url = "https://play.google.com/store/apps/dev?id=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        //source code
        root.findViewById<ImageButton>(R.id.github).setOnClickListener {
            val url = "https://github.com/lastaapps/bakalari_extension"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        //API link
        root.findViewById<ImageButton>(R.id.api).setOnClickListener {
            val url = "https://github.com/bakalari-api"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        //view whats new
        root.findViewById<ImageButton>(R.id.whats_new).setOnClickListener {
            WhatsNew(requireContext()).showDialog()
        }
        //view license
        root.findViewById<ImageButton>(R.id.license).setOnClickListener {
            requireActivity().startActivity(Intent(requireContext(), LicenseActivity::class.java))
        }

        return root
    }
}