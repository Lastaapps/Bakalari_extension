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

package cz.lastaapps.bakalariextension.api.web

import android.util.Log
import cz.lastaapps.bakalariextension.api.ConnMgr

/**Loads web module data from the server*/
class WebModulesLoader {
    companion object {

        private val TAG = WebModulesLoader::class.java.simpleName

        suspend fun loadFromServer(): WebRoot? {

            Log.i(TAG, "Loading from server")

            val json = ConnMgr.serverGet("webmodule") ?: return null

            return WebModulesParser.parse(json)
        }
    }
}