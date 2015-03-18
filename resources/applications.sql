-- name: read-applications
SELECT id, name, team_id
  FROM kio.application;

--name: read-application
SELECT id, name, team_id, description, url, scm_url, documentation_url
  FROM kio.application
 WHERE id = :id;

-- name: create-or-update-application!
WITH application_update AS (
     UPDATE kio.application
        SET name              = :name,
            team_id           = :team_id,
            description       = :description,
            url               = :url,
            scm_url           = :scm_url,
            documentation_url = :documentation_url
      WHERE id = :id
  RETURNING *)
INSERT INTO kio.application
            (id, name, team_id, description, url, scm_url, documentation_url)
     SELECT :id, :name, :team_id, :description, :url, :scm_url, :documentation_url
      WHERE NOT EXISTS (SELECT * FROM application_update);

-- name: update-application-secret!
UPDATE kio.application
   SET secret = :secret
 WHERE id = :id;

-- name: delete-application!
DELETE FROM kio.application
      WHERE id = :id;


