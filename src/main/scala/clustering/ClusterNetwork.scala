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

package clustering

import model.{Entity, Relationship}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsArray, JsNumber, JsObject, Json}
import util.JsonUtils.getJsonArray

import scala.collection.mutable


final case class ClusterNode(clusterId: Int, var zoom: Int)

class ClusterNetwork {
  
  val nodeMap: mutable.Map[Long, ClusterNode] = mutable.Map()
  val clusterLabels: mutable.Map[Int, List[Long]] = mutable.Map()
  val edges: mutable.Set[Long] = mutable.Set()
  val unfoldedClusters: mutable.Set[Int] =  mutable.Set()

  def convertToJson(entities: mutable.HashMap[Long, Entity], relationships: mutable.HashMap[Long, Relationship],
                     filter: Boolean = false): JsObject = {


    val(mappedNodes, mappedEdges) = if(filter) filterNetwork(relationships) else (nodeMap, edges)

    val nodes = nodesToJson(mappedNodes, entities)
    val links = edgesToJson(mappedEdges, relationships)
    val groups = labelsToJson()
    
    val unfold = JsArray(unfoldedClusters.map(id => JsNumber(id)).toSeq)

    Json.obj("nodes" -> getJsonArray(nodes), "links" -> getJsonArray(links), "groups" -> getJsonArray(groups), "unfold" -> unfold)
  }

  //TODO: Change ClusterExpander to only consider zoom = 0 then this is no longer needed
  def filterNetwork(relationships: mutable.HashMap[Long, Relationship]) = {

    val nodes = nodeMap.filter { case (_, node) => node.zoom == 0 }
    val rels = edges.filter { id =>

      val relationship = relationships(id)
      nodes.contains(relationship.e1) && nodes.contains(relationship.e2)
    }
    (nodes, rels)
  }

  private def nodesToJson(nodeMap: mutable.Map[Long, ClusterNode], entities: mutable.HashMap[Long, Entity]) = {

    nodeMap.map {
      case (id, node) =>

        val entity = entities(id)
        val image = Entity.getImageLink(id).getOrElse("")

        Json.obj("id" -> id, "name" -> entity.name, "type" -> entity.stringType,
                 "image" -> image ,"group" -> node.clusterId, "zoom" -> node.zoom)
    }.toList
  }

  private def edgesToJson(edges: mutable.Set[Long], relationships: mutable.HashMap[Long, Relationship]) = {

    edges.map { id =>
      val relationship = relationships(id)
      Json.obj("id" -> id, "source" -> relationship.e1, "target" -> relationship.e2)
    }.toList
  }

  private def labelsToJson() = {

    clusterLabels.map { case (groupId, nodes) => Json.obj(groupId.toString -> Json.toJson(nodes)) }.toList
  }
}