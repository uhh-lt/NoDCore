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

import pipeline.Settings._
import util.DBUtils._

import scala.collection.mutable

class RelationshipStore {

  private val relationships = new mutable.HashMap[(Long, Long), Relationship]
  private var nextId: Long = getMaxIdFromTable(defaultSettings.relationshipsTable).getOrElse(0)

  //Assumed to be in the right order!
  private def getNextId(first: Long, second: Long): Long = {

    val idOpt = Relationship.getRelationshipId(first, second)
    idOpt.getOrElse({ nextId += 1; nextId})
  }

  def getOrCreateRelationship(entity1: Entity, entity2: Entity): Relationship = {

    val (first, second) = if (entity1.id < entity2.id) (entity1, entity2) else (entity2, entity1)
    relationships.getOrElseUpdate((first.id, second.id), Relationship(getNextId(first.id, second.id), first.id, second.id))
  }

  def releaseResources() = relationships.clear()
}