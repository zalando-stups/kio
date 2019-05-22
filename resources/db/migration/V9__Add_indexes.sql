-- adds indexes

CREATE INDEX application_a_last_modified_idx ON zk_data.application(a_last_modified);
CREATE INDEX application_a_active_idx ON zk_data.application(a_active);
CREATE INDEX application_a_team_id_idx ON zk_data.application(a_team_id);
