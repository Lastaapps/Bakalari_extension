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

package cz.lastaapps.bakalari.app.ui.others

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.tools.getVersionCode
import cz.lastaapps.bakalari.tools.getVersionName
import cz.lastaapps.common.Communication
import cz.lastaapps.common.DeveloperInfo
import cz.lastaapps.common.PlayStoreReview

class AboutDialog : BottomSheetDialogFragment() {

    companion object {
        private val TAG get() = AboutDialog::class.java.simpleName
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view for AboutFragment")

        val root = inflater.inflate(R.layout.fragment_about, container, false)
        val context = requireContext()

        //sets for example Lasta apps 2020
        root.findViewById<TextView>(R.id.author).text = DeveloperInfo.getNameAndBuildYear(context)

        //info about current app version
        root.findViewById<TextView>(R.id.version).text =
            "${context.getVersionName()} ${context.getVersionCode()}"

        //share app
        root.findViewById<ImageButton>(R.id.share).setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.share_message) + " https://play.google.com/store/apps/details?id=cz.lastaapps.bakalari.app"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        //rate app
        root.findViewById<ImageButton>(R.id.rate).setOnClickListener {
            PlayStoreReview.doInAppReview(requireActivity())
        }
        //view Facebook page
        root.findViewById<ImageButton>(R.id.facebook).setOnClickListener {
            Communication.openFacebook(requireContext())
        }
        //show all my apps
        root.findViewById<ImageButton>(R.id.google_play).setOnClickListener {
            Communication.openPlayStore(requireContext())
        }
        //source code
        root.findViewById<ImageButton>(R.id.github).setOnClickListener {
            Communication.openProjectsGithub(requireContext(), "bakalari_extension")
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
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(R.id.nav_license)
        }

        return root
    }
}

