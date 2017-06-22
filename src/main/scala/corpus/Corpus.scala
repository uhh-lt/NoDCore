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

package corpus

import java.time.LocalDate

import model.{Source, Sentence}


trait Corpus {

  /**
   *  Returns a list containing all dates that are represented by that corpus
   */
  def dates(): List[LocalDate]

  /**
   * Returns an iterator of sentences in the corpus.
   */
  def sentences(): Iterator[Sentence]

  /**
   * Returns an iterator of sources in the corpus.
   */
  def sources(): Iterator[Source]

  /**
   * Provides an iterator that contains relations between sentences and sources.
   * For instance, if sentence n is from source m, it contains the entry List(n, m).
   */
  def sentences2sources(): Iterator[List[Long]]
}
