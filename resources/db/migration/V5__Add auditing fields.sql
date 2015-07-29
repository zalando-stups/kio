-- 1) add columns

-- application auditing
ALTER TABLE zk_data.application
 ADD COLUMN a_created TIMESTAMP DEFAULT NOW(),
 ADD COLUMN a_created_by TEXT,
 ADD COLUMN a_last_modified TIMESTAMP DEFAULT NOW(),
 ADD COLUMN a_last_modified_by TEXT;

-- version auditing
ALTER TABLE zk_data.version
 ADD COLUMN v_created TIMESTAMP DEFAULT NOW(),
 ADD COLUMN v_created_by TEXT,
 ADD COLUMN v_last_modified_by TEXT;
-- last_modified is already there with migration V2


-- 2) fill with data
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

-- 3) set columns NOT NULL
 ALTER TABLE zk_data.application
ALTER COLUMN a_created SET NOT NULL,
ALTER COLUMN a_created_by SET NOT NULL,
ALTER COLUMN a_last_modified SET NOT NULL,
ALTER COLUMN a_last_modified_by SET NOT NULL;

 ALTER TABLE zk_data.version
ALTER COLUMN v_created SET NOT NULL,
ALTER COLUMN v_created_by SET NOT NULL,
ALTER COLUMN v_last_modified_by SET NOT NULL;
