CREATE SCHEMA IF NOT EXISTS kio;

CREATE TABLE IF NOT EXISTS kio.application (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  team_id TEXT NOT NULL,
  description TEXT  NOT NULL,
  url TEXT ,
  scm_url TEXT NOT NULL,
  documentation_url TEXT,
  secret TEXT
);