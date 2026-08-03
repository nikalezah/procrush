-- Matching DB test seed (aligned with backend/platform/persistence db/seed/test_users.sql)
-- Scores only — display fields live in the main API DB.

INSERT INTO seeker_snapshots (
    seeker_id, desired_occupation_ids_json, skill_ids_json,
    personality_ready, personality_axes_json, matching_eligible, updated_at
) VALUES
    (1, '[4,6]', '[1,2,8,25,10,15]',
     true, '{"axisDominance":0.62,"axisInfluence":0.45,"axisStability":0.71,"axisIntegrity":0.83,"axisAutonomy":0.78,"axisPace":0.55}',
     true, NOW()),
    (2, '[5,6]', '[5,6,7,4,22,13]',
     true, '{"axisDominance":0.35,"axisInfluence":0.82,"axisStability":0.48,"axisIntegrity":0.55,"axisAutonomy":0.60,"axisPace":0.72}',
     true, NOW()),
    (3, '[11,12]', '[18,19,20,29,30]',
     true, '{"axisDominance":0.78,"axisInfluence":0.70,"axisStability":0.58,"axisIntegrity":0.65,"axisAutonomy":0.72,"axisPace":0.68}',
     true, NOW()),
    (4, '[8]', '[2,14,23,10,18]',
     true, '{"axisDominance":0.42,"axisInfluence":0.38,"axisStability":0.88,"axisIntegrity":0.90,"axisAutonomy":0.55,"axisPace":0.48}',
     true, NOW()),
    (5, '[7]', '[11,10,12,14,3]',
     true, '{"axisDominance":0.55,"axisInfluence":0.40,"axisStability":0.80,"axisIntegrity":0.88,"axisAutonomy":0.85,"axisPace":0.62}',
     true, NOW()),
    (6, '[14,15]', '[22,21,30]',
     true, '{"axisDominance":0.38,"axisInfluence":0.75,"axisStability":0.62,"axisIntegrity":0.58,"axisAutonomy":0.68,"axisPace":0.65}',
     true, NOW()),
    (7, '[9]', '[3,23,8,29,28]',
     true, '{"axisDominance":0.48,"axisInfluence":0.35,"axisStability":0.75,"axisIntegrity":0.92,"axisAutonomy":0.80,"axisPace":0.50}',
     true, NOW()),
    (8, '[13,11]', '[1,2,18,19,20,17]',
     true, '{"axisDominance":0.85,"axisInfluence":0.78,"axisStability":0.55,"axisIntegrity":0.60,"axisAutonomy":0.70,"axisPace":0.75}',
     true, NOW()),
    (9, '[4]', '[3,8,13]', false, NULL, false, NOW()),
    (10, '[9,11]', '[29,30]', false, NULL, false, NOW());

INSERT INTO job_profile_snapshots (
    job_profile_id, occupation_id, skill_ids_json, personality_axes_json, is_active, updated_at
) VALUES
    (1, 4, '[1,2,8,25,15,10]',
     '{"axisDominance":0.55,"axisInfluence":0.40,"axisStability":0.70,"axisIntegrity":0.85,"axisAutonomy":0.75,"axisPace":0.50}',
     true, NOW()),
    (2, 13, '[1,18,19,17,20]',
     '{"axisDominance":0.80,"axisInfluence":0.75,"axisStability":0.55,"axisIntegrity":0.60,"axisAutonomy":0.65,"axisPace":0.70}',
     true, NOW()),
    (3, 7, '[11,10,12,14,3]',
     '{"axisDominance":0.50,"axisInfluence":0.35,"axisStability":0.85,"axisIntegrity":0.90,"axisAutonomy":0.80,"axisPace":0.60}',
     true, NOW()),
    (4, 8, '[2,14,23,10,18]',
     '{"axisDominance":0.40,"axisInfluence":0.35,"axisStability":0.90,"axisIntegrity":0.90,"axisAutonomy":0.50,"axisPace":0.45}',
     true, NOW()),
    (5, 9, '[3,23,8,29,28]',
     '{"axisDominance":0.45,"axisInfluence":0.30,"axisStability":0.75,"axisIntegrity":0.92,"axisAutonomy":0.78,"axisPace":0.48}',
     true, NOW()),
    (6, 14, '[22,21]',
     '{"axisDominance":0.35,"axisInfluence":0.78,"axisStability":0.60,"axisIntegrity":0.55,"axisAutonomy":0.65,"axisPace":0.65}',
     true, NOW()),
    (7, 15, '[22,21,30]',
     '{"axisDominance":0.38,"axisInfluence":0.72,"axisStability":0.62,"axisIntegrity":0.58,"axisAutonomy":0.68,"axisPace":0.62}',
     true, NOW()),
    (8, 11, '[18,19,20,29]',
     '{"axisDominance":0.75,"axisInfluence":0.68,"axisStability":0.58,"axisIntegrity":0.62,"axisAutonomy":0.70,"axisPace":0.65}',
     true, NOW()),
    (9, 12, '[18,19,20]',
     '{"axisDominance":0.65,"axisInfluence":0.70,"axisStability":0.65,"axisIntegrity":0.60,"axisAutonomy":0.55,"axisPace":0.60}',
     true, NOW());

INSERT INTO match_results (
    seeker_id, job_profile_id, match_score, match_score_display, personality_included, computed_at
) VALUES
    (1, 1, 0.981, 98, true, NOW()),
    (4, 4, 0.956, 96, true, NOW()),
    (5, 3, 0.978, 98, true, NOW()),
    (6, 6, 0.923, 92, true, NOW()),
    (6, 7, 0.901, 90, true, NOW()),
    (7, 5, 0.967, 97, true, NOW()),
    (8, 2, 0.891, 89, true, NOW()),
    (3, 8, 0.891, 89, true, NOW()),
    (8, 8, 0.823, 82, true, NOW()),
    (3, 9, 0.758, 76, true, NOW());
