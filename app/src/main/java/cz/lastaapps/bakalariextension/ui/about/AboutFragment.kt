package cz.lastaapps.bakalariextension.ui.about

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import cz.lastaapps.bakalariextension.BuildConfig
import cz.lastaapps.bakalariextension.MainActivity
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.login.Logout
import cz.lastaapps.bakalariextension.send.ReportIssueActivity
import cz.lastaapps.bakalariextension.send.SendIdeaActivity
import cz.lastaapps.bakalariextension.ui.settings.SettingsActivity
import java.util.*

class AboutFragment : Fragment() {

    companion object {
        private val TAG = "${AboutFragment::class.java.simpleName}"
    }

    private lateinit var aboutViewModel: AboutViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        aboutViewModel =
            ViewModelProviders.of(this).get(AboutViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_about, container, false)

        root.findViewById<TextView>(R.id.author).text = "${getString(R.string.author)} " +
                "${{
                    val cal = Calendar.getInstance()
                    cal.time = (BuildConfig.BUILD_TIME)
                    cal[Calendar.YEAR]
                }.invoke()}"
        root.findViewById<TextView>(R.id.version).text =
            "${BuildConfig.VERSION_NAME} ${BuildConfig.VERSION_CODE}"

        root.findViewById<ImageButton>(R.id.share).setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message) + " https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        root.findViewById<ImageButton>(R.id.rate).setOnClickListener {
            val url = "https://play.google.com/store/apps/details?id=cz.lastaapps.bakalariextension"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        root.findViewById<ImageButton>(R.id.facebook).setOnClickListener {
            val url = "https://www.facebook.com/lastaapps/"
            var uri = Uri.parse(url)
            try {
                val applicationInfo =
                    activity!!.packageManager.getApplicationInfo("com.facebook.katana", 0)
                if (applicationInfo.enabled) {
                    uri = Uri.parse("fb://facewebmodal/f?href=$url")
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        root.findViewById<ImageButton>(R.id.google_play).setOnClickListener {
            //TODO
            val url = "https://play.google.com/store/apps/dev?id=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        root.findViewById<ImageButton>(R.id.github).setOnClickListener {
            val url = "https://github.com/lastaapps/bakalari_extension"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
        root.findViewById<ImageButton>(R.id.api).setOnClickListener {
            val url = "https://github.com/bakalari-api"
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        return root
    }
}