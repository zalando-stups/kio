-- adds a new column, without locking the table.

-- 1) create the new field (w/o default and nullable)

ALTER TABLE zk_data.application
 ADD COLUMN a_publicly_accessible BOOLEAN;

-- 2) set default value
ALTER TABLE zk_data.application
    ALTER COLUMN a_publicly_accessible SET DEFAULT false;

-- 3) migrate missing values

UPDATE zk_data.application
   SET a_publicly_accessible = false
 WHERE a_publicly_accessible IS NULL;

-- 4) set new column NOT NULL

 ALTER TABLE zk_data.application
ALTER COLUMN a_publicly_accessible SET NOT NULL;
