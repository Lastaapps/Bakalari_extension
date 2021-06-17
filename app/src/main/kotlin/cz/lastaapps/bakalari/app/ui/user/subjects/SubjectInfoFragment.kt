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

package cz.lastaapps.bakalari.app.ui.user.subjects

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import cz.lastaapps.bakalari.api.entity.absence.AbsenceSubject
import cz.lastaapps.bakalari.api.entity.homework.HomeworkList
import cz.lastaapps.bakalari.api.entity.marks.MarksPair
import cz.lastaapps.bakalari.api.entity.subjects.Subject
import cz.lastaapps.bakalari.api.entity.subjects.Teacher
import cz.lastaapps.bakalari.api.entity.user.User
import cz.lastaapps.bakalari.app.NavGraphUserDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentSubjectInfoBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.UserViewModel
import cz.lastaapps.bakalari.app.ui.user.absence.AbsenceViewModel
import cz.lastaapps.bakalari.app.ui.user.homework.HmwAdapter
import cz.lastaapps.bakalari.app.ui.user.homework.HmwViewModel
import cz.lastaapps.bakalari.app.ui.user.marks.MarksAdapter
import cz.lastaapps.bakalari.app.ui.user.marks.MarksViewModel
import cz.lastaapps.bakalari.app.ui.user.themes.ThemeAdapter
import cz.lastaapps.bakalari.app.ui.user.themes.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**shows info about subject - marks, homework list, theme, absence*/
class SubjectInfoFragment : Fragment() {

    companion object {
        private val TAG get() = SubjectInfoFragment::class.java.simpleName

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
    private val subjectViewModel: SubjectViewModel by accountsViewModels()
    private val userViewModel: UserViewModel by accountsViewModels()
    private val absenceViewModel: AbsenceViewModel by accountsViewModels()
    private val homeworkViewModel: HmwViewModel by accountsViewModels()
    private val marksViewModel: MarksViewModel by accountsViewModels()
    private lateinit var themeViewModel: ThemeViewModel
    private val args: SubjectInfoFragmentArgs by navArgs()

    private lateinit var subject: Subject
    private lateinit var subjectId: String

    private lateinit var teacher: Teacher

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //loads arguments
        subjectId = args.subjectId
        val tempSubject = runBlocking(Dispatchers.IO) {
            subjectViewModel.getSubject(subjectId)
        }

        if (tempSubject == null) {

            Log.e(TAG, "Subject $subjectId not found!")
            Toast.makeText(requireContext(), R.string.subject_not_found, Toast.LENGTH_LONG).show()
            requireActivity().findNavController(R.id.nav_host_fragment).apply {
                if (currentDestination?.id == R.id.nav_subject_info) {
                    navigateUp()
                }
            }

            return null
        } else {
            subject = tempSubject
        }

        //views setup
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subject_info, container, false)
        binding.setLifecycleOwner { lifecycle }

        binding.subject = subject
        binding.subjectViewModel = subjectViewModel


        subjectViewModel.waitAsync(
            lifecycleScope,
            { subjectViewModel.getTeacher(subject.teacherId) }) {
            it?.let {
                teacher = it
                binding.teacher = it

                binding.teacherName.setOnClickListener {
                    requireActivity().findNavController(R.id.nav_host_fragment).apply {
                        if (currentDestination?.id == R.id.nav_subject_info) {
                            navigateUp()
                        }
                        navigate(
                            NavGraphUserDirections.actionTeacherInfo(teacher.id)
                        )
                    }
                }
            }
        }

        //used to filter modules when they are disabled
        userViewModel.runOrRefresh(lifecycle) {
            setupBasedOnUser(it)
        }

        return binding.root
    }

    private fun setupBasedOnUser(user: User) {

        user.runIfFeatureEnabled(User.ABSENCE_SHOW) {
            //shows absence for subject given
            absenceViewModel.runOrRefresh(
                absenceViewModel.thresholdHolder,
                lifecycle
            ) { threshold ->
                threshold?.let {
                    absenceViewModel.waitAsync(
                        lifecycleScope,
                        { absenceViewModel.getSubject(subject.name) },
                        { subject -> subject?.let { showAbsence(subject, threshold) } })
                }
            }
        }

        user.runIfFeatureEnabled(User.HOMEWORK_SHOW) {
            //loads homework list
            homeworkViewModel.waitAsync(
                lifecycleScope,
                { homeworkViewModel.getHomeworkListForSubject(subjectId) }) { showHomeworkList(it) }
        }

        user.runIfFeatureEnabled(User.MARKS_SHOW) {
            //loads marks
            marksViewModel.runOrRefresh(marksViewModel.getPair(subjectId), lifecycle) {
                it?.let { showMarks(it) }
            }
        }

        user.runIfFeatureEnabled(User.SUBJECTS_SHOW_THEMES) {
            themeViewModel = subjectViewModel.getThemeViewModelForSubject(subjectId)

            //RecyclerView adapter setup
            binding.themeLayout.list.adapter = ThemeAdapter()
            binding.themeLayout.viewModel = themeViewModel

            //loads themes
            themeViewModel.runOrRefresh(lifecycle) { showThemes() }
        }
    }

    private fun showAbsence(subject: AbsenceSubject, threshold: Double) {
        Log.i(TAG, "Showing absence")

        //replaces normally shown subject name with label Absence:
        val fakeSubject = subject.run {
            AbsenceSubject(
                getString(R.string.absence_subject_info_label),
                lessonCount, base, late, soon, school
            )
        }

        binding.absence.root.visibility = View.VISIBLE

        binding.absence.subject = fakeSubject
        binding.absence.threshold = threshold

    }

    /**shows homework list*/
    private fun showHomeworkList(list: HomeworkList) {
        Log.i(TAG, "Showing homework list")

        if (list.isNotEmpty()) {
            binding.homeworkBox.visibility = View.VISIBLE
            binding.homeworkList.adapter = HmwAdapter(requireActivity() as AppCompatActivity, list)
        }
    }

    /**shows marks*/
    private fun showMarks(pair: MarksPair) {
        Log.i(TAG, "Showing marks")

        if (pair.marks.isNotEmpty()) {
            val average = pair.subject.averageText
            val text = getString(R.string.marks_average) + ": " + average

            binding.marksBox.visibility = View.VISIBLE
            binding.marksAverage.text = text
            binding.list.adapter = MarksAdapter(pair.marks)
        }
    }

    /**shows themes*/
    private fun showThemes() {
        Log.i(TAG, "Showing themes")

        (binding.themeLayout.list.adapter as ThemeAdapter)
            .update(themeViewModel.data.value!!)
    }
}