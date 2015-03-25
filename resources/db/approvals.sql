-- name: read-approvals-by-version
SELECT application_id, version_id, approval_type, user_id, approved_at
  FROM approval
 WHERE application_id = :application_id
   AND version_id = :version_id;

-- name: approve-version!
INSERT INTO approval (application_id, version_id, user_id, approval_type)
    VALUES (:application_id, :version_id, :user_id, :approval_type);
