-- Every application query filters on user_id.
CREATE INDEX idx_applications_user_id ON applications (user_id);

-- Dashboard status breakdown and the status filter.
CREATE INDEX idx_applications_user_status ON applications (user_id, status_id);

-- Default list sort (date_applied DESC).
CREATE INDEX idx_applications_user_date ON applications (user_id, date_applied DESC);

-- Matches the timeline ordering (event_date ASC, id ASC) exactly.
CREATE INDEX idx_progress_application ON application_progress (application_id, event_date, id);
