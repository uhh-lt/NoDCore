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

import java.io.File
import com.typesafe.config.{ConfigFactory, Config}


trait Settings {

  val config: Config

  val language: String

  val entitiesTable = "entities"
  val relationshipsTable = "relationships"
  val sentencesTable = "sentences"
  val sourcesTable = "sources"
  val sent2sourceTable = "sentences_to_sources"
  val rel2sentTable = "relationships_to_sentences"

  val crfSentencesFilename: String
  val annotatedSentencesFilename: String
  val sent2entitiesFilename: String

  val entitiesSourceFilename: String
  val entitiesTargetFilename: String

  val relationshipsSourceFilename: String
  val relationshipsTargetFilename: String

  val rel2sentFilename: String
  val sentences2sourcesFilename: String
  val sentencesFilename: String
  val sourcesFilename: String

  val tables: List[String]
  val tablesToFiles: List[(String, String)]
}

class DefaultSettings(val config: Config) extends Settings {

  override val language = "de"

  //Entity extractor related
  override val crfSentencesFilename = "sentences.crf"
  override val annotatedSentencesFilename = "annotatedSentences.tsv"
  override val sent2entitiesFilename = "sentences2entities.tsv"

  //Aggregator related
  override val entitiesSourceFilename = "entities.tsv"
  override val entitiesTargetFilename = "entities.tsv.uniq"

  override val relationshipsSourceFilename = "relationships.tsv"
  override val relationshipsTargetFilename = "relationships.tsv.uniq"

  override val rel2sentFilename = "relationships_to_sentences.tsv"
  override val sentences2sourcesFilename = "sentences_to_sources.tsv"
  override val sentencesFilename = "sentences.tsv"
  override val sourcesFilename = "sources.tsv"

  //Database Importer related
  override val tables = List("entities", "relationships", "sentences", "sources",
    "sentences_to_sources", "relationships_to_sentences")

  override val tablesToFiles = List(
    entitiesTable -> entitiesTargetFilename,
    relationshipsTable -> relationshipsTargetFilename,
    sentencesTable -> sentencesFilename,
    sourcesTable -> sourcesFilename,
    sent2sourceTable -> sentences2sourcesFilename,
    rel2sentTable -> rel2sentFilename)
}

//TODO: improve access to Config.entitiesFilename?
//TODO: Tmp folder einziehen für entities.tvs usw
object Settings {

  lazy val defaultSettings = getDefaultSettings

  def getParameterAsString(name: String) = defaultSettings.config.getString(name)

  private def getDefaultSettings = {
    val conf = ConfigFactory.parseFile(new File("resources/application.conf"))
   //val conf = ConfigFactory.load()
    new DefaultSettings(conf)
  }
}
