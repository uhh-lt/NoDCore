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
 *         - Seid Muhie Yimam (initial code)
 *         - Uli Fahrer (scala port)
 */

package pipeline

import java.io.File
import java.net.URL
import java.nio.file.Path

import org.jsoup.Jsoup._

import scala.collection.JavaConversions._
import scala.sys.process._

class LeipzigDownloader(baseUrl: String, downloadDir: Path) {

  //Will download all files if not already present in the downloadDir
  //In case it already exists nothing will happen
  def downloadAll(): List[File] = {

    val document = connect(baseUrl).get
    val filenames = document.select("td a").map { _.attr("href") }.filter { _.endsWith(".bz2") }

    //TODO: Handle case empty list to tell user all files exist do local import
    val newFiles = filenames.map(downloadDir.resolve(_).toFile).filter(!_.exists)
    newFiles.map { file => download(baseUrl + file.getName, file) }.toList
  }

  //Will download the file if not already exists
  def downloadSingle(file: File): List[File] = {

    val fileToDownload = downloadDir.resolve(file.getName).toFile

    if(fileToDownload.exists()) {
      println("File %s already exists. Do local import instead!".format(file.toString))
      List()
    } else {
      List(download(baseUrl + fileToDownload.getName, fileToDownload))
    }
  }

  private def download(url: String, file: File): File = {
    println("Download file %s from %s".format(file.toString, url))
    new URL(url) #> file !!

    file
  }
}
