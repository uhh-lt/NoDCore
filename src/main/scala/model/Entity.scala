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

import java.time.LocalDate

import database.DatabaseUsage
import scalikejdbc._
import util.IOUtils._

import scala.language.{implicitConversions, postfixOps}


abstract class Entity(val entityType: EntityType.Value) {

  def id: Long
  def name: String
  var frequency: Long

  def isPerson = entityType == EntityType.Person
  def isOrganization = entityType == EntityType.Organization

  def binaryType = if (isPerson) 0 else 1
  def stringType = if (isPerson) "PERSON" else "ORGANIZATION"

  def toTsvWithDate(date: LocalDate) = toTsv(id, binaryType, name, date, 1)
}

object EntityType extends Enumeration {

  val Person = Value
  val Organization = Value
}

case class Person(id: Long, name: String, var frequency: Long = 0) extends Entity(EntityType.Person)
case class Organization(id: Long, name: String, var frequency: Long = 0) extends Entity(EntityType.Organization)


object Entity extends DatabaseUsage {

  val row2Entity = (rs: WrappedResultSet) => rs.int("type") match {

    case 0 => Person(rs.long("id"), rs.string("name"), rs.int("frequency"))
    case 1 => Organization(rs.long("id"), rs.string("name"), rs.int("frequency"))
  }

  def getEntityId(name: String): Option[Long] = {

    DB readOnly { implicit session =>

      sql"""SELECT id, name FROM entities
            WHERE name = ${name} LIMIT 1;""".map({rs => rs.long("id")}).single.apply()
    }
  }

  def getImageLink(id: Long): Option[String] = {

    DB readOnly { implicit session =>
      sql"""SELECT link FROM entities_to_links WHERE entity_id = ${id};
          """.map({ rs => rs.string("entities_to_links.link") }).single().apply()
    }
  }

  def processAll(op: Entity => Unit, date: LocalDate) {

    DB readOnly  { implicit session =>
      allLazy(date) foreach { entity =>
        op(entity)
      }
    }
  }

  private def allLazy(date: LocalDate)(implicit connection: DBSession) = {

    sql"""SELECT id, type, name, dayFrequency AS frequency, date
           FROM entities
           WHERE useEntity
           AND date = ${date}
          """.map(row2Entity).list.apply()
  }
}

