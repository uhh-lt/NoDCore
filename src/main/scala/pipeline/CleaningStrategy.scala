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

import scalikejdbc.{DBSession, SQL}

abstract class CleaningStrategy {

  def clean()(implicit session: DBSession) = {
    val before = countsAndFrequencies

    removeUnusedSentences()
    removeUnusedEntities()
    mergeEntities()

    val after = countsAndFrequencies
    println(renderCountsAndFrequencies(before, after))
  }


  def removeUnusedSentences()(implicit session: DBSession)

  def removeUnusedEntities()(implicit session: DBSession)

  def mergeEntities()(implicit  session: DBSession) = {} //hook

  def countsAndFrequencies()(implicit session: DBSession) = {

    val (cEntities, cRelationships, cSent, cSent2Sources, cRel2Sent) = (
      SQL("SELECT COUNT(*) AS CO FROM entities_tmp").map(rs => rs.longOpt("CO")).single.apply.get,
      SQL("SELECT COUNT(*) AS CO FROM relationships_tmp").map(rs => rs.longOpt("CO")).single.apply.get,
      SQL("SELECT COUNT(*) AS CO FROM sentences_tmp").map(rs => rs.longOpt("CO")).single.apply.get,
      SQL("SELECT COUNT(*) AS CO FROM sentences_to_sources_tmp").map(rs => rs.longOpt("CO")).single.apply.get,
      SQL("SELECT COUNT(*) AS CO FROM relationships_to_sentences_tmp").map(rs => rs.longOpt("CO")).single.apply.get)

    val (fEntities, fRelationships) = (
      SQL("SELECT SUM(dayFrequency) AS SU FROM entities_tmp").map(rs => rs.longOpt("SU")).single.apply.get,
      SQL("SELECT SUM(dayFrequency) AS SU FROM relationships_tmp").map(rs => rs.longOpt("SU")).single.apply.get)

    Map(
      "entities" -> Map(
        "count" -> cEntities.get,
        "frequency" -> fEntities.get),
      "relationships" -> Map(
        "count" -> cRelationships.get,
        "frequency" -> fRelationships.get),
      "sentences" -> Map(
        "count" -> cSent.get),
      "sent2sources" -> Map(
        "count" -> cSent2Sources.get),
      "r2s" -> Map(
        "count" -> cRel2Sent.get))
  }

  def renderCountsAndFrequencies(before: Map[String, Map[String, Long]], after: Map[String, Map[String, Long]]) = {
    """
      |ENTITIES
      |count     %8s // before
      |          %8s // after
      |frequency %8s // before
      |          %8s // after
      |
      |RELATIONSHIPS
      |count     %8s // before
      |          %8s // after
      |frequency %8s // before
      |          %8s // after
      |
      |SENTENCES
      |count     %8s //before
      |          %8s //after
      |
      |SENT2SOURCES
      |count     %8s //before
      |          %8s //after
      |
      |R2S
      |count     %8s // before
      |          %8s // after
    """.stripMargin.format(
        before("entities")("count"),
        after("entities")("count"),
        before("entities")("frequency"),
        after("entities")("frequency"),
        before("relationships")("count"),
        after("relationships")("count"),
        before("relationships")("frequency"),
        after("relationships")("frequency"),
        before("sentences")("count"),
        after("sentences")("count"),
        before("sent2sources")("count"),
        after("sent2sources")("count"),
        before("r2s")("count"),
        after("r2s")("count"))
  }
}
