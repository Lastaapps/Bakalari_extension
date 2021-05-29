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

package cz.lastaapps.bakalari.api.repo.themes

import cz.lastaapps.bakalari.api.database.APIBase
import cz.lastaapps.bakalari.api.repo.core.Deletable
import cz.lastaapps.bakalari.api.repo.core.RepoProducer

private typealias Repo = ThemesMainRepository

val APIBase.themesRepository: Repo get() = Producer().getInstance(this)


private class Producer : RepoProducer<Repo>() {
    override fun createRepo(database: APIBase) = Repo(database)

    override fun isThisRepo(repo: Deletable): Boolean = repo is Repo
}