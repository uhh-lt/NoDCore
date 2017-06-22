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
 *       - Artjom Kochtchi
 *       - Uli Fahrer
 */

package model

import java.time.LocalDate
import java.time.LocalDate._

import util.IOUtils


case class Sentence(id: Long, sentence: String, date: LocalDate) {

  def toTsv = IOUtils.toTsv(id, sentence)

  def copyWith(id: Long = this.id, sentence: String = this.sentence, date: LocalDate = this.date) = {

    new Sentence(id, sentence, date)
  }
}

object Sentence {

  def unapply(line: String): Option[(Long, String, LocalDate)] = {

    line.split(IOUtils.tab) match {
      case Array(id, sentence, date) => Some((id.trim.toLong, sentence.trim, parse(date)))
      case _ => None
    }
  }

  def fromTsv(line: String): Sentence = {

    line match {
      case Sentence(id, sentence, date) => new Sentence(id, sentence, date)
      case _ => throw new IllegalArgumentException("Illegal line format of sentence '%s'".format(line))
    }
  }


}
