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

package model

import java.time.LocalDate
import java.time.LocalDate._

import util.IOUtils


case class Source(id: Long, url: String, date: LocalDate) {

  def copyWith(id: Long = this.id, url: String = this.url, date: LocalDate = this.date) = new Source(id, url, date)

  def toTsv = IOUtils.toTsv(id, url, date.toString)
}

object Source {

  def unapply(line: String): Option[(Long, String, LocalDate)] = {

    line.split(IOUtils.tab) match {
      case Array(id, url, date) => Some((id.trim.toLong, url.trim, parse(date)))
      case _ => None
    }
  }

  def fromTsv(line: String): Source = {

    line match {
      case Source(id, url, date) => new Source(id, url, date)
      case _ => throw new IllegalArgumentException("Illegal line format of source '%s'".format(line))
    }
  }
}
