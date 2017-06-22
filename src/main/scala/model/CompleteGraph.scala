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

import edu.uci.ics.jung.graph.UndirectedSparseMultigraph

import scala.collection.mutable
import scala.language.{implicitConversions, postfixOps}

class CompleteGraph(date: LocalDate) {

  /**
   * Object representation of entities.
   *
   * @see Entity
   */
  val entities = mutable.HashMap[Long, Entity]()

  /**
   * Object representation of relationships.
   *
   * @see Relationship
   */
  val relationships = mutable.HashMap[Long, Relationship]()


  lazy val graph = {

    println("Loading graph...")
    val graph = new UndirectedSparseMultigraph[Long, Long]()

    Entity.processAll(processEntity(_: Entity, graph), date)
    Relationship.processAll(processRelationship(_: Relationship, graph), date)

    println("Graph loaded: %d nodes, %d edges".format(graph.getVertexCount, graph.getEdgeCount))
    graph
  }

  private def processEntity(e: Entity, graph: UndirectedSparseMultigraph[Long, Long]) = {
    entities.getOrElseUpdate(e.id, e)
    graph.addVertex(e.id)
  }

  private def processRelationship(r: Relationship, graph: UndirectedSparseMultigraph[Long, Long]) = {
    if (entities.contains(r.e1) && entities.contains(r.e2)) {
      relationships.getOrElseUpdate(r.id, r)
      graph.addEdge(r.id, r.e1, r.e2)
    }
  }
}

object CompleteGraph {

  def graphForDate(date: LocalDate) = new CompleteGraph(date)
}
