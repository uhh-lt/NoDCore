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

import scalikejdbc._

object DatabaseSchema {
  
  def createSchemaIfNotExists()(implicit session: DBSession) = {

    sql"""CREATE TABLE IF NOT EXISTS entities (
            id BIGINT,
            type INT NOT NULL,
            name VARCHAR(255) NOT NULL,
            date DATE NOT NULL,
            useEntity Boolean NOT NULL DEFAULT 1,
            dayFrequency INT NOT NULL DEFAULT 0,
            CONSTRAINT entities_pkey PRIMARY KEY(id, date)
          ) ENGINE = MyISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS relationships (
            id BIGINT,
            entity1 BIGINT NOT NULL,
            entity2 BIGINT NOT NULL,
            date DATE NOT NULL,
            dayFrequency INT NOT NULL DEFAULT 0,
            CONSTRAINT relationships_pkey PRIMARY KEY(id, date)
          ) ENGINE = MyISAM;""".execute.apply()


    sql"""CREATE TABLE IF NOT EXISTS sentences (
            id BIGINT,
            sentence TEXT NOT NULL,
            CONSTRAINT sentences_pkey PRIMARY KEY(id)
          ) ENGINE = MyISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS sources (
            id BIGINT,
            source VARCHAR(255) NOT NULL,
            date DATE NOT NULL,
            CONSTRAINT sources_pkey PRIMARY KEY(id)
          ) ENGINE = MyISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS sentences_to_sources (
            sentence_id BIGINT NOT NULL,
            source_id BIGINT NOT NULL
          ) ENGINE = MyISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS relationships_to_sentences (
            relationship_id BIGINT NOT NULL,
            sentence_id BIGINT NOT NULL
          ) ENGINE = MyISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS labels (
            id BIGINT NOT NULL AUTO_INCREMENT,
            label VARCHAR(255) NOT NULL,
            CONSTRAINT labels_pkey PRIMARY KEY(id)
          ) ENGINE = MyISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS tags (
            id BIGINT NOT NULL AUTO_INCREMENT,
            relationship_id BIGINT NOT NULL,
            sentence_id BIGINT NOT NULL,
            label_id BIGINT NOT NULL,
            direction VARCHAR(1) NOT NULL,
            created Date NOT NULL,
            showOnEdge boolean NOT NULL DEFAULT false,
            situative boolean NOT NULL,
            CONSTRAINT tags_pkey PRIMARY KEY(id)
          ) ENGINE = MyISAM;""".execute.apply()


    sql"""CREATE TABLE IF NOT EXISTS entities_to_links (
            entity_id BIGINT NOT NULL,
            link TEXT NOT NULL
          ) ENGINE = MYISAM;""".execute.apply()

    sql"""CREATE TABLE IF NOT EXISTS trendwords (
            cluster_id BIGINT NOT NULL,
            entity_id  BIGINT NOT NULL
          ) ENGINE = MYISAM;""".execute.apply()


    sql"""CREATE TABLE IF NOT EXISTS clusters (
            id BIGINT NOT NULL AUTO_INCREMENT,
            date DATE NOT NULL,
            json LONGTEXT NOT NULL,
            CONSTRAINT id_pkey PRIMARY KEY(id)
          ) ENGINE = MyISAM;""".execute.apply()
  }

  def createIndexIfNotExists()(implicit session: DBSession) = {

    if(!indexExists) {

      // Indexes for direct access
      sql"CREATE INDEX e_pkey_index ON entities (id);".execute.apply()
      sql"CREATE INDEX e_date_index ON entities (date);".execute.apply()
      sql"CREATE INDEX e_id_date_index ON entities (id,date);".execute.apply()

      sql"""CREATE INDEX r_id_date_index ON relationships (id,date);""".execute.apply()
      sql"""CREATE INDEX r_pkey_index ON relationships (id);""".execute.apply()
      sql"""CREATE INDEX r_date_index ON relationships (date);""".execute.apply()

      sql"""CREATE INDEX s_pkey_index ON sentences (id);""".execute.apply()
      sql"""CREATE INDEX so_pkey_index ON sources (id);""".execute.apply()
      sql"""CREATE INDEX c_date_index ON clusters (date);""".execute.apply()

      // Foreign Key indexes
      sql"""CREATE INDEX r_e1_fkey_index ON relationships(entity1);""".execute.apply()
      sql"""CREATE INDEX r_e2_fkey_index ON relationships(entity2);""".execute.apply()
      sql"""CREATE INDEX r2s_r_fkey_index ON relationships_to_sentences (relationship_id);""".execute.apply()
      sql"""CREATE INDEX r2s_s_fkey_index ON relationships_to_sentences (sentence_id);""".execute.apply()
      sql"""CREATE INDEX e2l_fkey_index ON entities_to_links (entity_id);""".execute.apply()
      sql"""CREATE INDEX t_c_fkey_index ON trendwords (cluster_id);""".execute.apply()


      // Index for fast JOINs on sentences_to_sources
      sql"""CREATE INDEX s2s_s_index ON sentences_to_sources (sentence_id);""".execute.apply()

      // Full Text Search on Entity Names
      sql"""CREATE INDEX entities_name_index ON entities(name) USING BTREE;""".execute.apply()

      // -- labels
      sql"""CREATE INDEX l_pkey_index ON labels (id);""".execute.apply()
      sql"""CREATE INDEX labels_label_index ON labels(label) USING BTREE;""".execute.apply()

      // -- tags
      sql"""CREATE INDEX t_pkey_index ON tags (id);""".execute.apply()
      sql"""CREATE INDEX t_r_fkey_index ON tags(relationship_id);""".execute.apply()
      sql"""CREATE INDEX t_l_fkey_index ON tags(label_id);""".execute.apply()
    }
  }

  private def indexExists()(implicit session: DBSession) = {

    val index = sql"""SHOW INDEX FROM entities WHERE KEY_NAME = 'e_pkey_index';""".map(rs => rs.string("Key_name")).single().apply()
    index.isDefined
  }
}
