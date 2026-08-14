-- display_order uses gaps of 10 so statuses can be inserted later without
-- renumbering. It controls dropdown presentation order only and implies no
-- workflow ordering (MVP.md 5.2).
INSERT INTO statuses (id, code, name, display_order) VALUES
    (1,  'APPLIED',             'Applied',             10),
    (2,  'RECRUITER_VIEWED',    'Recruiter Viewed',    20),
    (3,  'HR_SCREENING',        'HR Screening',        30),
    (4,  'INTERVIEW',           'Interview',           40),
    (5,  'TECHNICAL_INTERVIEW', 'Technical Interview', 50),
    (6,  'FINAL_INTERVIEW',     'Final Interview',     60),
    (7,  'OFFER',               'Offer',               70),
    (8,  'ACCEPTED',            'Accepted',            80),
    (9,  'REJECTED',            'Rejected',            90),
    (10, 'RECONSIDERED',        'Reconsidered',       100),
    (11, 'WITHDRAWN',           'Withdrawn',          110);

-- The inserts above supply explicit ids, which does not advance the BIGSERIAL
-- sequence. Without this, the next id-less insert would collide on the primary
-- key.
SELECT setval(pg_get_serial_sequence('statuses', 'id'), (SELECT max(id) FROM statuses));
