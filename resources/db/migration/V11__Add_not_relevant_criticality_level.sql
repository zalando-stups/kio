ALTER TABLE zk_data.application
  DROP CONSTRAINT application_a_criticality_level_check
, ADD  CONSTRAINT application_a_criticality_level_check CHECK (a_criticality_level >= 1 AND a_criticality_level <= 4);

