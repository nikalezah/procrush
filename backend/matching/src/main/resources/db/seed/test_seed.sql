-- Matching DB test seed (aligned with backend/platform/persistence db/seed/test_users.sql)
-- Main DB test_users.sql uses explicit ids: employers 1–5, seekers 1–10, job profiles 1–9.
-- Occupation leaf ids 4–15, skill ids 1–30 (see init_inserts.sql).
--
-- Occupation id map (leaf, from init_inserts.sql insert order):
--   4 Backend, 5 Frontend, 6 Fullstack, 7 DevOps, 8 QA, 9 Data Engineer,
--   11 Product Manager, 12 Project Manager, 13 Team Lead, 14 UX/UI, 15 Product Designer

-- =============================================================================
-- Seeker snapshots (seekers 1–8: matching_eligible, seekers 9–10: not yet eligible)
-- =============================================================================

INSERT INTO seeker_snapshots (
    seeker_id, desired_occupation_ids_json, skill_ids_json, skill_names_json,
    personality_ready, personality_axes_json, matching_eligible,
    first_name, last_name, updated_at
) VALUES
    (1, '[4,6]', '[1,2,8,25,10,15]', '["Kotlin","Java","PostgreSQL","Spring Boot","Docker","REST API"]',
     true, '{"axisDominance":0.62,"axisInfluence":0.45,"axisStability":0.71,"axisIntegrity":0.83,"axisAutonomy":0.78,"axisPace":0.55}',
     true, '1-Иван', 'Петров', NOW()),
    (2, '[5,6]', '[5,6,7,4,22,13]', '["TypeScript","React","Vue.js","JavaScript","Figma","Git"]',
     true, '{"axisDominance":0.35,"axisInfluence":0.82,"axisStability":0.48,"axisIntegrity":0.55,"axisAutonomy":0.60,"axisPace":0.72}',
     true, '2-Мария', 'Сидорова', NOW()),
    (3, '[11,12]', '[18,19,20,29,30]', '["Agile","Scrum","Project Management","Data Analysis","Technical Writing"]',
     true, '{"axisDominance":0.78,"axisInfluence":0.70,"axisStability":0.58,"axisIntegrity":0.65,"axisAutonomy":0.72,"axisPace":0.68}',
     true, '3-Алексей', 'Козлов', NOW()),
    (4, '[8]', '[2,14,23,10,18]', '["Java","CI/CD","SQL","Docker","Agile"]',
     true, '{"axisDominance":0.42,"axisInfluence":0.38,"axisStability":0.88,"axisIntegrity":0.90,"axisAutonomy":0.55,"axisPace":0.48}',
     true, '4-Елена', 'Волкова', NOW()),
    (5, '[7]', '[11,10,12,14,3]', '["Kubernetes","Docker","AWS","CI/CD","Python"]',
     true, '{"axisDominance":0.55,"axisInfluence":0.40,"axisStability":0.80,"axisIntegrity":0.88,"axisAutonomy":0.85,"axisPace":0.62}',
     true, '5-Дмитрий', 'Орлов', NOW()),
    (6, '[14,15]', '[22,21,30]', '["Figma","UI/UX Design","Technical Writing"]',
     true, '{"axisDominance":0.38,"axisInfluence":0.75,"axisStability":0.62,"axisIntegrity":0.58,"axisAutonomy":0.68,"axisPace":0.65}',
     true, '6-Анна', 'Морозова', NOW()),
    (7, '[9]', '[3,23,8,29,28]', '["Python","SQL","PostgreSQL","Data Analysis","Machine Learning"]',
     true, '{"axisDominance":0.48,"axisInfluence":0.35,"axisStability":0.75,"axisIntegrity":0.92,"axisAutonomy":0.80,"axisPace":0.50}',
     true, '7-Сергей', 'Лебедев', NOW()),
    (8, '[13,11]', '[1,2,18,19,20,17]', '["Kotlin","Java","Agile","Scrum","Project Management","Microservices"]',
     true, '{"axisDominance":0.85,"axisInfluence":0.78,"axisStability":0.55,"axisIntegrity":0.60,"axisAutonomy":0.70,"axisPace":0.75}',
     true, '8-Ольга', 'Соколова', NOW()),
    (9, '[4]', '[3,8,13]', '["Python","PostgreSQL","Git"]',
     false, NULL, false, '9-Никита', 'Фролов', NOW()),
    (10, '[9,11]', '[29,30]', '["Data Analysis","Technical Writing"]',
     false, NULL, false, '10-Виктория', 'Романова', NOW());

-- =============================================================================
-- Job profile snapshots (9 active vacancies from test_users.sql)
-- =============================================================================

INSERT INTO job_profile_snapshots (
    job_profile_id, occupation_id, skill_ids_json, personality_axes_json,
    is_active, company_name, occupation_name, description, updated_at
) VALUES
    (1, 4, '[1,2,8,25,15,10]',
     '{"axisDominance":0.55,"axisInfluence":0.40,"axisStability":0.70,"axisIntegrity":0.85,"axisAutonomy":0.75,"axisPace":0.50}',
     true, '1-ТехНова', 'Backend-разработчик',
     'Разработка микросервисов на Kotlin/Spring Boot. Команда 6 человек, гибрид, code review и менторинг.', NOW()),
    (2, 13, '[1,18,19,17,20]',
     '{"axisDominance":0.80,"axisInfluence":0.75,"axisStability":0.55,"axisIntegrity":0.60,"axisAutonomy":0.65,"axisPace":0.70}',
     true, '1-ТехНова', 'Team Lead',
     'Техническое лидерство backend-команды, архитектура, найм и развитие инженеров.', NOW()),
    (3, 7, '[11,10,12,14,3]',
     '{"axisDominance":0.50,"axisInfluence":0.35,"axisStability":0.85,"axisIntegrity":0.90,"axisAutonomy":0.80,"axisPace":0.60}',
     true, '2-КлаудБридж', 'DevOps-инженер',
     'Поддержка Kubernetes-кластеров, CI/CD, миграции в AWS, on-call по графику.', NOW()),
    (4, 8, '[2,14,23,10,18]',
     '{"axisDominance":0.40,"axisInfluence":0.35,"axisStability":0.90,"axisIntegrity":0.90,"axisAutonomy":0.50,"axisPace":0.45}',
     true, '2-КлаудБридж', 'QA-инженер',
     'Автотесты, тестовая стратегия, интеграция в CI/CD. Фокус на стабильность релизов.', NOW()),
    (5, 9, '[3,23,8,29,28]',
     '{"axisDominance":0.45,"axisInfluence":0.30,"axisStability":0.75,"axisIntegrity":0.92,"axisAutonomy":0.78,"axisPace":0.48}',
     true, '3-ДатаПульс', 'Data Engineer',
     'Построение ETL-пайплайнов, DWH, Spark/Airflow. Работа с аналитиками и ML-командой.', NOW()),
    (6, 14, '[22,21]',
     '{"axisDominance":0.35,"axisInfluence":0.78,"axisStability":0.60,"axisIntegrity":0.55,"axisAutonomy":0.65,"axisPace":0.65}',
     true, '4-ДизайнМинт', 'UX/UI дизайнер',
     'Мобильные и веб-интерфейсы, user research, дизайн-система, тесная работа с разработкой.', NOW()),
    (7, 15, '[22,21,30]',
     '{"axisDominance":0.38,"axisInfluence":0.72,"axisStability":0.62,"axisIntegrity":0.58,"axisAutonomy":0.68,"axisPace":0.62}',
     true, '4-ДизайнМинт', 'Product Designer',
     'End-to-end дизайн продукта: от исследований до handoff, A/B-тесты, дизайн-системы.', NOW()),
    (8, 11, '[18,19,20,29]',
     '{"axisDominance":0.75,"axisInfluence":0.68,"axisStability":0.58,"axisIntegrity":0.62,"axisAutonomy":0.70,"axisPace":0.65}',
     true, '5-АджайлВоркс', 'Product Manager',
     'B2B SaaS, roadmap, метрики, работа с 2 командами разработки и стейкхолдерами.', NOW()),
    (9, 12, '[18,19,20]',
     '{"axisDominance":0.65,"axisInfluence":0.70,"axisStability":0.65,"axisIntegrity":0.60,"axisAutonomy":0.55,"axisPace":0.60}',
     true, '5-АджайлВоркс', 'Project Manager',
     'Agile-трансформация клиентов, управление сроками и рисками, фасилитация команд.', NOW());

-- =============================================================================
-- Match results (symmetric: seeker recommendations ↔ employer candidates per job_profile_id)
-- Rule: job.occupation_id ∈ seeker.desired_occupation_ids AND matching_eligible = true
--
-- Bidirectional map (seeker_id ↔ job_profile_id), aligned with test_users.sql:
--   1 ↔ 1   1-Иван Петров          ↔ 1-ТехНова Backend
--   4 ↔ 4   4-Елена Волкова        ↔ 2-КлаудБридж QA
--   5 ↔ 3   5-Дмитрий Орлов        ↔ 2-КлаудБридж DevOps
--   6 ↔ 6   6-Анна Морозова        ↔ 4-ДизайнМинт UX/UI
--   6 ↔ 7   6-Анна Морозова        ↔ 4-ДизайнМинт Product Designer
--   7 ↔ 5   7-Сергей Лебедев       ↔ 3-ДатаПульс Data Engineer
--   8 ↔ 2   8-Ольга Соколова       ↔ 1-ТехНова Team Lead
--   3 ↔ 8   3-Алексей Козлов       ↔ 5-АджайлВоркс Product Manager
--   3 ↔ 9   3-Алексей Козлов       ↔ 5-АджайлВоркс Project Manager
--   8 ↔ 8   8-Ольга Соколова       ↔ 5-АджайлВоркс Product Manager
-- =============================================================================

INSERT INTO match_results (
    seeker_id, job_profile_id, occupation_id, company_name, position_name, job_description,
    seeker_first_name, seeker_last_name, seeker_skills_json,
    match_score, match_score_display, personality_included, computed_at
) VALUES
    (1, 1, 4, '1-ТехНова', 'Backend-разработчик',
     'Разработка микросервисов на Kotlin/Spring Boot. Команда 6 человек, гибрид, code review и менторинг.',
     '1-Иван', 'Петров', '["Kotlin","Java","PostgreSQL","Spring Boot","Docker","REST API"]',
     0.981, 98, true, NOW()),
    (4, 4, 8, '2-КлаудБридж', 'QA-инженер',
     'Автотесты, тестовая стратегия, интеграция в CI/CD. Фокус на стабильность релизов.',
     '4-Елена', 'Волкова', '["Java","CI/CD","SQL","Docker","Agile"]',
     0.956, 96, true, NOW()),
    (5, 3, 7, '2-КлаудБридж', 'DevOps-инженер',
     'Поддержка Kubernetes-кластеров, CI/CD, миграции в AWS, on-call по графику.',
     '5-Дмитрий', 'Орлов', '["Kubernetes","Docker","AWS","CI/CD","Python"]',
     0.978, 98, true, NOW()),
    (6, 6, 14, '4-ДизайнМинт', 'UX/UI дизайнер',
     'Мобильные и веб-интерфейсы, user research, дизайн-система, тесная работа с разработкой.',
     '6-Анна', 'Морозова', '["Figma","UI/UX Design","Technical Writing"]',
     0.923, 92, true, NOW()),
    (6, 7, 15, '4-ДизайнМинт', 'Product Designer',
     'End-to-end дизайн продукта: от исследований до handoff, A/B-тесты, дизайн-системы.',
     '6-Анна', 'Морозова', '["Figma","UI/UX Design","Technical Writing"]',
     0.901, 90, true, NOW()),
    (7, 5, 9, '3-ДатаПульс', 'Data Engineer',
     'Построение ETL-пайплайнов, DWH, Spark/Airflow. Работа с аналитиками и ML-командой.',
     '7-Сергей', 'Лебедев', '["Python","SQL","PostgreSQL","Data Analysis","Machine Learning"]',
     0.967, 97, true, NOW()),
    (8, 2, 13, '1-ТехНова', 'Team Lead',
     'Техническое лидерство backend-команды, архитектура, найм и развитие инженеров.',
     '8-Ольга', 'Соколова', '["Kotlin","Java","Agile","Scrum","Project Management","Microservices"]',
     0.891, 89, true, NOW()),
    (3, 8, 11, '5-АджайлВоркс', 'Product Manager',
     'B2B SaaS, roadmap, метрики, работа с 2 командами разработки и стейкхолдерами.',
     '3-Алексей', 'Козлов', '["Agile","Scrum","Project Management","Data Analysis","Technical Writing"]',
     0.891, 89, true, NOW()),
    (8, 8, 11, '5-АджайлВоркс', 'Product Manager',
     'B2B SaaS, roadmap, метрики, работа с 2 командами разработки и стейкхолдерами.',
     '8-Ольга', 'Соколова', '["Kotlin","Java","Agile","Scrum","Project Management","Microservices"]',
     0.823, 82, true, NOW()),
    (3, 9, 12, '5-АджайлВоркс', 'Project Manager',
     'Agile-трансформация клиентов, управление сроками и рисками, фасилитация команд.',
     '3-Алексей', 'Козлов', '["Agile","Scrum","Project Management","Data Analysis","Technical Writing"]',
     0.758, 76, true, NOW());
