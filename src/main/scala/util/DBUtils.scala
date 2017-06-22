/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *         - Uli Fahrer
 */

package util

import java.time.LocalDate

import database.DatabaseUsage
import scalikejdbc._


object DBUtils extends DatabaseUsage {

  def getMaxIdFromTable(tableName: String): Option[Long] = {

    DB readOnly  { implicit session =>

      SQL("SELECT MAX(id) AS max FROM %s;".format(tableName)).map({rs => rs.longOpt("max")}).single.apply().get
    }
  }

  def retrieveTrendWords(date: LocalDate, limit: Int): List[Long] = {
    DB readOnly { implicit session =>

      sql"""SELECT  id, type, COUNT(id) AS ct
            FROM
                (SELECT
                    e1.id, e1.type
                FROM
                    entities e1
                GROUP BY e1.id , e1.date) AS trending
            WHERE
                id IN (SELECT
                        e2.id
                    FROM
                        entities e2
                    WHERE
                        e2.date = ${date}
                        AND e2.useEntity
                        AND (LENGTH(e2.name) - LENGTH(REPLACE(e2.name, ' ', '')) + 1) > 1
                        AND e2.dayFrequency > (SELECT
                                AVG(e3.dayFrequency) + 10
                            FROM
                                entities e3
                            WHERE
                                date = ${date}))
            GROUP BY id
            ORDER BY ct desc, id desc
            LIMIT ${limit};""".map(rs => rs.long("id")).list.apply()
    }
  }
}
