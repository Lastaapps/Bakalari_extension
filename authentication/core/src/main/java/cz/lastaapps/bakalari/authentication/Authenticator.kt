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

package cz.lastaapps.bakalari.authentication

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.data.toBakalariAccount
import kotlinx.coroutines.runBlocking
import java.util.*


class Authenticator(private val context: Context) : AbstractAccountAuthenticator(context) {

    private val suspendImpl = TokensAPI(context)

    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        tokenType: String?,
        options: Bundle?
    ): Bundle {

        val bakalariAccount = runBlocking {
            val bakalariAccount = account.toBakalariAccount(context) ?: return@runBlocking null

            val newTokensPair = suspendImpl.getRefreshedToken(bakalariAccount.uuid)
            when (newTokensPair.first) {
                TokensAPI.SUCCESS -> {
                    return@runBlocking BakalariAccount.of(
                        bakalariAccount.toLoginInfo(),
                        newTokensPair.second!!,
                        bakalariAccount.toProfile()
                    )
                }
                TokensAPI.OLD_TOKENS -> {
                    return@runBlocking bakalariAccount
                }
                else -> {
                    return@runBlocking null
                }
            }

        } ?: return Bundle().apply {
            //TODO something went wrong
        }

        return Bundle().apply {
            putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
            putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
            putString(AccountManager.KEY_AUTHTOKEN, bakalariAccount.accessToken)
        }
    }

    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        tokenType: String?,
        authenticatorSpecific: Array<String>?,
        options: Bundle?
    ): Bundle {

        val intent = LoginModuleConfig.addAccountIntent(context)

        return Bundle().apply {
            putParcelable(AccountManager.KEY_INTENT, intent)
        }
    }

    override fun editProperties(
        response: AccountAuthenticatorResponse,
        accountType: String
    ): Bundle {

        val intent = LoginModuleConfig.editPropertiesIntent(context)

        return Bundle().apply {
            putParcelable(AccountManager.KEY_INTENT, intent)

            response.onResult(this)
        }
    }

    override fun updateCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        tokenType: String,
        bundle: Bundle
    ): Bundle = throw UnsupportedOperationException("Not implemented")

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse,
        account: Account,
        bundle: Bundle
    ): Bundle? = throw UnsupportedOperationException("Not implemented")

    override fun getAuthTokenLabel(tokenType: String): String =
        throw UnsupportedOperationException("Not implemented")

    override fun hasFeatures(
        response: AccountAuthenticatorResponse,
        account: Account,
        strings: Array<String>
    ): Bundle = throw UnsupportedOperationException("Not implemented")
}