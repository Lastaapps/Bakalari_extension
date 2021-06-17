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

package cz.lastaapps.bakalari.app.ui.navigation;

import android.os.Parcelable;

import androidx.annotation.NonNull;


public class ComplexDeepLinkNavigatorParcelableAccessor {

    /*

    @NonNull
    public static Parcelable.Creator<> getBlahCreator() {
        return .CREATOR;
    }
    */

    @NonNull
    public static Parcelable.Creator<ComplexDeepLinkNavigator.ActionHolder> getActionHolderCreator() {
        return ComplexDeepLinkNavigator.ActionHolder.CREATOR;
    }
}