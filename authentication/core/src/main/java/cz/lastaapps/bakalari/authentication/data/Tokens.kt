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

import android.os.Parcelable
import cz.lastaapps.bakalari.tools.getStringOrEmpty
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject
import java.time.Instant
import java.util.*

@Parcelize
data class Tokens(
    val uuid: UUID,

    val url: String,
    val refreshToken: String,
    val accessToken: String,
    val tokenExpiration: Instant,
    val tokenType: String,
    val userId: String,
    val apiVersion: String,
    val appVersion: String,
    val scope: String,
    val idToken: String,
) : Parcelable {
    @IgnoredOnParcel
    val scopeArray by lazy { scope.split(" ") }

    /**@return if refreshing access token is necessary*/
    fun isExpired(): Boolean {
        return Instant.now() > tokenExpiration.minusSeconds(5) //5 sec just to make sure
    }

    @IgnoredOnParcel
    val isOpenIdSupported: Boolean
        get() = scopeArray.contains("openid")
}

fun Long.toExpirationInstant(): Instant = Instant.now().plusSeconds(this)

fun JSONObject.toTokens(uid: UUID, url: String): Tokens {

    val accessToken = getString("access_token")
    val refreshToken = getString("refresh_token")
    val tokenExpiration = getLong("expires_in")
    val tokenType = getString("token_type")
    val userId = getString("bak:UserId")
    val apiVersion = getString("bak:ApiVersion")
    val appVersion = getString("bak:AppVersion")
    val scope = getString("scope")
    val idToken = getStringOrEmpty("id_token")

    return Tokens(
        uid, url, refreshToken, accessToken,
        tokenExpiration.toExpirationInstant(),
        tokenType, userId, apiVersion, appVersion,
        scope, idToken
    )
}

