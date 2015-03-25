-- base entity: application
CREATE TABLE application (
-- the official application ID, like 'kio' or 'pierone'
  id                TEXT NOT NULL,
-- the team ID of the owning team
  team_id           TEXT NOT NULL,

-- if the application is active
  active            BOOL NOT NULL,

-- a human readable name, like 'Kio' or 'Pier One'
  name              TEXT NOT NULL,
-- a human readable addition to the name like 'Application Registry'
  subtitle          TEXT,
-- some markdown formatted long description
  description       TEXT,

-- URL where to access the service if it provides one
  service_url       TEXT,
-- URL to the source code management system if there is some
  scm_url           TEXT,
-- URL to the application's documentation if it provides one
  documentation_url TEXT,

  PRIMARY KEY (id)
);

-- an application's version
CREATE TABLE version (
-- a unique version of the application like '1.0'
  id             TEXT NOT NULL,
-- the application's ID (see above)
  application_id TEXT NOT NULL REFERENCES application (id),

-- reference to the used artifact, e.g. the docker image and version like 'docker://stups/kio:1.0'
  artifact       TEXT,

-- release notes in markdown
  notes          TEXT,

  PRIMARY KEY (id, application_id)
);

-- an approval for an application's version
CREATE TABLE approval (
-- the application
  application_id TEXT NOT NULL,
-- the application's version
  version_id     TEXT NOT NULL,
-- the approving user
  user_id        TEXT NOT NULL,
-- what kind of approval
  approval_type  TEXT NOT NULL,

-- when this was actually approved
  approved_at     TIMESTAMP NOT NULL DEFAULT NOW(),

  PRIMARY KEY (application_id, version_id, user_id, approval_type),
  FOREIGN KEY (application_id, version_id) REFERENCES version (application_id, id)
);
