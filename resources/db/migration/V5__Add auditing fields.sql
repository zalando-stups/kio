-- application auditing
ALTER TABLE zk_data.application
ADD COLUMN a_created TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE zk_data.application
ADD COLUMN a_created_by TEXT;

ALTER TABLE zk_data.application
ADD COLUMN a_last_modified TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE zk_data.application
ADD COLUMN a_last_modified_by TEXT;


-- version auditing
ALTER TABLE zk_data.version
ADD_COLUMN v_created TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE zk_data.version
ADD_COLUMN v_created_by TEXT;

ALTER TABLE zk_data.version
ADD_COLUMN v_last_modified_by TEXT;

-- last_modified is already there with migration V2
