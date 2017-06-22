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

import java.nio.file.{Path, Paths}
import util.Benchmark._
import java.util

import de.tu.darmstadt.lt.ner.preprocessing.TrainNERModel._
import model.Sentence
import pipeline.Settings._

import scala.collection.JavaConversions._
import scala.language.{implicitConversions, postfixOps}

//TODO: This limit parameter is only for entities. All sentences are written >.>
//TODO: Add AbstractTagger trait to simple replace the Tagger
class EntityExtractor(limit: Option[Int] = None) extends ProcessingUnit {

  override val moduleName = "Entity Extractor"
  
  //TODO: Better way? With config? defining here is not good.
  val crfModelDir = Paths.get("resources/model/")
  val addData = Paths.get("resources/ner/")

  def limitSentences(sents: Iterator[Sentence], limit: Option[Int]) = if (limit.isDefined) sents.take(limit.get) else sents
  implicit def toIntegerList(lst: List[Int]): util.List[Integer] = seqAsJavaList(lst.map(i => i:java.lang.Integer))


  override def produce: (Cas) => Cas = (c: Cas) => {

    val crfFile = c.outputDir.resolve(defaultSettings.crfSentencesFilename)
    val annotatedSentencesFile = c.outputDir.resolve(defaultSettings.annotatedSentencesFilename)
    val sent2entitiesFile = c.outputDir.resolve(defaultSettings.sent2entitiesFilename)

    createCrfFile(crfFile, limitSentences(c.corpus.sentences(), limit)).withBenchmark("Creating CRF sentence file")
    tagSentences(sent2entitiesFile, annotatedSentencesFile, crfFile, limitSentences(c.corpus.sentences(), limit)).withBenchmark("Tagging sentence file")

    c
  }

  def createCrfFile(output: Path, sentences: Iterator[Sentence]) = {

    val javaList: util.List[String] = sentences.map(_.sentence).toList
    sentenceToCRFFormat(javaList, output.toString, defaultSettings.language)
  }

  def tagSentences(sentences2entitiesFile: Path, entitiesFile: Path, crfFile: Path, sentences: Iterator[Sentence]) = {

    val sentIds = sentences.map(_.id.toInt).toList

    /*classifyTestFile(crfModelDir.toFile,
                     crfFile.toFile,
                     entitiesFile.toFile,
                     sentences2entitiesFile.toFile,
                     sentIds,
                     defaultSettings.language,
                     true,
                     addData.resolve("freebase.txt").toString,
                     true,
                     addData.resolve("suffix_list.txt").toString,
                     addData.resolve("unsupervised_pos.txt").toString)*/

    classifyTestFile(crfModelDir.toFile,
      crfFile.toFile,
      entitiesFile.toFile,
      sentences2entitiesFile.toFile,
      sentIds,
      defaultSettings.language,
      true,
      null,
      true,
      null,
      null)
  }
}
