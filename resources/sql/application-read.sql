SELECT id, name, team_id, description, url, scm_url, documentation_url
  FROM kio.application
 WHERE id = :id;
