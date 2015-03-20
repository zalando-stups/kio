-- name: read-applications
SELECT id, name, team_id
  FROM application;

--name: read-application
SELECT id, name, team_id, description, url, scm_url, documentation_url
  FROM application
 WHERE id = :id;

-- name: create-or-update-application!
WITH application_update AS (
     UPDATE application
        SET name              = :name,
            team_id           = :team_id,
            description       = :description,
            url               = :url,
            scm_url           = :scm_url,
            documentation_url = :documentation_url
      WHERE id = :id
  RETURNING *)
INSERT INTO application
            (id, name, team_id, description, url, scm_url, documentation_url)
     SELECT :id, :name, :team_id, :description, :url, :scm_url, :documentation_url
      WHERE NOT EXISTS (SELECT * FROM application_update);

-- name: update-application-secret!
UPDATE application
   SET secret = :secret
 WHERE id = :id;

-- name: delete-application!
DELETE FROM application
      WHERE id = :id;
