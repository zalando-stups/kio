
ALTER TABLE zk_data.application
ALTER COLUMN a_criticality_level DROP NOT NULL;

ALTER TABLE zk_data.application ALTER COLUMN a_criticality_level SET DEFAULT NULL;
