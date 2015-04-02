-- name: read-applications
SELECT id, team_id, active, name, subtitle, service_url
  FROM application;

-- name: search-applications
SELECT id,
  team_id,
  active,
  name,
  subtitle,
  service_url,
  ts_rank_cd(vector, query) AS matched_rank,
  ts_headline('simple', description, query) AS matched_description
FROM (SELECT id,
        team_id,
        active,
        name,
        subtitle,
        service_url,
        description,
        setweight(to_tsvector('simple', name), 'A')
        || setweight(to_tsvector('simple', COALESCE(subtitle, '')), 'B')
        || setweight(to_tsvector('simple', COALESCE(description, '')), 'C')
          as vector
      FROM application) as apps,
  to_tsquery('simple', :searchquery) query
WHERE query @@ vector
ORDER BY matched_rank DESC;

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
