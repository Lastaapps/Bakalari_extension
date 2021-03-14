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

package cz.lastaapps.bakalari.authentication.data

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import cz.lastaapps.bakalari.authentication.R
import cz.lastaapps.bakalari.authentication.data.BakalariAccount.Companion.validateUrl
import cz.lastaapps.bakalari.authentication.database.AccountsDatabase
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

@Entity(tableName = AccountsDatabase.BAKALARI_ACCOUNT)
@Parcelize
data class BakalariAccount(
    @PrimaryKey
    val uuid: UUID,

    val userName: String,
    val password: String?,
    val savePassword: Boolean,
    /**doesn't end with '/'*/
    val url: String,
    val townName: String?,
    val schoolName: String?,

    val refreshToken: String,
    val accessToken: String,
    val tokenExpiration: Instant,
    val tokenType: String,
    val userId: String,
    val apiVersion: String,
    val appVersion: String,
    val idToken: String,
    val scope: String,

    val confirmed: ZonedDateTime,
    val dateCreated: ZonedDateTime,

    val profileName: String,
    val imageUri: Uri?,
    val order: Int,
    val autoLaunch: Boolean,
) : Parcelable, Comparable<BakalariAccount> {

    init {
        if (url.endsWith("/"))
            throw IllegalArgumentException("The URL cannot end with '/'")
    }

    fun toAccount(context: Context): Account =
        Account(profileName, context.getString(R.string.authenticator_key))

    fun toLoginInfo(): LoginInfo = loginInfo

    @delegate:Ignore
    @IgnoredOnParcel
    private val loginInfo by lazy {
        LoginInfo(uuid, userName, password, savePassword, url, townName, schoolName)
    }

    fun toTokens(): Tokens = tokens

    @delegate:Ignore
    @IgnoredOnParcel
    private val tokens by lazy {
        Tokens(
            uuid, url, refreshToken, accessToken, tokenExpiration,
            tokenType, userId, apiVersion, appVersion, idToken, scope,
        )
    }

    fun toProfile(): Profile = profile

    @delegate:Ignore
    @IgnoredOnParcel
    private val profile by lazy {
        Profile(uuid, confirmed, dateCreated, profileName, imageUri, order, autoLaunch)
    }

    fun getAPIUrl(): String = url.getAPIUrl()

    @Ignore
    @IgnoredOnParcel
    val stringUUID = uuid.toString()

    companion object {
        fun of(info: LoginInfo, tokens: Tokens, profile: Profile): BakalariAccount =
            profile.run {
                tokens.run {
                    info.run {
                        BakalariAccount(
                            uuid, userName, password, savePassword, url, townName,
                            schoolName, refreshToken, accessToken, tokenExpiration, tokenType,
                            userId, apiVersion, appVersion, idToken, scope, confirmed,
                            dateCreated, profileName, imageUri, order, autoLaunch
                        )
                    }
                }
            }

        fun validateUrl(oldUrl: String): String {
            return if (oldUrl.endsWith("/"))
                oldUrl.substring(0, oldUrl.length - 1)
            else
                oldUrl
        }
    }

    override fun compareTo(other: BakalariAccount): Int {
        return -1 * order.compareTo(other.order)
    }
}


suspend fun Account.toBakalariAccount(context: Context): BakalariAccount? {
    return AccountsDatabase.getDatabase(context).repository.getByDisplayName(this.name)
}

/**@return url in format example www.example.com */
fun String.getRawUrl(): String {
    val replaced = this
        .replace("/next/login.aspx", "")
        .replace("/login.aspx", "")
    return validateUrl(replaced)
}

/**@return url in format www.example.com/api */
fun String.getAPIUrl(): String {
    return this.getRawUrl() + "/api"
}
