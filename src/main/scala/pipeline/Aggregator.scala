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

import java.nio.file.Path

import corpus.Corpus
import model.EntityStore
import pipeline.Settings.defaultSettings
import util.IOUtils._
import util.Scripts._

import scala.collection.mutable
import scala.language.postfixOps
import scala.sys.process.stringSeqToProcess


class Aggregator extends ProcessingUnit {

  override val moduleName: String = "Aggregator"

  override def produce: (Cas) => Cas = (c: Cas) => {

    saveSources(c.corpus, c.outputDir)

    val sent2entities = getSentences2entities(c.outputDir, c.entityStore)
    extractCoocurrences(c, sent2entities)

    aggregateEntitiesAndRelationships(c.outputDir)

    //Release Resources from cas
    c.releaseResources()

    c
  }

  def saveSources(corpus: Corpus, outputDir: Path) = {

    val sources = outputDir.resolve(defaultSettings.sourcesFilename)
    val sentences2sources = outputDir.resolve(defaultSettings.sentences2sourcesFilename)

    withOutput(sources) { out =>
      println("Copying sources...")
      for (source <- corpus.sources()) out.write(source.toTsv)
    }

    withOutput(sentences2sources) { out =>
      println("Copying sentences-to-sources...")
      for (s2s <- corpus.sentences2sources()) out.write(toTsv(s2s: _*))
    }
  }


  def getSentences2entities(outputDir: Path, store: EntityStore) = {

    val sent2entitiesFile = outputDir.resolve(defaultSettings.sent2entitiesFilename)
    val result = mutable.HashMap[Long, List[String]]().withDefaultValue(List())

    for(line <- getInputLines(sent2entitiesFile)) {

      line.split(tab) match  {
        case Array(sentId, "O", _, "O") =>
        case Array(sentId: String, perString, _, "O") =>
          result(sentId.toInt) = store.createPerFromString(perString)
        case Array(sentId: String, "O", _, orgString) =>
          result(sentId.toInt) = store.createOrgFromString(orgString)
        case Array(sentId: String, perString, _, orgString) =>
          result(sentId.toInt) = store.createOrgFromString(orgString) ++ store.createPerFromString(perString)

        case _ => throw new IllegalArgumentException("Sentences2Entities file do have the wrong format.")
      }
    }

    result
  }

  def extractCoocurrences(cas: Cas, sent2entities: mutable.Map[Long, List[String]]) = {

    val entityFile = cas.outputDir.resolve(defaultSettings.entitiesSourceFilename)
    val relationshipFile = cas.outputDir.resolve(defaultSettings.relationshipsSourceFilename)
    val sentences = cas.outputDir.resolve(defaultSettings.sentencesFilename)
    val rel2sentFile = cas.outputDir.resolve(defaultSettings.rel2sentFilename)

    withOutputs(entityFile, relationshipFile, sentences, rel2sentFile) {
      case Seq(entOut, relOut, sentOut, rel2sentOut) =>

        cas.corpus.sentences() foreach { sent =>

          val entities = sent2entities(sent.id) map { cas.entityStore.getEntityByName }
          entities foreach { e => entOut.write(e.toTsvWithDate(sent.date)) }

          sentOut.write(sent.toTsv)

          entities.combinations(2) foreach {
            case Seq(entity1, entity2) =>

              if(entity1 != entity2) {

                val rel = cas.relationshipStore.getOrCreateRelationship(entity1, entity2)
                relOut.write(rel.toTsvWithDate(sent.date))

                rel2sentOut.write(toTsv(rel.id, sent.id))
              }
          }
        }
    }
  }

  def aggregateEntitiesAndRelationships(outputDir: Path) = {

    val entitiesSourceFile = outputDir.resolve(defaultSettings.entitiesSourceFilename)
    val entitiesTargetFile = outputDir.resolve(defaultSettings.entitiesTargetFilename)

    val relationshipsSourceFile = outputDir.resolve(defaultSettings.relationshipsSourceFilename)
    val relationshipsTargetFile = outputDir.resolve(defaultSettings.relationshipsTargetFilename)

    aggregate(entitiesSourceFile, entitiesTargetFile)
    aggregate(relationshipsSourceFile, relationshipsTargetFile)
  }

  def aggregate(source: Path, target: Path) = {

    println("Aggregate entities of '%s'.".format(source))
    val command = countUnique(source, target)
    command!
  }
}
