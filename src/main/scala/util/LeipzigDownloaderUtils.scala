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

import java.io.{FileInputStream, File}
import java.nio.file.Path

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.io.FilenameUtils._
import util.IOUtils._

import scala.collection.mutable
import scala.io.{BufferedSource, Source}

object LeipzigDownloaderUtils {

  /**
   * @param dir path where to store extracted and converted file
   * @param fileToExtract a bz2 compressed file
   */
  def extractAndConvert(dir: Path, fileToExtract: File) = {

    val fileBuffer = extractToBuffer(fileToExtract)

    val baseFilename = getBaseName(fileToExtract.getName)
    convertToLLC(dir, baseFilename, fileBuffer)
  }

  private def extractToBuffer(file: File) = {

    val input = new BZip2CompressorInputStream(new FileInputStream(file))
    Source.fromInputStream(input, utf8)
  }

  private def convertToLLC(dir: Path, baseFilename: String, fileBuffer: BufferedSource) = {

    val sources: mutable.Map[String, Int] = mutable.Map()
    val sentences: mutable.Map[Int, String] = mutable.Map()
    val sentences2Sources: mutable.Map[Int, Int] = mutable.Map()

    for((line, sentenceId) <- fileBuffer.getLines().zipWithIndex) {
      val Array(sentence, source) = line.split(tab)

      //Prevent Ill-Formed file
      if(sentence.nonEmpty && source.nonEmpty) {
        val sourceId = sources.getOrElseUpdate(source, sources.size + 1)

        sentences += (sentenceId -> sentence)
        sentences2Sources += (sentenceId -> sourceId)
      }
    }

    //This corpus size var could be removed, because it's not used anymore
    val corpusSize = Math.ceil(sentences2Sources.size.toDouble / 1000000.0).toInt
    val corpusName = s"dail_news_${baseFilename}_${corpusSize}M-%s"
    //TODO: Improve this implicit coding of the date in the filename. It's somehow confusing
    val dateFileFormat = baseFilename.replaceFirst("(\\d{4})(\\d{2})(\\d{2})", "$1-$2-$3")

    writeToFiles(dir, corpusName, dateFileFormat, sentences, sources, sentences2Sources)
  }

  private def writeToFiles(dir: Path, corpusName: String, date: String, sentences: mutable.Map[Int, String],
                           sources: mutable.Map[String, Int], sentences2Sources: mutable.Map[Int, Int]) = {

    //TODO: Here a common naming with Leipzig reader make available the endings
    val sourceFile = dir.resolve(corpusName.format("sources.txt"))
    val sentenceFile = dir.resolve(corpusName.format("sentences.txt"))
    val sentence2SourcesFile = dir.resolve(corpusName.format("inv_so.txt"))

    withOutputs(sourceFile, sentenceFile, sentence2SourcesFile) {
      case Seq(sourceOut, sentOut, sent2sourceOut) =>

        sentences.foreach { case (id, sent) =>
          sentOut.write(toTsv(id, sent, date))
        }

        sources.foreach { case (source, id) =>
          sourceOut.write(toTsv(id, source, date))
        }

        sentences2Sources.foreach { case (sentId, sourceId) =>
          sent2sourceOut.write(toTsv(sentId, sourceId))
        }
    }
  }
}
