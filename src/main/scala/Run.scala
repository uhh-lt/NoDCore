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
 */

import java.io.File
import java.nio.file.{Path, Paths}

import corpus.LeipzigTextReader
import org.apache.commons.io.FileUtils
import pipeline._
import util.IOUtils._
import util.LeipzigDownloaderUtils

object InputFormat extends Enumeration {
  type format = Value
  val compressed, llc = Value
}

case class ArgumentOptions(format: InputFormat.Value = InputFormat.compressed, directory: File = new File("NONE"),
                           file: File = new File("NONE"), downloadURI: String = "") {
  
  def hasFileParameter = file.getName != "NONE"
}

object Run {

  implicit val InputFormatRead: scopt.Read[InputFormat.Value] = scopt.Read.reads(InputFormat withName _)

  val compressedExtension = ".bz2"

  val defaultCorpusDir = Paths.get("corpus/")
  val defaultProcessingDir = Paths.get("corpus/output")

  def main(args:Array[String]): Unit = {

    val parser = createParser()
    parser.parse(args, ArgumentOptions()) match {
      case Some(config) => configurePipeline(config)
      case None => parser.showUsage
    }
  }

  def configurePipeline(arguments: ArgumentOptions) = {

    println("Configure Pipeline")
    createFolderIfNotExists(arguments.directory.toPath)

    arguments.format match {
      case InputFormat.compressed =>

        val files = if(arguments.downloadURI.nonEmpty) doDownloadImport(arguments) else doLocalImport(arguments)
        files.foreach { f => runPipeline(f) }

      case InputFormat.llc => throw new IllegalArgumentException("Not implemented yet.")
    }
  }

  def doDownloadImport(arg: ArgumentOptions): List[File] = {
    println("Start downloading...")

    val leipzigLoader = new LeipzigDownloader(arg.downloadURI, arg.directory.toPath)
    if(arg.hasFileParameter) leipzigLoader.downloadSingle(arg.file) else leipzigLoader.downloadAll()
  }

  def doLocalImport(arg: ArgumentOptions): List[File] = {

    val sourceDir = arg.directory.toPath
    if (arg.hasFileParameter) List(sourceDir.resolve(arg.file.toPath).toFile) else
                              listFiles(sourceDir.toFile, compressedExtension).toList
  }

  def runPipeline(file: File) = {

    //Create import env
    createFolderIfNotExists(defaultCorpusDir)
    createFolderIfNotExists(defaultProcessingDir)

    LeipzigDownloaderUtils.extractAndConvert(defaultCorpusDir, file)
    val pipe = createPipelineWithCorpus(defaultCorpusDir)
    pipe.produce(defaultProcessingDir)
    //Clean up
    FileUtils.forceDelete(defaultCorpusDir.toFile)
  }

  def createPipelineWithCorpus(corpusDir: Path) = {

   val cleaner = new AdvancedCleaner()
    new LeipzigTextReader(corpusDir) -> new EntityExtractor() -> new Aggregator() ->
      new Importer(cleaner) -> new Clustering()
  }

  def createParser() = {

    new scopt.OptionParser[ArgumentOptions]("nodcore") {
      head("NoDCore", "0.9")
      help("help") text("prints this usage text.")
      version("version") text("Version 0.9")

      opt[InputFormat.Value]("format") required() action { (x, c) =>
        c.copy(format = x)
      } text """[required] Format/Type of the given file parameter.
                The <value> could be either "compressed" or "llc"."""

      opt[File]("dir") required() action { (x, c) =>
        c.copy(directory = x)
      } text """[required] Path where the given file is located.
                If the download flag is specified, the downloaded
                files will be stored in this directory."""

      opt[File]("file") optional() action { (x, c) =>
        c.copy(file = x)
      } text """[optional] File in the given directory to process.
                If none is specified, every file is processed. If
                the download flag is specified, this parameter indicates
                the file to download. If none is specified and the
                download flag is set, it will download all files if
                not already present in the given directory."""

      opt[String]("download") optional() action { (x, c) =>
        c.copy(downloadURI = x)
      } text """[optional] Server URL to fetch files in the given format.
                Files will be stored in the given directory if not already
                downloaded."""
    }
  }
}
