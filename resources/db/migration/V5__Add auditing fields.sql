-- application auditing
ALTER TABLE zk_data.application
  ADD COLUMN a_created TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE zk_data.application
  ADD COLUMN a_created_by TEXT NOT NULL;

ALTER TABLE zk_data.application
  ADD COLUMN a_last_modified TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE zk_data.application
  ADD COLUMN a_last_modified_by TEXT NOT NULL;


-- version auditing
ALTER TABLE zk_data.version
  ADD COLUMN v_created TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE zk_data.version
  ADD COLUMN v_created_by TEXT NOT NULL;

ALTER TABLE zk_data.version
  ADD COLUMN v_last_modified_by TEXT NOT NULL;

-- last_modified is already there with migration V2


-- fill with dummy data
UPDATE zk_data.application
   SET a_created = NOW(),
       a_created_by = 'kio-migration',
       a_last_modified = NOW(),
       a_last_modified_by = 'kio-migration';

UPDATE zk_data.version
   SET v_created = NOW(),
       v_created_by = 'kio-migration',
       v_last_modified = NOW(), -- this overwrites existing value, but better be consistent
       v_last_modified_by = 'kio-migration';
