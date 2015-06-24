ALTER TABLE zk_data.version
-- how many approvers a version needs
ADD COLUMN a_required_approvers SMALLINT DEFAULT 2 CHECK (a_required_approvers > 0);
