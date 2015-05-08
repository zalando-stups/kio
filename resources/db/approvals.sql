-- name: read-approvals-by-version
SELECT ap_application_id, ap_version_id, ap_approval_type, ap_user_id, ap_approved_at, ap_notes
  FROM zk_data.approval
 WHERE ap_application_id = :application_id
   AND ap_version_id = :version_id;

-- name: approve-version!
INSERT INTO zk_data.approval (ap_application_id, ap_version_id, ap_user_id, ap_approval_type, ap_notes)
     VALUES (:application_id, :version_id, :user_id, :approval_type, :notes);

-- name: read-application-approvals
SELECT ap_approval_type
  FROM zk_data.approval
 WHERE ap_application_id = :application_id
 GROUP BY ap_approval_type;
