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

package pipeline

import model.{Relationship, Entity}
import model.Entity.row2Entity
import scalikejdbc._
import util.DocumentSimilarity._

class AdvancedCleaner extends SimpleCleaner {

  override def mergeEntities()(implicit session: DBSession) = {
    println("Merging Entities...")

    retrieveSingleNamePersons().foreach { p =>

        val fullNameCandidates = retrievePersonsByPattern(p.name)
        val matched = fullNameCandidates.size match {
          //No matching entity to merge
          case 0 => None
          case 1 => Some(fullNameCandidates.head)
          case _ => Some(disambiguate(p, fullNameCandidates))
        }

        matched.foreach { e => mergeEntity(p, e) }
    }
  }

  private def disambiguate(singleNamePerson: Entity, fullNamePersons: List[Entity])(implicit session: DBSession): Entity = {

    val singleNameDocument = retrieveSentences(singleNamePerson.id).mkString(" ")

    fullNamePersons.map { e =>
      val fullNameDocument = retrieveSentences(e.id).mkString(" ")
      (e, similarity(documentFrequency(singleNameDocument), documentFrequency(fullNameDocument)))
    }.maxBy( { case(_, score) => score })._1
  }

  private def mergeEntity(singleEntity: Entity, fullEntity: Entity)(implicit session: DBSession) = {

    val singleNameRel = retrieveRelationships(singleEntity.id, fullEntity.id)
    if(singleNameRel.isDefined) {
      val fullNameRel = retrieveRelationships(fullEntity.id, singleEntity.id)

      val entityFreq = singleEntity.frequency + fullEntity.frequency
      val relationFreq = singleNameRel.get.frequency + fullNameRel.get.frequency

      //Update freq of fullname entity
      sql"""UPDATE entities_tmp SET dayFrequency = ${entityFreq} WHERE id = ${fullEntity.id};""".update.apply()
      //Update freq of fullname rel
      sql"""UPDATE relationships_tmp SET dayFrequency = ${relationFreq} WHERE id = ${fullNameRel.get.id};""".update.apply()
      //Update rel of entity 1
      sql"""UPDATE relationships_tmp SET entity1 = ${fullEntity.id} WHERE entity1 = ${singleEntity.id};""".update.apply()
      //Update rel of entity 2
      sql"""UPDATE relationships_tmp SET entity2 = ${fullEntity.id} WHERE entity2 = ${singleEntity.id};""".update.apply()
      //Delete entity with short name
      sql"""DELETE FROM entities_tmp WHERE id = ${singleEntity.id};""".update.apply()
      //Delete rels with short name
      sql"""DELETE FROM relationships_tmp WHERE id = ${singleNameRel.get.id};""".update.apply()

      println("Entity %s is merged with %s.".format(singleEntity.name, fullEntity.name))
    }
  }

  //TODO: Could think of merging this into the Entity and creating a model project.
  //That can be used from NoDCore, NoDWeb. Do NoDWeb need this?
  //Then do retrieveSIngleNameEntities with Option[Type]
  private def retrieveSingleNamePersons()(implicit session: DBSession): List[Entity] = {

    sql"""SELECT DISTINCT id, type, name, dayFrequency AS frequency FROM entities_tmp
          WHERE type = 0
          HAVING (LENGTH(name) - LENGTH(REPLACE(name,' ','')) + 1 = 1);
       """.map(row2Entity).list.apply()
  }

  private def retrievePersonsByPattern(name: String)(implicit session: DBSession): List[Entity] = {

    sql"""SELECT DISTINCT id, type, name, dayFrequency AS frequency FROM entities_tmp
          WHERE type = 0
          AND name != ${name}
          AND (name LIKE ${"% " + name} OR name LIKE ${name + " %"});
       """.map(row2Entity).list.apply()
  }

  //TODO: Improve query looks weird
  private def retrieveSentences(entityId: Long)(implicit session: DBSession): List[String] = {

    sql"""SELECT DISTINCT s.sentence AS sentence FROM sentences_tmp s, entities_tmp e, relationships_tmp r, relationships_to_sentences_tmp rs
          WHERE e.id = ${entityId}
          AND (e.id = r.entity1 || e.id = r.entity2)
          AND r.id = rs.relationship_id
          AND s.id = rs.sentence_id;""".map(_.string("sentence")).list.apply()
  }

  private def retrieveRelationships(first: Long, second: Long)(implicit session: DBSession): Option[Relationship] = {

    //Cannot reference a temporary table in the same query twice!
    sql"""CREATE TEMPORARY TABLE relationships_inner_tmp LIKE relationships_tmp;""".execute.apply()
    sql"""INSERT relationships_inner_tmp SELECT * FROM relationships_tmp;""".update().apply()

    val res = sql"""SELECT id, entity1 AS e1, entity2 AS e2, dayFrequency AS frequency
      	            FROM relationships_tmp r
      	            WHERE entity1 = ${first}
      	            AND entity2 IN (SELECT entity2 FROM relationships_inner_tmp r2 WHERE entity1 = ${second});
                """.map(Relationship.mapping).headOption().apply()

    sql"""DROP TEMPORARY TABLE relationships_inner_tmp;""".execute.apply()

    res
  }
}
