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

package cz.lastaapps.bakalari.authentication.database

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import cz.lastaapps.bakalari.authentication.R
import cz.lastaapps.bakalari.authentication.data.BakalariAccount
import cz.lastaapps.bakalari.authentication.data.Tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.*

class AccountsRepository(private val database: AccountsDatabase, private val dao: AccountsDao) {

    companion object {
        private val TAG get() = AccountsRepository::class.simpleName
    }

    suspend fun addAccount(context: Context, account: BakalariAccount) {
        val mgr: AccountManager = AccountManager.get(context)

        shiftOrder()

        if (!account.savePassword && account.password != null) {
            dao.insert(account.copy(password = null))
        } else {
            dao.insert(account)
        }

        //todo login

        mgr.addAccountExplicitly(account.toAccount(context), null, null)
    }

    suspend fun removeAccount(context: Context, bakalariAccount: BakalariAccount) {
        val mgr: AccountManager = AccountManager.get(context)

        dao.delete(bakalariAccount)

        //todo logout

        mgr.removeAccount(bakalariAccount.toAccount(context), null, null)
    }

    suspend fun updateAccount(
        context: Context,
        oldBakalariAccount: BakalariAccount,
        newBakalariAccount: BakalariAccount
    ) {
        val mgr: AccountManager = AccountManager.get(context)

        dao.update(newBakalariAccount)

        mgr.removeAccount(oldBakalariAccount.toAccount(context), null, null)
        mgr.addAccountExplicitly(newBakalariAccount.toAccount(context), null, Bundle())
    }

    suspend fun updateToken(tokens: Tokens) {
        val account =
            getByUUID(tokens.uuid) ?: throw IllegalArgumentException("No row to update tokens in")
        dao.update(BakalariAccount.of(account.toLoginInfo(), tokens, account.toProfile()))
    }

    suspend fun updateAutoLaunch(account: BakalariAccount) {
        if (account.autoLaunch) {
            val accounts = dao.getAllAutoStart().map { it.copy(autoLaunch = false) }
            accounts.forEach {
                dao.update(it)
            }
        }
        dao.update(account)
    }

    suspend fun refreshSystemAccounts(context: Context) {
        val mgr: AccountManager = AccountManager.get(context)
        val type = context.getString(R.string.authenticator_key)

        val accountNames = mgr.getAccountsByType(type).map { it.name }
        val validNames = getAll().map { it.profileName }

        for (name in accountNames) {
            if (!validNames.contains(name)) {
                mgr.removeAccount(Account(name, type), null, null)
            }
        }

        for (name in validNames) {
            if (!accountNames.contains(name)) {
                mgr.addAccountExplicitly(Account(name, type), null, null)
            }
        }
    }

    suspend fun expireTokens() = dao.expireTokens()

    suspend fun canCreateAccount(
        url: String,
        userName: String,
        profileName: String
    ): CanCreateAccount {

        if (existsProfileName(profileName)) {
            return CanCreateAccount.PROFILE_NAME_EXISTS
        }

        if (existsAccount(userName, url)) {
            return CanCreateAccount.USERNAME_EXISTS
        }

        return CanCreateAccount.ALLOWED
    }

    private suspend fun shiftOrder() {
        val all = dao.getAll()
        val new = all.map { it.copy(order = it.order + 1) }

        dao.update(new)
    }

    suspend fun getByUUID(uuid: UUID): BakalariAccount? = dao.getByUUID(uuid)

    suspend fun getByDisplayName(displayName: String): BakalariAccount? =
        dao.getByDisplayName(displayName)

    suspend fun getAutoStart(): BakalariAccount? = dao.getAutoStart()

    suspend fun getAll(): List<BakalariAccount> = dao.getAll()

    fun getAllObservable(): Flow<List<BakalariAccount>> =
        dao.getAllObservable().distinctUntilChanged()

    suspend fun getAccountsNumber(): Int = dao.getAccountsNumber()

    suspend fun exitsUUID(uuid: UUID): Boolean = dao.exitsUUID(uuid)

    suspend fun existsProfileName(displayName: String): Boolean =
        !dao.existsProfileName(displayName)

    suspend fun existsAccount(userName: String, url: String): Boolean =
        !dao.existsAccount(userName, url)

    suspend fun newUUID(): UUID {
        while (true) {
            val uuid = UUID.randomUUID()
            if (!exitsUUID(uuid)) {
                Log.i(TAG, "New UUID generated $uuid")
                return uuid
            } else {
                Log.i(TAG, "Same UUID found, generating new one!")
            }
        }
    }
}

enum class CanCreateAccount {
    ALLOWED, PROFILE_NAME_EXISTS, USERNAME_EXISTS,
}
