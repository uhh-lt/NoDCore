/*
 * Copyright 2015 Technische Universitaet Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *       - Uli Fahrer
 *       - Artjom Kochtchi
 */

package corpus

import java.nio.file.{Files, Path}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import model.{EntityStore, RelationshipStore, Sentence, Source}
import pipeline.Settings._
import pipeline.{Cas, CollectionReader}
import util.DBUtils._
import util.{IOUtils, Scripts}

import scala.collection.JavaConversions._
import scala.language.{implicitConversions, postfixOps}
import scala.sys.process.stringSeqToProcess

private case class LeipzigTextCorpus(sentences: Path, sources: Path, sentences2sources: Path, language: String,
                                     textType: String, date: LocalDate) {

  def sentencesFixed = sentences.getParent.resolve(sentences.getFileName + ".fixed")

  override def toString = "Corpus: %s %s (%s)".format(language, textType, date.toString)
}


class LeipzigText(path: Path, corpora: List[LeipzigTextCorpus]) extends Corpus {

  override def dates(): List[LocalDate] = corpora map { _.date }

  override def sentences(): Iterator[Sentence] = {

    val offset = offsetForCorpus(defaultSettings.sentencesTable)

    for(corpus <- corpora.iterator; line <- IOUtils.getInputLines(corpus.sentencesFixed)) yield {
      val sentence = Sentence.fromTsv(line)

      sentence.copyWith(id = sentence.id + offset)
    }
  }


  override def sources(): Iterator[Source] = {

    val offset = offsetForCorpus(defaultSettings.sourcesTable)

    for (corpus <- corpora.iterator; line <- IOUtils.getInputLines(corpus.sources)) yield {
      val source = Source.fromTsv(line)

      source.copyWith(id = source.id + offset)
    }
  }


  override def sentences2sources(): Iterator[List[Long]] = {

    val sentOffset = offsetForCorpus(defaultSettings.sentencesTable)
    val sourceOffset = offsetForCorpus(defaultSettings.sourcesTable)

    for (corpus <- corpora.iterator; line <- IOUtils.getInputLines(corpus.sentences2sources)) yield {
      val Array(sentence, source) = line.split(IOUtils.tab)

      List(sentence.toLong + sentOffset, source.toLong + sourceOffset)
    }
  }

  private def offsetForCorpus(tableName: String): Long = {

    val idOpt = getMaxIdFromTable(tableName)
    idOpt.getOrElse(0)
  }
}


class LeipzigTextReader(val inputDir: Path) extends CollectionReader {

  override val moduleName: String = "LeipzigTextReader"

  val sentenceFilenamePattern = """^(.+)_(.+)_(\d+)_(\d+)(\D?)-sentences\.txt$""".r
  val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  val sentenceAffix = "sentences"
  val sourcesAffix = "sources"
  val sentencesToSourcesAffix = "inv_so"

  implicit def path2Filename(path: Path): String = path.getFileName.toString

  override def produce: Path => Cas = (outputDir: Path) => {

    val corpus = loadLeipzigCorpora(inputDir)
    Cas(corpus, outputDir, new EntityStore, new RelationshipStore)
  }

  /**
   * Loads all existing Leipzig corpora files under the given path.
   *
   * @param path path to the corpora files
   */
  def loadLeipzigCorpora(path: Path) = {

    val corpora = findCorpora(path)
    corpora foreach fixEncoding

    new LeipzigText(path, corpora)
  }


  private def findCorpora(path: Path): List[LeipzigTextCorpus] = {

    Files.newDirectoryStream(path) flatMap { file =>

      if (sentenceFilenamePattern.pattern.matcher(file).matches()) {

        val sources = file.getParent.resolve(file.replace(sentenceAffix, sourcesAffix))
        val sentences2sources = file.getParent.resolve(file.replace(sentenceAffix, sentencesToSourcesAffix))

        if (Files.exists(sources) && Files.exists(sentences2sources)) {
          val corpus = createLeipzigTextCorpus(file, sources, sentences2sources)
          Some(corpus)
        } else None
      } else None
    } toList
  }


  private def createLeipzigTextCorpus(file: Path, sources: Path, sentences2sources: Path) = {

    val sentenceFilenamePattern(language, textType, date, _, _) = path2Filename(file)
    val localDate = LocalDate.parse(date, dateFormatter)

    val corpus = LeipzigTextCorpus(file, sources, sentences2sources, language, textType, localDate)
    println("Added to processing: %s".format(corpus.toString))

    corpus
  }

  private def fixEncoding(corpus: LeipzigTextCorpus) = {

    val source = corpus.sentences
    val target = corpus.sentencesFixed

    if (!Files.exists(target)) {
      println("Fixing encoding of '%s'.".format(source))
      val command = Scripts.fixEncoding(source, target)
      command!
    }
  }
}



