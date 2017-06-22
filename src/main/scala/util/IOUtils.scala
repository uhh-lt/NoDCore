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
 *         - Artjom Kochtchi
 *         - Uli Fahrer
 */

package util

import java.io.{File, BufferedWriter}
import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils

import scala.io.Source


object IOUtils {

  val tab = "\t"
  val newline = "\n"
  val comma = ","
  val utf8 = "UTF-8"

  /**
   * Creates a buffered line-by-line iterator for a given path.
   *
   * @param path path to file
   */
  def getInputLines(path: Path) = Source.fromFile(path.toUri(), utf8).getLines

  /**
   * Opens a file for writing. Use like this:
   *   withOutput(path) { out
   *     out.write("...")
   *   }
   */
  def withOutputs[B](paths: Path*)(op: Seq[BufferedWriter] => B): B = {

    val outs = for (path <- paths)
      yield Files.newBufferedWriter(path, Charset.forName("UTF-8"))
    try {
      op(outs)
    } finally {
      for (out <- outs) out.close()
    }
  }

  def withOutput[B](path: Path)(op: BufferedWriter => B): B = {

    withOutputs(path) { case Seq(out) =>
      op(out)
    }
  }

  /**
   * Converts a sequence of objects to a tab separated line format.
   */
  def toTsv(fields: Any*) = fields.mkString(tab) + newline

  def distinctSplit(value: String, delimiter: String = comma) = value.split(delimiter).map(_.trim).distinct

  def createFolderIfNotExists(folder: Path) = {

    if(!Files.exists(folder)) {
      FileUtils.forceMkdir(folder.toFile)
    }
  }

  //def fileExists(file: File) = file.length() == 0

  def listFiles(dir: File, fileExtension: String) = {

    dir.listFiles().filter(_.getName.endsWith(fileExtension))
  }
}
