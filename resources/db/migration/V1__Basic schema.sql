CREATE SCHEMA zk_data;
SET search_path TO zk_data;

-- base entity: application
CREATE TABLE application (
-- the official application ID, like 'kio' or 'pierone'
  a_id                TEXT NOT NULL,
-- the team ID of the owning team
  a_team_id           TEXT NOT NULL,

-- if the application is active
  a_active            BOOL NOT NULL,

-- a human readable name, like 'Kio' or 'Pier One'
  a_name              TEXT NOT NULL,
-- a human readable addition to the name like 'Application Registry'
  a_subtitle          TEXT,
-- some markdown formatted long description
  a_description       TEXT,

-- URL where to access the service if it provides one
  a_service_url       TEXT,
-- URL to the source code management system if there is some
  a_scm_url           TEXT,
-- URL to the application's documentation if it provides one
  a_documentation_url TEXT,
-- URL to the specification tool if it provides one
  a_specification_url TEXT,

  PRIMARY KEY (a_id)
);

-- an application's version
CREATE TABLE version (
-- a unique version of the application like '1.0'
  v_id             TEXT NOT NULL,
-- the application's ID (see above)
  v_application_id TEXT NOT NULL REFERENCES application (a_id),

-- reference to the used artifact, e.g. the docker image and version like 'docker://stups/kio:1.0'
  v_artifact       TEXT,

-- release notes in markdown
  v_notes          TEXT,

  PRIMARY KEY (v_id, v_application_id)
);

-- an approval for an application's version
CREATE TABLE approval (
-- the application
  ap_application_id TEXT NOT NULL,
-- the application's version
  ap_version_id     TEXT NOT NULL,
-- the approving user
  ap_user_id        TEXT NOT NULL,
-- what kind of approval
  ap_approval_type  TEXT NOT NULL,

-- when this was actually approved
  ap_approved_at    TIMESTAMP NOT NULL DEFAULT NOW(),

-- some hints
  ap_notes          TEXT,

  PRIMARY KEY (ap_application_id, ap_version_id, ap_user_id, ap_approval_type),
  FOREIGN KEY (ap_application_id, ap_version_id) REFERENCES version (v_application_id, v_id)
);
