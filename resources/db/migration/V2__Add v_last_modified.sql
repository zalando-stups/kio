ALTER TABLE version
-- when the version was created or last modified
 ADD COLUMN v_last_modified TIMESTAMP NOT NULL DEFAULT NOW();