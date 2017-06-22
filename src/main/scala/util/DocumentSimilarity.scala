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
 *         - Uli Fahrer
 */

package util

/**
 * Compute document similarities based on the recommendation here
 * http://chepurnoy.org/blog/2014/03/faster-cosine-similarity-between-two-dicuments-with-scala-and-lucene/
 */
object DocumentSimilarity {

  def documentFrequency(document: String): Map[String, Int] = {

    document.split(" ").groupBy(e => e).map(e => e._1 -> e._2.length)
  }

  def similarity(t1: Map[String, Int], t2: Map[String, Int]): Double = {
    //word, t1 freq, t2 freq
    val m = scala.collection.mutable.HashMap[String, (Int, Int)]()

    val sum1 = t1.foldLeft(0d) {
      case (sum, (word, freq)) =>
        m += word -> (freq, 0)
        sum + freq
    }

    val sum2 = t2.foldLeft(0d) {
      case (sum, (word, freq)) =>
        m.get(word) match {
          case Some((freq1, _)) => m += word -> (freq1, freq)
          case None => m += word -> (0, freq)
        }
        sum + freq
    }

    val (p1, p2, p3) = m.foldLeft((0d, 0d, 0d)) {
      case ((s1, s2, s3), e) =>
        val fs = e._2
        val f1 = fs._1 / sum1
        val f2 = fs._2 / sum2
        (s1 + f1 * f2, s2 + f1 * f1, s3 + f2 * f2)
    }

    val cos = p1 / (Math.sqrt(p2) * Math.sqrt(p3))
    cos
  }
}
