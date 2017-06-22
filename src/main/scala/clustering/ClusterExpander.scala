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

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph
import model.{Entity, Relationship}
import util.GraphUtils._

import scala.collection.JavaConversions.{asScalaBuffer, mapAsScalaMap, _}
import scala.collection.{immutable, mutable}
import scala.language.{implicitConversions, postfixOps}


object Implicit {
  implicit def coll2Tuple(coll: List[Long]): (Long, Long) = {
    coll.toList match {
      case x :: y :: Nil => (x, y)
      case _ => throw new IllegalArgumentException
    }
  }
}

class ClusterExpander(graph: UndirectedSparseMultigraph[Long, Long], entities: mutable.HashMap[Long, Entity], relationships: mutable.HashMap[Long, Relationship], numNodes: Int, numLabels: Int) {

  private val network = new ClusterNetwork()

  private val namedClusters: mutable.Map[Int, Set[Long]] = mutable.Map[Int, Set[Long]]()
  private val node2Cluster: mutable.Map[Long, Int] = mutable.Map[Long, Int]()

  private val unsetNodeState = -1

  def expandGraph(clusters: Set[Set[Long]], selected: Set[Long]) = {

    initNetwork(clusters)

    val interClusterEdges: Set[Long] = selectAllInterClusterEdges()
    val selectedInterClusterEdges: Set[Long] = selectMaximalWeigthedInterClusterEdges(interClusterEdges)

    network.edges ++= selectedInterClusterEdges

    namedClusters foreach {

      case (id, nodes) =>
        val relevantNodes: List[Long] = selectRelevantNodes(nodes)
        network.clusterLabels(id) = relevantNodes.take(numLabels)

        val connectingNodes: Set[Long] = selectedInterClusterEdges.flatMap(e => graph.getIncidentVertices(e).toList).filter(nodes.contains)
        val trendyWordsForCluster = nodes & selected
        val subgraph: (Set[Long], Set[Long]) = buildClusterFromNodes(nodes, relevantNodes.toSet, connectingNodes ++ trendyWordsForCluster)

        network.edges ++= subgraph._2
        subgraph._1.foreach(n => network.nodeMap(n).zoom = 0)
    }
    
    unfoldCluster(selected)
    
    network
  }

  private def initNetwork(clusters: Set[Set[Long]]) = {

    namedClusters ++= clusters.filterNot(_.isEmpty).zipWithIndex.map(_.swap).toMap
    node2Cluster ++= namedClusters.flatMap { case (c, ns) => ns.map(n => (n, c)) }
    network.nodeMap ++= namedClusters.flatMap { case (c, ns) => ns.map(n => (n, ClusterNode(c, unsetNodeState))) }
  }

  private def selectAllInterClusterEdges(): Set[Long] = {

    val connectsClusters = (edge: Long) => {
      val (from, to) = Implicit.coll2Tuple(graph.getIncidentVertices(edge).toList)
      node2Cluster(from) != node2Cluster(to)
    }
    graph.getEdges.filter(connectsClusters).toSet
  }

  private def selectMaximalWeigthedInterClusterEdges(edges: Set[Long]): Set[Long] = {

    val clusters = namedClusters.keys.toSet
    val weightedEdges = weightEdges(edges)
    val clusterPairs: Set[(Int, Int)] = (for (x <- clusters; y <- clusters) yield (x, y)).filter(t => t._1 < t._2)
    val selectedEdges: mutable.Set[Long] = mutable.Set()

    clusterPairs.foreach {
      case (c1, c2) =>
        val relevantEdges = weightedEdges.filter {
          case (id, _) =>
            val nodes = graph.getIncidentVertices(id).toList
            nodes.forall(n => node2Cluster(n) == c1 || node2Cluster(n) == c2)
        }
        if (relevantEdges.nonEmpty) {
          selectedEdges += relevantEdges.maxBy { case (_, w) => w }._1
        }
    }
    selectedEdges.toSet
  }

  private def weightEdges(edges: Set[Long]): Set[(Long, Double)] = edges map { edge =>

    (edge, relationships(edge).frequency.toDouble)
  }

  private def selectRelevantNodes(nodes: Set[Long]) = {

    val subgraph = createSubgraph(graph, nodes)
    rankWithPagerank(subgraph).take(numNodes)
  }

  private def buildClusterFromNodes(clusterNodes: Set[Long], relevantNodes: Set[Long], connectingNodes: Set[Long]) = {

    //assert(findBiggestConnectedGraph(graph.createSubgraph(clusterNodes)) == clusterNodes.size, "Disconnected Clusters")
    val subgraph = createSubgraph(graph, relevantNodes)
    val connectedNodes = weakComponents(subgraph).maxBy(_.size)

    val algo = new DijkstraShortestPath(createSubgraph(graph, clusterNodes))

    val nodesOnPath = connectingNodes.flatMap { n =>
      val distMap = algo.getDistanceMap(n)
      val closestNode = distMap.filter(mapping => connectedNodes.contains(mapping._1)).minBy(_._2.asInstanceOf[Double])._1
      val path = algo.getPath(n, closestNode) flatMap { edge => graph.getIncidentVertices(edge).toList }
      path
    }

    val finalSubgraph = createSubgraph(graph, connectedNodes ++ nodesOnPath)

    (finalSubgraph.getVertices.toSet, finalSubgraph.getEdges.toSet)
  }
  
  private def unfoldCluster(trendyWordIds: Set[Long]) = {

    trendyWordIds.take(3).foreach(id => 
      if(network.nodeMap.contains(id)){
    	  network.unfoldedClusters += network.nodeMap(id).clusterId
      }
    )
  }
}

object ClusterExpander {

  private val defaultNumNodes = 10
  private val defaultNumLabels = 3

  def expand(clusters: immutable.Set[immutable.Set[Long]], selected: Set[Long], graph: UndirectedSparseMultigraph[Long, Long], entities: mutable.HashMap[Long, Entity], relationships: mutable.HashMap[Long, Relationship], numNodes: Int = defaultNumNodes,
             numLabels: Int = defaultNumLabels) = {

    new ClusterExpander(graph, entities, relationships, numNodes, numLabels).expandGraph(clusters, selected)
  }
}