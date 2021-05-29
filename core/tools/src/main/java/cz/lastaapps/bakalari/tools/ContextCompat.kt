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

package cz.lastaapps.bakalari.tools

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build


/**
 * @return the current apps version code
 * */
fun Context.getVersionCode(): Long {
    val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
    @Suppress("DEPRECATION")
    return if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode else pInfo.versionCode.toLong()
}

/**
 * @return the current apps name
 * */
fun Context.getVersionName(): String {
    val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
    return pInfo.versionName
}