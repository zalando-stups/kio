-- 1) create new field a_criticality_level

ALTER TABLE zk_data.application
 ADD COLUMN a_criticality_level SMALLINT DEFAULT 2 CHECK (a_criticality_level >= 1 AND
                                                          a_criticality_level <= 3);

-- 2) fill it based on a_required_approvers

UPDATE zk_data.application
   SET a_criticality_level = 1
 WHERE a_required_approvers = 1;

 UPDATE zk_data.application
    SET a_criticality_level = 2
  WHERE a_required_approvers > 1;

-- 3) set new column NOT NULL

 ALTER TABLE zk_data.application
ALTER COLUMN a_criticality_level SET NOT NULL;

-- TODO in future migration: DROP COLUMN a_required_approvers
