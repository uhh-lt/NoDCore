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

import database.DatabaseUsage
import pipeline.Settings.defaultSettings
import scalikejdbc._
import util.DBPediaUtils._

//TODO: Handle Type 0 1 same entity, because it has the same id :(
class Importer(cleaner: CleaningStrategy) extends ProcessingUnit with DatabaseUsage {

  override val moduleName: String = "Database Importer"

  val useDBPediaFeature = defaultSettings.config.getBoolean("nod.component.importer.applyDBPedia")

  override def produce: (Cas) => Cas = (c: Cas) => {

    println("Start Importing...")

    DB localTx { implicit session =>
      beforeImport

      insertDataToDatabase(c.outputDir)
      cleaner.clean

      if(useDBPediaFeature) {

        println("Fetch thumbnails from DBPedia")
        fetchEntityImageLinks
      }

      mergeTables

      afterImport
    }

    c
  }

  private def insertDataToDatabase(from: Path)(implicit session: DBSession)  = {

    for ((table, file) <- defaultSettings.tablesToFiles) {

      val query = SQL(
        """
            LOAD DATA LOCAL INFILE
            '%s' INTO table %s;
        """.format(from.resolve(file).toAbsolutePath.toString(), table + "_tmp"))

      query.execute.apply()
    }
  }

  def fetchEntityImageLinks()(implicit session: DBSession) = {

    val entities = sql"""SELECT id, name, dayFrequency FROM entities_tmp
                         WHERE (SELECT COUNT(*) FROM entities_to_links
                                WHERE entities_to_links.entity_id = id) = 0
                         AND dayFrequency > 1;""".map( rs => (rs.long("id"), rs.string("name"))).list.apply()

    val batchParams: Seq[Seq[Any]] = imageLinksForNames(entities)

    sql"INSERT INTO entities_to_links (entity_id, link) values(?, ?)".batch(batchParams: _*).apply()
  }

  def mergeTables()(implicit session: DBSession) = {

    defaultSettings.tables foreach { table =>

      val tmpTable = table + "_tmp"
      SQL("INSERT IGNORE INTO %s SELECT * FROM %s;".format(table, tmpTable)).update.apply()
    }
  }

  def beforeImport()(implicit session: DBSession) = {
    sql"""SET NAMES utf8;""".execute.apply()

    defaultSettings.tables foreach { table =>

      val tmpTable = table + "_tmp"
      SQL("CREATE TEMPORARY TABLE %s LIKE %s;".format(tmpTable, table)).execute.apply()
      SQL("ALTER TABLE %s DISABLE KEYS;".format(tmpTable)).execute.apply()
    }
  }

  def afterImport()(implicit session: DBSession) = {

    defaultSettings.tables foreach { table =>

      SQL("DROP TEMPORARY TABLE %s;".format(table + "_tmp")).execute.apply()
    }
  }
}
