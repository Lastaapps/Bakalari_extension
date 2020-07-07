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

package cz.lastaapps.bakalariextension.ui.subjects

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import cz.lastaapps.bakalariextension.MobileNavigationDirections
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.absence.data.AbsenceSubject
import cz.lastaapps.bakalariextension.api.homework.data.Homework
import cz.lastaapps.bakalariextension.api.subjects.data.Subject
import cz.lastaapps.bakalariextension.api.user.data.User
import cz.lastaapps.bakalariextension.databinding.FragmentSubjectInfoBinding
import cz.lastaapps.bakalariextension.ui.UserViewModel
import cz.lastaapps.bakalariextension.ui.absence.AbsenceViewModel
import cz.lastaapps.bakalariextension.ui.homework.HmwAdapter
import cz.lastaapps.bakalariextension.ui.homework.HmwViewModel
import cz.lastaapps.bakalariextension.ui.marks.MarksAdapter
import cz.lastaapps.bakalariextension.ui.marks.MarksViewModel

/**shows info about subject - marks, homework list, theme, absence*/
class SubjectInfoFragment : Fragment() {

    companion object {
        private val TAG = SubjectInfoFragment::class.java.simpleName

        private const val SUBJECT_EXTRA = "SUBJECT_EXTRA"

        /**shows info about subject in nav_host_fragment*/
        fun navigateTo(activity: FragmentActivity, subjectId: String) {
            navigateTo(activity.findNavController(R.id.nav_host_fragment), subjectId)
        }

        /**shows info about subject in nav_host_fragment*/
        fun navigateTo(navController: NavController, subjectId: String) {
            val arguments = Bundle().apply {
                putString(SUBJECT_EXTRA, subjectId)
            }

            val options = NavOptions.Builder().setLaunchSingleTop(true).build()
            navController.navigate(R.id.nav_subject_info, arguments, options)
        }
    }

    private lateinit var binding: FragmentSubjectInfoBinding
    private val subjectViewModel: SubjectViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val absenceViewModel: AbsenceViewModel by activityViewModels()
    private val homeworkViewModel: HmwViewModel by activityViewModels()
    private val marksViewModel: MarksViewModel by activityViewModels()
    private lateinit var themeViewModel: ThemeViewModel
    private val args: SubjectInfoFragmentArgs by navArgs()

    private lateinit var subject: Subject
    private lateinit var subjectId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //views setup
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subject_info, container, false)
        binding.setLifecycleOwner { lifecycle }

        binding.subjectViewModel = subjectViewModel

        //RecyclerView adapter setup
        binding.themeLayout.list.adapter = ThemeAdapter()

        //loads arguments
        subjectId = args.subjectId

        //loads subjects
        subjectViewModel.executeOrRefresh(lifecycle) { showData() }

        //used to filter modules when they are disabled
        val user = userViewModel.requireData()

        user.runIfFeatureEnabled(User.HOMEWORK_SHOW) {
            //loads homework list
            homeworkViewModel.executeOrRefresh(lifecycle) { showHomeworkList() }
        }

        user.runIfFeatureEnabled(User.MARKS_SHOW) {
            //loads marks
            marksViewModel.executeOrRefresh(lifecycle) { showMarks() }
        }

        user.runIfFeatureEnabled(User.SUBJECTS_SHOW_THEMES) {
            themeViewModel = subjectViewModel.getThemeViewModelForSubject(subjectId)

            binding.themeLayout.viewmodel = themeViewModel

            //loads themes
            themeViewModel.executeOrRefresh(lifecycle) { showThemes() }
        }

        return binding.root
    }

    /**shows basic data - subject name, teacher*/
    private fun showData() {
        Log.i(TAG, "Showing data")

        val subject = subjectViewModel.subjects.value!!.getById(subjectId)

        if (subject == null) {

            Log.e(TAG, "Subject $subjectId not found!")
            Toast.makeText(requireContext(), R.string.subject_not_found, Toast.LENGTH_LONG).show()
            requireActivity().findNavController(R.id.nav_host_fragment).apply {
                if (currentDestination?.id == R.id.nav_subject_info) {
                    navigateUp()
                }
            }

            return
        }

        this.subject = subject
        binding.subject = subject

        binding.teacher.setOnClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment).navigate(
                MobileNavigationDirections.actionTeacherInfo(subject.teacher.id)
            )
        }

        //shows absence for subject given
        absenceViewModel.executeOrRefresh(lifecycle) { showAbsence() }
    }

    private fun showAbsence() {
        Log.i(TAG, "Showing absence")

        val data = absenceViewModel.requireData()
        for (sbj in data.subjects) {

            if (sbj.name == subject.name) {

                binding.absence.apply {
                    root.visibility = View.VISIBLE

                    //replaces normally shown subject name with label Absence:
                    val fakeSubject = sbj.run {
                        AbsenceSubject(
                            getString(R.string.absence_subject_info_label),
                            lessonCount, base, late, soon, school
                        )
                    }

                    subject = fakeSubject
                    threshold = data.percentageThreshold
                }

                break
            }
        }
    }

    /**shows homework list*/
    private fun showHomeworkList() {
        Log.i(TAG, "Showing homework list")

        val homeworkList = Homework.getBySubject(homeworkViewModel.homework.value!!, subjectId)

        if (homeworkList.isNotEmpty()) {
            binding.homeworkBox.visibility = View.VISIBLE
            binding.homeworkList.adapter =
                HmwAdapter(requireActivity() as AppCompatActivity, homeworkList)
        }
    }

    /**shows marks*/
    private fun showMarks() {
        Log.i(TAG, "Showing marks")

        val subjectMarks = marksViewModel.marks.value!!.getMarksForSubject(subjectId) ?: return

        if (subjectMarks.marks.isNotEmpty()) {
            val average = subjectMarks.averageText
            val text = getString(R.string.marks_average) + ": " + average

            binding.marksBox.visibility = View.VISIBLE
            binding.marksAverage.text = text
            binding.list.adapter = MarksAdapter(subjectMarks.marks)
        }
    }

    /**shows themes*/
    private fun showThemes() {
        Log.i(TAG, "Showing themes")

        (binding.themeLayout.list.adapter as ThemeAdapter)
            .update(themeViewModel.data.value!!)
    }
}