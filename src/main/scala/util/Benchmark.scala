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
 */

package util

import java.util.concurrent.TimeUnit._

import scala.language.implicitConversions


object Benchmark {

  implicit def toBenchmarkable[B](op: => B) = new Benchmarkable(op)
}


class Benchmarkable[B](op: => B) {

  /**
   * Given a message, logs it at the beginning of the operation, performs the operation, logs
   * the message again with the time (in seconds) it took to execute the operation, and returns
   * the operation's result.
   *
   * Use like this:
   *
   *   ([block of code]).withBenchmark("Performing operation")
   *
   * This will result in the output:
   *
   *   """
   *   Performing operation...
   *   Performing operation... [n seconds]
   *   """
   */
  def withBenchmark(message: String) = {

    println(message + "...")

    val start = System.nanoTime
    val result = op
    val end = System.nanoTime
    val seconds = NANOSECONDS.toSeconds(end - start)

    println("%s... Done. [%d seconds]".format(message, seconds))

    result
  }
}