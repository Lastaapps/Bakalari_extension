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

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalari.api.entity.core.SimpleData
import cz.lastaapps.bakalari.api.entity.subjects.SubjectList
import cz.lastaapps.bakalari.api.entity.subjects.Teacher
import cz.lastaapps.bakalari.api.repo.timetable.WebTimetableDate
import cz.lastaapps.bakalari.api.repo.timetable.WebTimetableType
import cz.lastaapps.bakalari.app.NavGraphUserDirections
import cz.lastaapps.bakalari.app.R
import cz.lastaapps.bakalari.app.databinding.FragmentTeacherInfoBinding
import cz.lastaapps.bakalari.app.ui.uitools.accountsViewModels
import cz.lastaapps.bakalari.app.ui.user.timetable.TimetableMainViewModel
import kotlinx.coroutines.*


/**shows info about teacher given*/
class TeacherInfoDialog : BottomSheetDialogFragment() {

    companion object {
        private val TAG get() = TeacherInfoDialog::class.java.simpleName
    }

    private lateinit var binding: FragmentTeacherInfoBinding
    private val viewModel: SubjectViewModel by accountsViewModels()
    private val args: TeacherInfoDialogArgs by navArgs()
    private val timetableViewModel: TimetableMainViewModel by accountsViewModels()

    private lateinit var teacherId: String
    private lateinit var simpleTeacher: SimpleData
    private var fullTeacher: Teacher? = null
    private lateinit var teacherSubjects: SubjectList

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "Creating view")

        //views setup
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_teacher_info, container, false)
        binding.setLifecycleOwner { lifecycle }

        if (!loadTeacher()) return null

        viewModel.waitAsync(lifecycleScope, { viewModel.getTeachersSubjects(simpleTeacher.id) }) {
            teacherSubjects = it
            showData()
            yield()

            //forces the dialog to fully expand with the new layout
            val bottomSheet: FrameLayout =
                dialog?.findViewById(com.google.android.material.R.id.design_bottom_sheet)
                    ?: return@waitAsync
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.skipCollapsed = true
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }

        binding.simpleTeacher = simpleTeacher

        fullTeacher?.let {
            binding.fullTeacher = it

            binding.addContact.setOnClickListener {
                addContact()
            }
        }

        binding.komens.setOnClickListener {
            //TODO link to komens
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val available = timetableViewModel.isWebTimetableAvailable()
            if (available)
                withContext(Dispatchers.Main) {
                    binding.openTimetable.visibility = View.VISIBLE
                    binding.openTimetable.setOnClickListener {
                        timetableViewModel.openWebTimetable(
                            requireContext(),
                            WebTimetableDate.ACTUAL,
                            WebTimetableType.TEACHER,
                            simpleTeacher.id
                        )
                    }
                }
        }

        return binding.root
    }

    /**Loads simple and tries also to load full teacher
     * @return if loading had succeed*/
    private fun loadTeacher(): Boolean {
        //gets teacher's id from input data
        teacherId = args.teacherId
        val tempTeacher = runBlocking(Dispatchers.IO) { viewModel.getTeacher(teacherId) }

        if (tempTeacher != null) {
            fullTeacher = tempTeacher
            simpleTeacher = tempTeacher.toSimpleData()

            return true

        } else {
            val tempSimple = runBlocking(Dispatchers.IO) { viewModel.getSimpleTeacher(teacherId) }

            return if (tempSimple != null) {
                simpleTeacher = tempSimple
                true

            } else {

                Toast.makeText(requireContext(), R.string.teacher_not_found, Toast.LENGTH_LONG)
                    .show()
                dismiss()
                false
            }
        }
    }

    private fun showData() {

        Log.i(TAG, "setting up with teacher data")

        //tries to get teacher object for id given

        //sets up the list showing teacher's subjects
        binding.subjectList.apply {
            setHasFixedSize(true)
            adapter = cz.lastaapps.bakalari.app.ui.user.subjects.SubjectAdapter({ it.name }).apply {
                update(teacherSubjects)

                //shows subject info
                onItemClicked = { subject ->
                    requireActivity().findNavController(R.id.nav_host_fragment).navigate(
                        NavGraphUserDirections.actionSubjectInfo(subject.id)
                    )
                    dismiss()
                }
            }
        }

    }

    /**adds this teachers info into contacts - opens settings activity*/
    private fun addContact() {
        val intent = Intent(Intent.ACTION_INSERT)
        val fullTeacher = fullTeacher!!

        intent.type = ContactsContract.Contacts.CONTENT_TYPE
        intent.putExtra(ContactsContract.Intents.Insert.NAME, fullTeacher.name.let {

            //changes the order of names from last name first name to first name last name
            val array = it.split(" ").reversed()

            val builder = StringBuilder(it.length)
            for (i in array.indices) {
                builder.append(array[i])
                if (i != array.size - 1) builder.append(" ")
            }

            builder.toString()
        })
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, fullTeacher.email)
        intent.putExtra(ContactsContract.Intents.Insert.NOTES, fullTeacher.web)
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, fullTeacher.phoneSchool)
        intent.putExtra(
            ContactsContract.Intents.Insert.PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        )
        intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, fullTeacher.phoneHome)
        intent.putExtra(
            ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        )
        intent.putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, fullTeacher.phoneMobile)
        intent.putExtra(
            ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        )

        startActivity(intent)
    }
}