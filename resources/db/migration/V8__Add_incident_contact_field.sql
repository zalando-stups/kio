-- adds a new column, without locking the table.

ALTER TABLE zk_data.application
 ADD COLUMN a_incident_contact TEXT;
