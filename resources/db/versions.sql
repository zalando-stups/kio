-- name: read-versions-by-application
SELECT v_id, v_application_id, v_artifact
  FROM zk_data.version
 WHERE v_application_id = :application_id;

--name: read-version-by-application
SELECT v_id, v_application_id, v_artifact, v_notes
  FROM zk_data.version
 WHERE v_id = :id
   AND v_application_id = :application_id;

-- name: create-or-update-version!
WITH version_update AS (
     UPDATE zk_data.version
        SET v_artifact           = :artifact,
            v_notes              = :notes
      WHERE v_id = :id AND v_application_id = :application_id
  RETURNING *)
INSERT INTO zk_data.version
            (v_id, v_application_id, v_artifact, v_notes)
     SELECT :id, :application_id, :artifact, :notes
      WHERE NOT EXISTS (SELECT * FROM version_update);
