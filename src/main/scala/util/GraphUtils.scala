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

import java.nio.file.Path

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer
import edu.uci.ics.jung.algorithms.filters.FilterUtils._
import edu.uci.ics.jung.algorithms.scoring.PageRank
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph
import model.{Entity, Relationship}
import util.IOUtils._

import scala.collection.JavaConversions._
import scala.collection.mutable


object GraphUtils {

  def createSubgraph(graph: UndirectedSparseMultigraph[Long, Long], nodes: Set[Long]): UndirectedSparseMultigraph[Long, Long] = {

    createInducedSubgraph[Long, Long, UndirectedSparseMultigraph[Long, Long]](nodes, graph)
  }

  def weakComponents(graph: UndirectedSparseMultigraph[Long, Long]) = {

    val finder = new WeakComponentClusterer[Long, Long]()
    finder.transform(graph).map(_.toSet)
  }

  def rankWithPagerank(graph: UndirectedSparseMultigraph[Long, Long]) = {

    val ranker: PageRank[Long, Long] = new PageRank(graph, 0.0)
    ranker.evaluate()
    val ranked = (graph.getVertices map { id => (id, ranker.getVertexScore(id).doubleValue()) }).toMap

    val ordering = Ordering.by { value: (Long, Double) => value._2 }.reverse
    val orderedNodes = ranked.toSeq.sorted(ordering)

    orderedNodes.map { case (id, _) => id }.toList
  }

  def applyPagerank(graph: UndirectedSparseMultigraph[Long, Long]): Map[Long, Double] = {

    val ranker: PageRank[Long, Long] = new PageRank(graph, 0.0)
    ranker.evaluate()
    val ranked = (graph.getVertices map { id => (id, ranker.getVertexScore(id).doubleValue()) }).toMap

    /*val ordering = Ordering.by { value: (Long, Double) => value._2 }.reverse

    val orderedNodes = ranked.toSeq.sorted(ordering)

    orderedNodes.map { case (id, _) => id }.toList*/

    ranked
  }

  def exportGraphToTsv(graph: UndirectedSparseMultigraph[Long, Long], entities: mutable.HashMap[Long, Entity],
                       relationships: mutable.HashMap[Long, Relationship], outputDir: Path) = {

    val nodes = outputDir.resolve("nodes.tsv")
    val relation = outputDir.resolve("relation.tsv")

    withOutput(nodes) { p =>
      p.write(toTsv("Id", "Label", "Frequency"))

      graph.getVertices.toList.foreach { id =>
        val node = entities(id)
        p.write(toTsv(node.id, node.name, node.frequency))
      }
    }

    withOutput(relation) { p =>
      p.write(toTsv("Source", "Target", "Frequency"))

      graph.getEdges.toList.foreach { id =>
        val rel = relationships(id)
        p.write(toTsv(rel.e1, rel.e2, rel.frequency))
      }
    }
  }
}
