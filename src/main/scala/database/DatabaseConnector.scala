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

package database

import javax.sql.DataSource

import com.zaxxer.hikari.HikariDataSource
import database.DatabaseSchema._
import pipeline.Settings
import scalikejdbc.{DB, ConnectionPool, DataSourceConnectionPool}


object DatabaseSettings {

  val host = Settings.getParameterAsString("db.default.host")
  val database = Settings.getParameterAsString("db.default.db")
  val user = Settings.getParameterAsString("db.default.user")
  val password = Settings.getParameterAsString("db.default.password")
  val dataSourceClassName = Settings.getParameterAsString("db.default.datasource")

  val dataSource: DataSource = {

    val ds = new HikariDataSource()
    ds.setDataSourceClassName(dataSourceClassName)
    ds.addDataSourceProperty("url", s"jdbc:mysql://$host/$database?characterSetResults=utf8&character_set_server=utf8mb4&useUnicode=true&characterEncoding=UTF-8")
    ds.addDataSourceProperty("user", user)
    ds.addDataSourceProperty("password", password)
    ds
  }

  lazy val init = {

    Class.forName(Settings.getParameterAsString("db.default.driver"))
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    DB localTx { implicit session =>
      createSchemaIfNotExists()
      createIndexIfNotExists()
    }
  }
}

trait DatabaseUsage {
  DatabaseSettings.init
}
