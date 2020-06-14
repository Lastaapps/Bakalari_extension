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

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.lastaapps.bakalariextension.R
import cz.lastaapps.bakalariextension.api.subjects.SubjectList
import cz.lastaapps.bakalariextension.api.subjects.data.Teacher
import cz.lastaapps.bakalariextension.databinding.FragmentTeacherInfoBinding

/**shows info about teacher given*/
class TeacherInfoFragment : BottomSheetDialogFragment() {

    companion object {
        private val TAG = TeacherInfoFragment::class.java.simpleName

        private const val TEACHER_EXTRA = "TEACHER_EXTRA"

        /**shows fragment on the bottom of the screen*/
        fun show(manager: FragmentManager, teacherId: String) {
            TeacherInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(TEACHER_EXTRA, teacherId)
                }
            }.show(manager, TAG)
        }
    }

    private lateinit var binding: FragmentTeacherInfoBinding
    private val viewModel: SubjectViewModel by activityViewModels()

    private lateinit var teacherId: String
    private lateinit var teacher: Teacher
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

        //gets teacher's id from input data
        teacherId = requireArguments().getString(TEACHER_EXTRA) ?: ""

        viewModel.subjects.apply {
            observe({ lifecycle }) { showData() }
            if (value != null) {
                showData()
            } else {
                viewModel.onRefresh()
            }
        }

        return binding.root
    }

    private fun showData() {

        Log.i(TAG, "setting up with teacher data")

        //tries to get teacher object for id given
        val subjects = viewModel.subjects.value!!

        val teacher = Teacher.subjectsToTeachers(subjects).getById(teacherId)
        if (teacher == null) {
            Toast.makeText(requireContext(), R.string.teacher_not_found, Toast.LENGTH_LONG).show()
            dismiss()
            return
        }

        //gets teacher's subjects
        teacherSubjects = Teacher.getTeachersSubjects(subjects, teacher)

        this.teacher = teacher
        binding.teacher = teacher

        //sets up the list showing teacher's subjects
        binding.subjectList.apply {
            setHasFixedSize(true)
            adapter = SubjectAdapter({ it.name }).apply {
                update(teacherSubjects)

                //shows subject info
                onItemClicked = { subject ->
                    SubjectInfoFragment.navigateTo(requireActivity(), subject.id)

                    dismiss()
                }
            }
        }

        binding.addContact.setOnClickListener {
            addContact()
        }

        binding.komens.setOnClickListener {
            //TODO link to komens
        }

    }

    /**adds this teachers info into contacts - opens settings activity*/
    private fun addContact() {
        val intent = Intent(Intent.ACTION_INSERT)

        intent.type = ContactsContract.Contacts.CONTENT_TYPE
        intent.putExtra(ContactsContract.Intents.Insert.NAME, teacher.name.let {

            //changes the order of names from last name first name to first name last name
            val array = it.split(" ").reversed()

            val builder = StringBuilder(it.length)
            for (i in array.indices) {
                builder.append(array[i])
                if (i != array.size - 1) builder.append(" ")
            }

            builder.toString()
        })
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, teacher.email)
        intent.putExtra(ContactsContract.Intents.Insert.NOTES, teacher.web)
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, teacher.phoneSchool)
        intent.putExtra(
            ContactsContract.Intents.Insert.PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK
        )
        intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, teacher.phoneHome)
        intent.putExtra(
            ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME
        )
        intent.putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, teacher.phoneMobile)
        intent.putExtra(
            ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        )

        startActivity(intent)
    }
}