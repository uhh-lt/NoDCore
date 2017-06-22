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

import java.nio.file.Paths

import pipeline.Settings._
import util.DBUtils._
import util.IOUtils._

import scala.collection.mutable
import scala.language.implicitConversions


class EntityStore {

  //TODO: Was ist, wenn angela als person und orga dann fail bzw. es wird das zweite geupdatet >.>
  private val nameToEntity = new mutable.HashMap[String, Entity]()
  private var nextId: Long = getMaxIdFromTable(defaultSettings.entitiesTable).getOrElse(0)

  val resourcesDir = Paths.get("resources/cleaning/")

  private lazy val perNameBlacklist = (resolveToLines("names-blacklist") ++ resolveToLines("names-first")).toSet
  private lazy val orgNameBlacklist = resolveToLines("names-newsagencies").toSet
  private lazy val nameWhitelist = resolveToLines("names-whitelist").toSet

  private val perFilter = (e: String) => !nameWhitelist.contains(e) && (e.length < 3 || perNameBlacklist.contains(e))
  private val orgFilter = (e: String) => !nameWhitelist.contains(e) && (e.length < 3 || orgNameBlacklist.contains(e))

  private def resolveToLines(res: String) = getInputLines(resourcesDir.resolve(res))

  private def getNextId(name: String): Long = {

    val idOpt = Entity.getEntityId(name)
    idOpt.getOrElse({ nextId += 1; nextId})
  }

  def getEntityByName(name: String) = nameToEntity(name.toLowerCase)

  def createPerFromString(pers: String) = registerEntities(pers, perFilter, createPer)
  def createOrgFromString(orgs: String) = registerEntities(orgs, orgFilter, createOrg)

  private def registerEntities(entities: String, predicate: String => Boolean, factory: String => Entity) = {

    distinctSplit(entities).map(_.replace("\"","")).filterNot(predicate).map { factory(_).name }.toList
  }

  private def createPer(name: String) = addEntity(name, Person(getNextId(name), name))
  private def createOrg(name: String) = addEntity(name, Organization(getNextId(name), name))

  private def addEntity(name: String, fallback: => Entity) = {

    val entity = nameToEntity.getOrElseUpdate(name.toLowerCase, fallback)

    entity.frequency += 1
    entity
  }

  def releaseResources() = nameToEntity.clear()
}
