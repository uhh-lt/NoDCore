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

import java.time.LocalDate

import clustering.ClusterExpander._
import clustering.MCLClustering._
import database.DatabaseUsage
import model.CompleteGraph._
import play.api.libs.json.JsObject
import scalikejdbc._
import util.DBUtils._
import util.GraphUtils._


class Clustering extends ProcessingUnit with DatabaseUsage {

    override val moduleName: String = "Clustering"
    val trendWordLimit = 10

  override def produce: (Cas) => Cas = (c: Cas) => {

      c.corpus.dates() foreach { date =>

        //TODO Introduce graph abstraction to reduce huge amount of parameters
        val graphRepr = graphForDate(date)
        val components = weakComponents(graphRepr.graph)

        val trendWordIds = retrieveTrendWords(date, trendWordLimit)
        val importantComponents = components.filter(c => c.exists(id => trendWordIds.contains(id)))

        //importantComponents ++= components.filter(_.size > 7)
        //importantComponents ++= components.toList.sortBy(_.size).reverse.take(9)
        val biggestComponent = components.maxBy(_.size)
        //GraphUtils.exportGraphToTsv(graphRepr.graph, graphRepr.entities, graphRepr.relationships, Paths.get("/home/toa/"))
        importantComponents += biggestComponent
        val subgraph = createSubgraph(graphRepr.graph, importantComponents.flatten.toSet)
        val clusters = cluster(subgraph, graphRepr.entities, graphRepr.relationships)

        val expandedNetwork = expand(clusters, trendWordIds.toSet, subgraph, graphRepr.entities, graphRepr.relationships)
        val networkAsJson = expandedNetwork.convertToJson(graphRepr.entities, graphRepr.relationships, filter = true)

        DB localTx { implicit session =>

          val clusterId = storeCluster(date, networkAsJson)
          storeTrendWords(clusterId, trendWordIds)
        }
      }

      c
    }

    def storeCluster(date: LocalDate, json: JsObject)(implicit session: DBSession): Long = {

      DB localTx { implicit session =>
        sql"""SET NAMES utf8;""".execute.apply()

        sql"""INSERT INTO clusters(date, json)
              VALUES(${date}, ${json.toString()});""".updateAndReturnGeneratedKey.apply()
      }
    }

    def storeTrendWords(clusterId: Long, trendWords: List[Long])(implicit session: DBSession) = {

      trendWords foreach { w =>

        sql"""INSERT INTO trendwords(cluster_id, entity_id)
              VALUES(${clusterId}, ${w});""".update.apply()
      }
    }
}
