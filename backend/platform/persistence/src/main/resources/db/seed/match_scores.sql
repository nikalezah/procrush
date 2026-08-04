-- Seed match scores aligned with matching test_seed.sql / test_users.sql
INSERT INTO match_scores (
    seeker_id, job_profile_id, match_score, personality_included, computed_at
) VALUES
    (1, 1, 0.981, true, NOW()),
    (4, 4, 0.956, true, NOW()),
    (5, 3, 0.978, true, NOW()),
    (6, 6, 0.923, true, NOW()),
    (6, 7, 0.901, true, NOW()),
    (7, 5, 0.967, true, NOW()),
    (8, 2, 0.891, true, NOW()),
    (3, 8, 0.891, true, NOW()),
    (8, 8, 0.823, true, NOW()),
    (3, 9, 0.758, true, NOW())
ON CONFLICT DO NOTHING;
