-- name: read-versions-by-application
SELECT id, application_id, artifact
  FROM version
 WHERE application_id = :application_id;

--name: read-version-by-application
SELECT id, application_id, artifact, notes
  FROM version
 WHERE id = :id
   AND application_id = :application_id;

-- name: create-or-update-version!
WITH version_update AS (
     UPDATE version
        SET artifact           = :artifact,
            notes              = :notes
      WHERE id = :id AND application_id = :application_id
  RETURNING *)
INSERT INTO version
            (id, application_id, artifact, notes)
     SELECT :id, :application_id, :artifact, :notes
      WHERE NOT EXISTS (SELECT * FROM version_update);
