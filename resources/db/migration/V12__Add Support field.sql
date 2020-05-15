-- adds a new column for storing contact to support for the application

ALTER TABLE zk_data.application
 ADD COLUMN a_support_url TEXT;
