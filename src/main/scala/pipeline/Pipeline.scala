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
import model.{EntityStore, RelationshipStore}


abstract class Pipeline[-I, +O] {

  val moduleName: String
  def produce: I => O

  def ->[X](seg:Pipeline[_ >: O, X]):Pipeline[I, X] = {

    val function = this.produce
    val outerName = this.moduleName
    new Pipeline[I, X] {
      //TODO: Run with benchmark and log result!
      val moduleName = outerName + "." + seg.moduleName
      def produce = function andThen seg.produce
    }
  }
}

case class Cas(corpus: Corpus, outputDir: Path, entityStore: EntityStore, relationshipStore: RelationshipStore) {

  def releaseResources() = {
    entityStore.releaseResources()
    relationshipStore.releaseResources()
  }
}

abstract class CollectionReader extends Pipeline[Path, Cas]
abstract class ProcessingUnit extends Pipeline[Cas, Cas]
