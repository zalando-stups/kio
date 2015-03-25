-- name: read-applications
SELECT id, team_id, active, name, service_url
  FROM application;

--name: read-application
SELECT id, team_id, active, name, subtitle, description, service_url, scm_url, documentation_url
  FROM application
 WHERE id = :id;

-- name: create-or-update-application!
WITH application_update AS (
     UPDATE application
        SET team_id           = :team_id,
            active            = :active,
            name              = :name,
            subtitle          = :subtitle,
            description       = :description,
            service_url       = :service_url,
            scm_url           = :scm_url,
            documentation_url = :documentation_url
      WHERE id = :id
  RETURNING *)
INSERT INTO application
            (id, team_id, active, name, subtitle, description, service_url, scm_url, documentation_url)
     SELECT :id, :team_id, :active, :name, :subtitle, :description, :service_url, :scm_url, :documentation_url
      WHERE NOT EXISTS (SELECT * FROM application_update);
