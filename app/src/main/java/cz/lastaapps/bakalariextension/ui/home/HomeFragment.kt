package cz.lastaapps.bakalariextension.ui.home

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.Login
import cz.lastaapps.bakalariextension.api.timetable.Timetable
import java.util.*

class HomeFragment : Fragment() {

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_main, container, false)

        val nameView = root.findViewById<TextView>(R.id.name)
        val typeView = root.findViewById<TextView>(R.id.type)
        val schoolView = root.findViewById<TextView>(R.id.school)

        nameView.text = Login.get(Login.NAME)
        typeView.text = Login.getClassAndRole()
        schoolView.text = Login.get(Login.SCHOOL)

        //TODO
        root.findViewById<Button>(R.id.button).setOnClickListener {
            val task = object: AsyncTask<Unit, Unit, Unit>() {
                override fun doInBackground(vararg params: Unit?) {
                    Timetable.loadTimetable(Calendar.getInstance())
                }
            }
            task.execute()
        }

        return root
    }
}