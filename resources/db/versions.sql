-- name: read-versions-by-application
SELECT v_id,
       v_application_id,
       v_artifact
  FROM zk_data.version
 WHERE v_application_id = :application_id;

--name: read-version-by-application
SELECT v_id,
       v_application_id,
       v_artifact,
       v_notes,
       v_created,
       v_created_by,
       v_last_modified,
       v_last_modified_by
  FROM zk_data.version
 WHERE v_id = :id
   AND v_application_id = :application_id;

-- name: create-or-update-version!
WITH version_update AS (
     UPDATE zk_data.version
        SET v_artifact           = :artifact,
            v_notes              = :notes,
            v_last_modified      = NOW(),
            v_last_modified_by   = :last_modified_by,
            v_created_by         = :created_by
      WHERE v_id = :id AND v_application_id = :application_id
  RETURNING *)
INSERT INTO zk_data.version (
            v_id,
            v_application_id,
            v_artifact,
            v_notes,
            v_created_by,
            v_last_modified_by)
     SELECT :id,
            :application_id,
            :artifact,
            :notes,
            :created_by,
            :last_modified_by
      WHERE NOT EXISTS (SELECT * FROM version_update);
