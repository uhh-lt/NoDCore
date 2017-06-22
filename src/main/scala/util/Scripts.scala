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

import java.nio.file.{Path, Paths}

object Scripts {

  private def runScript(scriptName: String, source: Path, target: Path) = {

    val scriptPath = Paths.get("resources/scripts/") resolve scriptName
    val script = scriptPath.toFile
    Seq(script.getAbsolutePath, source.toAbsolutePath.toString, target.toAbsolutePath.toString)
  }

  /**
   * Extracts the fix_encoding script and returns a command for executing it.
   *
   * @param source The file to fix encoding.
   * @param target The output file.
   */
  def fixEncoding(source: Path, target: Path) = runScript("fix_encoding", source, target)

  /**
   * Extracts the aggregate_count script and returns a command for executing it.
   *
   * @param source The file to aggregate.
   * @param target The output file.
   */
  def countUnique(source: Path, target: Path) = runScript("aggregate_count", source, target)
}
