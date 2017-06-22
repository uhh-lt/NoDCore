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

package model

import java.time._

import database.DatabaseUsage
import scalikejdbc._
import util.IOUtils._

import scala.language.{implicitConversions, postfixOps}


case class Relationship(id: Long, e1: Long, e2: Long, frequency: Int = 0) {

  def toTsvWithDate(date: LocalDate) = toTsv(id, e1, e2, date)
}

object Relationship extends DatabaseUsage {

  val mapping = (rs: WrappedResultSet) => Relationship(rs.long("id"), rs.long("e1"), rs.long("e2"),
                                                       rs.int("frequency"))


  def getRelationshipId(e1: Long, e2: Long): Option[Long] = {

    DB readOnly { implicit session =>

      sql"""SELECT id, entity1, entity1 FROM relationships
            WHERE entity1 = ${e1} AND entity2 = ${e2} LIMIT 1;""".map({rs => rs.long("id")}).single.apply()
    }
  }

  def processAll(op: Relationship => Unit, date: LocalDate) = {

    DB readOnly { implicit session =>
      allLazy(date) foreach { relationship =>
        op(relationship)
      }
    }
  }

  private def allLazy(date: LocalDate)(implicit connection: DBSession) = {

      //TODO: Act as real stream
      sql"""SELECT id, entity1 AS e1, entity2 AS e2, dayFrequency as frequency, date
             FROM relationships
             WHERE date = ${date}""".map(mapping).toList.apply()
  }
}