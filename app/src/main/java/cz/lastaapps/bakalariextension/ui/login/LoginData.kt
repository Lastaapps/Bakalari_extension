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

package cz.lastaapps.bakalariextension.ui.login

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import cz.lastaapps.bakalariextension.App

/**
 * Stores data needed to login
 */
class LoginData {

    companion object {

        //saving to shared preferences
        private const val SP_KEY = "LOGIN"
        private const val URL = "URL"
        private const val USERNAME = "USERNAME"
        private const val TOWN = "TOWN"
        private const val SCHOOL = "SCHOOL"
        private const val ACCESS_TOKEN = "ACCESS_TOKEN"
        private const val REFRESH_TOKEN = "REFRESH_TOKEN"
        private const val TOKEN_EXPIRATION = "TOKEN_EXPIRATION"
        private const val TOKEN_TYPE = "TOKEN_TYPE"
        private const val API_VERSION = "API_VERSION"
        private const val APP_VERSION = "APP_VERSION"
        private const val USER_ID = "USER_ID"

        private fun get(key: String): String {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .getString(key, "").toString()
        }

        private fun set(key: String, value: String) {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
                .edit().putString(key, value).apply()
        }

        /**
         * Saves all at once
         */
        fun saveData(
            username: String,
            url: String,
            town: String,
            school: String
        ) {
            App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE).edit {
                putString(USERNAME, username)
                putString(URL, url)
                putString(TOWN, town)
                putString(SCHOOL, school)
                apply()
            }
        }

        /**@return if user is logged in*/
        fun isLoggedIn(): Boolean {
            return accessToken != ""
        }

        var username: String
            set(value) {
                set(USERNAME, value)
            }
            get(): String {
                return get(USERNAME)
            }

        var url: String
            set(value) {
                set(URL, value)
            }
            get(): String {
                return get(URL)
            }

        var town: String
            set(value) {
                set(TOWN, value)
            }
            get(): String {
                return get(TOWN)
            }

        var school: String
            set(value) {
                set(SCHOOL, value)
            }
            get(): String {
                return get(SCHOOL)
            }

        var accessToken: String
            set(value) {
                set(ACCESS_TOKEN, value)
            }
            get(): String {
                return get(ACCESS_TOKEN)
            }

        var refreshToken: String
            set(value) {
                set(REFRESH_TOKEN, value)
            }
            get(): String {
                return get(REFRESH_TOKEN)
            }

        var tokenExpiration: Long
            /**@param value expiration time in seconds since now*/
            set(value) {
                getSP().edit().putLong(TOKEN_EXPIRATION,
                    System.currentTimeMillis() + value * 1000).apply()
            }
            /**@returns expiration time in ms*/
            get(): Long {
                return getSP().getLong(TOKEN_EXPIRATION, 0)
            }

        var tokenType: String
            set(value) {
                set(TOKEN_TYPE, value)
            }
            get(): String {
                return get(TOKEN_TYPE)
            }

        var apiVersion: String
            set(value) {
                set(API_VERSION, value)
            }
            get(): String {
                return get(API_VERSION)
            }

        var appVersion: String
            set(value) {
                set(APP_VERSION, value)
            }
            get(): String {
                return get(APP_VERSION)
            }

        var userID: String
            set(value) {
                set(USER_ID, value)
            }
            get(): String {
                return get(USER_ID)
            }

        private fun getSP(): SharedPreferences {
            return App.context.getSharedPreferences(SP_KEY, Context.MODE_PRIVATE)
        }
    }
}