CREATE TABLE match_results (
    seeker_id BIGINT NOT NULL,
    job_profile_id BIGINT NOT NULL,
    occupation_id BIGINT NOT NULL,
    company_name TEXT NOT NULL,
    position_name TEXT NOT NULL,
    job_description TEXT NOT NULL DEFAULT '',
    seeker_first_name TEXT NOT NULL,
    seeker_last_name TEXT NOT NULL,
    seeker_skills_json TEXT NOT NULL DEFAULT '[]',
    match_score DOUBLE PRECISION NOT NULL,
    match_score_display INT NOT NULL,
    personality_included BOOLEAN NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (seeker_id, job_profile_id)
);

CREATE INDEX idx_match_results_seeker ON match_results (seeker_id, match_score DESC);
CREATE INDEX idx_match_results_job ON match_results (job_profile_id, match_score DESC);
