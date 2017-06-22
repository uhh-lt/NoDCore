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

import edu.uci.ics.jung.graph.UndirectedSparseMultigraph
import model.{Entity, Relationship}
import net.sf.javaml.clustering.mcl.{MarkovClustering, SparseMatrix}
import pipeline.Settings._
import util.Benchmark.toBenchmarkable
import util.GraphUtils._

import scala.collection.JavaConversions._
import scala.collection.{immutable, mutable}
import scala.language.{implicitConversions, postfixOps}


class MCLClustering(graph: UndirectedSparseMultigraph[Long, Long], entities: mutable.HashMap[Long, Entity], relationships: mutable.HashMap[Long, Relationship],
                    pGamma: Double, loopGain: Double) {

  //TODO: Why long, Int? Fix it
  private val renumbering: Map[Long, Int] = graph.getVertices.toSet.zipWithIndex.toMap
  
  private lazy val sparseMatrix = {

    val matrix = createSymmetricAdjacenyMatrix(graph.getVertexCount)
    new SparseMatrix(matrix)
  }

  private def createSymmetricAdjacenyMatrix(size: Int) = {

    val adjacencyMatrix = Array.fill[Double](size, size)(0)
    
    //Adding self loops improves clusters computation 
    //(small simple path loops can complicate computation in odd powers)
    (0 until size).foreach(index => adjacencyMatrix(index)(index) = 1)

    graph.getEdges.foreach { id =>

      val from = renumbering(relationships(id).e1.toInt)
      val to = renumbering(relationships(id).e2.toInt)
      //Symmetric matrix entries
      adjacencyMatrix(from.toInt)(to.toInt) = 1
      adjacencyMatrix(to.toInt)(from.toInt) = 1
    }
    adjacencyMatrix
  }


  def cluster() = {

    val maxResidual = 0.001d // default: 0.001d, this determines what is idempotent in double representation and should be left, probably
    val pruningParameter = 0.001d //computed values below this number will be interpreted as zero values, this parameter speeds up the computation

    val mcl = new MarkovClustering()
    sparseMatrix.normaliseRows()

    val resultMatrix = mcl.run(sparseMatrix, maxResidual, pGamma, loopGain, pruningParameter).withBenchmark("Start MCL Clustering")
    val clusters = getClusters(resultMatrix)

    fixClusters(clusters).toSet
  }

  private def getClusters(matrix: SparseMatrix) = {
    matrix.normaliseRows()

    val clusters = mutable.Set[mutable.Set[Long]]()
    val inverseNumbering = renumbering.map(_.swap)

    for (column <- 0 to matrix.size()) {
      val group = mutable.Set[Long]()

      for (row <- 0 to matrix.size()) {
        if (matrix.get(row, column) != 0) {
          group.add(inverseNumbering(row))
        }
      }

      if (group.nonEmpty) {
        clusters.add(group)
      }
    }
    clusters
  }

  def fixClusters(clusters: mutable.Set[mutable.Set[Long]]) = {

    val fixedClusters: mutable.Set[immutable.Set[Long]] = mutable.Set[immutable.Set[Long]]()
   
    clusters.foreach { c =>

      val subgraph = createSubgraph(graph, c.toSet)
      val components = weakComponents(subgraph)
      components.foreach(fixedClusters += _)
    }

    fixedClusters
  }
  
}

object MCLClustering {

  private val defaultPGamma = defaultSettings.config.getDouble("nod.component.cluster.pgamma")
  private val defaultLoopGain = 0.0d

  def cluster(graph: UndirectedSparseMultigraph[Long, Long], entities: mutable.HashMap[Long, Entity], relationships: mutable.HashMap[Long, Relationship],
              pGamma: Double = defaultPGamma, loopGain: Double = defaultLoopGain) = {

    new MCLClustering(graph, entities, relationships, pGamma, loopGain).cluster()
  }
}