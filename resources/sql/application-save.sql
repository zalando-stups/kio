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
INSERT INTO kio.application (id, name, team_id, description, url, scm_url, documentation_url)
     SELECT :id, :name, :team_id, :description, :url, :scm_url, :documentation_url
      WHERE NOT EXISTS (SELECT * FROM application_update);
