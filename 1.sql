# --- !Ups

CREATE TABLE entities (
  id BIGINT,
  type INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  date DATE NOT NULL,
  useEntity Boolean NOT NULL DEFAULT 1,
  dayFrequency INT NOT NULL DEFAULT 0,
  CONSTRAINT entities_pkey PRIMARY KEY(id, date)
) ENGINE = MyISAM;

CREATE TABLE relationships (
  id BIGINT,
  entity1 BIGINT NOT NULL, -- REFERENCES entities(id)
  entity2 BIGINT NOT NULL, -- REFERENCES entities(id)
  date DATE NOT NULL,
  dayFrequency INT NOT NULL DEFAULT 0,
  CONSTRAINT relationships_pkey PRIMARY KEY(id, date)
) ENGINE = MyISAM;

CREATE TABLE sentences (
  id BIGINT,
  sentence TEXT NOT NULL,
  CONSTRAINT sentences_pkey PRIMARY KEY(id)
) ENGINE = MyISAM;

CREATE TABLE sources (
  id BIGINT,
  source VARCHAR(255) NOT NULL,
  date DATE NOT NULL,
  CONSTRAINT sources_pkey PRIMARY KEY(id)
) ENGINE = MyISAM;

CREATE TABLE sentences_to_sources (
  sentence_id BIGINT NOT NULL,  -- REFERENCES sentences(id)
  source_id BIGINT NOT NULL 	-- REFERENCES sources(id)
) ENGINE = MyISAM;

CREATE TABLE relationships_to_sentences (
  relationship_id BIGINT NOT NULL, -- REFERENCES relationships(id)
  sentence_id BIGINT NOT NULL      -- REFERENCES sentences(id)
) ENGINE = MyISAM;

CREATE TABLE labels (
  id BIGINT NOT NULL AUTO_INCREMENT,
  label VARCHAR(255) NOT NULL,
  CONSTRAINT labels_pkey PRIMARY KEY(id)
) ENGINE = MyISAM;

CREATE TABLE tags (
  id BIGINT NOT NULL AUTO_INCREMENT,
  relationship_id BIGINT NOT NULL, 	 -- REFERENCES relationships(id)
  sentence_id BIGINT NOT NULL,		 -- REFERENCES sentences(id)
  label_id BIGINT NOT NULL,	         -- REFERENCES labels(id)
  direction VARCHAR(1) NOT NULL,
  created Date NOT NULL,
  showOnEdge boolean NOT NULL DEFAULT false,
  situative boolean NOT NULL,
  CONSTRAINT tags_pkey PRIMARY KEY(id)
) ENGINE = MyISAM;

CREATE TABLE entities_to_links (
  entity_id BIGINT NOT NULL, -- REFERENCES entities(id)
  link TEXT NOT NULL,
  CONSTRAINT entity_pkey PRIMARY KEY(entity_id)
) ENGINE = MYISAM;
 

CREATE TABLE trendwords (
  cluster_id BIGINT NOT NULL, 	   -- REFERENCES clusters(id)
  entity_id  BIGINT NOT NULL       -- REFERENCES entities(id)
) ENGINE = MYISAM;

CREATE TABLE clusters (
  id BIGINT NOT NULL AUTO_INCREMENT,
  date DATE NOT NULL,
  json LONGTEXT NOT NULL,
  CONSTRAINT id_pkey PRIMARY KEY(id)
) ENGINE = MyISAM;

# --- !Downs

DROP TABLE entities;
DROP TABLE relationships;
DROP TABLE sentences;
DROP TABLE sources;
DROP TABLE sentences_to_sources;
DROP TABLE relationships_to_sentences
DROP TABLE labels;
DROP TABLE tags;
DROP TABLE entities_to_images;
DROP TABLE trendwords;
DROP TABLE clusters;
