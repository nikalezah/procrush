CREATE TABLE seeker_snapshots (
    seeker_id BIGINT PRIMARY KEY,
    desired_occupation_ids_json TEXT NOT NULL DEFAULT '[]',
    skill_ids_json TEXT NOT NULL DEFAULT '[]',
    skill_names_json TEXT NOT NULL DEFAULT '[]',
    personality_ready BOOLEAN NOT NULL DEFAULT FALSE,
    personality_axes_json TEXT,
    matching_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    first_name TEXT NOT NULL DEFAULT '',
    last_name TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_seeker_snapshots_matching_eligible ON seeker_snapshots (matching_eligible);

CREATE TABLE job_profile_snapshots (
    job_profile_id BIGINT PRIMARY KEY,
    occupation_id BIGINT NOT NULL,
    skill_ids_json TEXT NOT NULL DEFAULT '[]',
    personality_axes_json TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    company_name TEXT NOT NULL,
    occupation_name TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_job_profile_snapshots_occupation ON job_profile_snapshots (occupation_id);
CREATE INDEX idx_job_profile_snapshots_active ON job_profile_snapshots (is_active);
