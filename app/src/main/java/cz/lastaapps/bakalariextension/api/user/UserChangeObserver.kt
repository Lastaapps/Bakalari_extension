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

package cz.lastaapps.bakalariextension.api.user

import android.content.Intent
import cz.lastaapps.bakalariextension.App
import cz.lastaapps.bakalariextension.MainActivity
import org.json.JSONObject

/** Notifies main activity and the others that user object has changed on server and the data should be reloaded*/
class UserChangeObserver {

    companion object {
        fun onNew(new: JSONObject) {
            val old = UserStorage.load() ?: return

            if (new.toString() != old.toString()) {
                val intent = Intent(MainActivity.USER_CHANGED)
                App.context.sendBroadcast(intent)
            }
        }
    }
}