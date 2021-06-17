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

package cz.lastaapps.bakalari.report

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import cz.lastaapps.bakalari.report.databinding.FragmentSendIdeaBinding
import cz.lastaapps.bakalari.tools.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sends idea to Firebase database
 * Limited to 1 per day
 */
class SendIdeaFragment : Fragment() {

    companion object {
        private val TAG get() = SendIdeaFragment::class.java.simpleName
        private const val SP_KEY = "SEND_IDEA"
        private const val SP_DATE_KEY = "LAST_SENT"
    }

    private lateinit var database: DatabaseReference
    private lateinit var binding: FragmentSendIdeaBinding

    private val args by navArgs<SendIdeaFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i(TAG, "Creating fragment, the uuid is ${args?.uuid}")

        if (timeCheck()) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_send_idea, container, false)

            database = FirebaseDatabase.getInstance().reference

            //sends data to Firebase
            binding.ideaFab.setOnClickListener {
                lifecycleScope.launch(Dispatchers.Default) {
                    send()

                    requireContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(
                            SP_DATE_KEY,
                            ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        )
                        .apply()
                }
            }

            return binding.root
        } else {
            Log.i(TAG, "Error check failed")

            //If limit per day was reached
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.idea_overload)
                .setPositiveButton(R.string.idea_go_back) { dialog, _ ->
                    run {
                        dialog.dismiss()
                        findNavController().navigateUp()
                    }
                }
                .setCancelable(false)
                .create()
                .show()
        }

        return null
    }

    /**
     * @return If message was sent today, or if user is moving through time in settings
     */
    private fun timeCheck(): Boolean {

        if (BuildConfig.DEBUG)
            return true

        val lastSent = requireContext().getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
            .getLong(SP_DATE_KEY, 0)

        val cal = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastSent), TimeTools.UTC)
        val now = TimeTools.now

        if (cal.isAfter(now))
            return false

        return cal.toLocalDate() != now.toLocalDate()
    }

    /**
     * Sends needed data to Firebase
     */
    private suspend fun send() {
        Log.i(TAG, "Sending data")

        val email = binding.email.text.trim().toString()
        val message = binding.message.text.trim().toString()

        if (message != "") {
            try {
                val id = TimeTools.format(TimeTools.now, TimeTools.COMPLETE_FORMAT) + "_" +
                        database.push().key.toString()

                //data to be sent
                val data = Message(
                    date = TimeTools.format(TimeTools.now, TimeTools.COMPLETE_FORMAT),
                    messageId = id,
                    email = email,
                    message = message,
                )

                val account = args?.uuid?.let {
                    AccountsDatabase.getDatabase(requireContext()).repository.getByUUID(it)
                }
                account?.let {
                    data.userHash =
                        (account.userId + account.townName + account.schoolName).hashCode()
                            .toString()
                }

                //posts data
                database.child("idea").child(id).setValue(data)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), R.string.idea_thanks, Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), R.string.idea_no_internet, Toast.LENGTH_LONG)
                        .show()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), R.string.idea_empty, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**Data structure of the data to be send*/
    @IgnoreExtraProperties
    data class Message(
        var date: String? = "",
        var messageId: String? = "",
        var email: String? = "",
        var message: String? = "",
        var userHash: String? = ""
    )
}
