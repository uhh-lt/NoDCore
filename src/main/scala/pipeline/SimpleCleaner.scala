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

import scalikejdbc._

class SimpleCleaner extends CleaningStrategy {

  override def removeUnusedSentences()(implicit session: DBSession) = {

    sql"""DELETE FROM sentences_tmp WHERE
          (SELECT COUNT(*) FROM relationships_to_sentences_tmp
           WHERE sentence_id = sentences_tmp.id) = 0;
      """.update.apply()


    sql"""DELETE FROM sentences_to_sources_tmp WHERE
          (SELECT COUNT(*) FROM sentences_tmp
          WHERE id = sentences_to_sources_tmp.sentence_id) = 0;
      """.update.apply()
  }

  override def removeUnusedEntities()(implicit session: DBSession) = {

    sql"""DELETE FROM entities_tmp WHERE
          (SELECT COUNT(*) FROM relationships_tmp
          WHERE entity1 = entities_tmp.id OR entity2 = entities_tmp.id) = 0;
      """.update.apply()
  }
}
