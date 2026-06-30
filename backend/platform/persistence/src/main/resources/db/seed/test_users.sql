-- Test users seed (depends on init_inserts.sql)
-- Dev login: seekerN@procrush.test / employerN@procrush.test (provider: dev)
--
-- Stable numeric ids (aligned with backend/matching db/seed/test_seed.sql):
--   employers 1–5, seekers 1–10, job profiles 1–9
--   occupation leaf ids 4–15 (see init_inserts.sql insert order)

-- =============================================================================
-- Users
-- =============================================================================

INSERT INTO users (id, email, role, oauth_provider, oauth_subject, created_at) VALUES
    ('10000000-0000-4000-8000-000000000001', 'employer1@procrush.test', 'EMPLOYER', 'dev', 'employer1@procrush.test', NOW()),
    ('10000000-0000-4000-8000-000000000002', 'employer2@procrush.test', 'EMPLOYER', 'dev', 'employer2@procrush.test', NOW()),
    ('10000000-0000-4000-8000-000000000003', 'employer3@procrush.test', 'EMPLOYER', 'dev', 'employer3@procrush.test', NOW()),
    ('10000000-0000-4000-8000-000000000004', 'employer4@procrush.test', 'EMPLOYER', 'dev', 'employer4@procrush.test', NOW()),
    ('10000000-0000-4000-8000-000000000005', 'employer5@procrush.test', 'EMPLOYER', 'dev', 'employer5@procrush.test', NOW()),
    ('20000000-0000-4000-8000-000000000001', 'seeker1@procrush.test',  'SEEKER',   'dev', 'seeker1@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000002', 'seeker2@procrush.test',  'SEEKER',   'dev', 'seeker2@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000003', 'seeker3@procrush.test',  'SEEKER',   'dev', 'seeker3@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000004', 'seeker4@procrush.test',  'SEEKER',   'dev', 'seeker4@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000005', 'seeker5@procrush.test',  'SEEKER',   'dev', 'seeker5@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000006', 'seeker6@procrush.test',  'SEEKER',   'dev', 'seeker6@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000007', 'seeker7@procrush.test',  'SEEKER',   'dev', 'seeker7@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000008', 'seeker8@procrush.test',  'SEEKER',   'dev', 'seeker8@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000009', 'seeker9@procrush.test',  'SEEKER',   'dev', 'seeker9@procrush.test',  NOW()),
    ('20000000-0000-4000-8000-000000000010', 'seeker10@procrush.test', 'SEEKER',   'dev', 'seeker10@procrush.test', NOW());

-- =============================================================================
-- Employers
-- =============================================================================

INSERT INTO employers (id, user_id, name, description, website, phone, email_contact, updated_at) VALUES
    (1, '10000000-0000-4000-8000-000000000001', '1-ТехНова',
     'Продуктовая IT-компания, разрабатывает B2B SaaS для логистики. Команда 120 человек, гибридный формат.',
     'https://technova.example.com', '+7 (495) 111-22-33', 'hr@technova.example.com', NOW()),
    (2, '10000000-0000-4000-8000-000000000002', '2-КлаудБридж',
     'Облачный интегратор и DevOps-консалтинг. Специализация — миграция в AWS и Kubernetes.',
     'https://cloudbridge.example.com', '+7 (812) 222-33-44', 'jobs@cloudbridge.example.com', NOW()),
    (3, '10000000-0000-4000-8000-000000000003', '3-ДатаПульс',
     'Аналитическая платформа для ритейла. Big Data, ML-рекомендации, дата-инжиниринг.',
     'https://datapulse.example.com', '+7 (343) 333-44-55', 'talent@datapulse.example.com', NOW()),
    (4, '10000000-0000-4000-8000-000000000004', '4-ДизайнМинт',
     'UX/UI-студия полного цикла: исследования, прототипирование, дизайн-системы для финтеха.',
     'https://designmint.example.com', '+7 (495) 444-55-66', 'hello@designmint.example.com', NOW()),
    (5, '10000000-0000-4000-8000-000000000005', '5-АджайлВоркс',
     'Аутсорс-команда с фокусом на Agile-трансформацию и управление продуктами.',
     'https://agileworks.example.com', '+7 (383) 555-66-77', 'careers@agileworks.example.com', NOW());

-- =============================================================================
-- Seekers
-- =============================================================================

INSERT INTO seekers (id, user_id, first_name, middle_name, last_name, phone, telegram, linkedin, updated_at) VALUES
    (1, '20000000-0000-4000-8000-000000000001', '1-Иван',     'Сергеевич',  'Петров',    '+7 (916) 100-01-01', '@ipetrov',    'https://linkedin.com/in/ipetrov',    NOW()),
    (2, '20000000-0000-4000-8000-000000000002', '2-Мария',    'Александровна', 'Сидорова', '+7 (903) 100-02-02', '@msidorova',  'https://linkedin.com/in/msidorova',  NOW()),
    (3, '20000000-0000-4000-8000-000000000003', '3-Алексей',  'Дмитриевич', 'Козлов',   '+7 (926) 100-03-03', '@akozlov',    'https://linkedin.com/in/akozlov',    NOW()),
    (4, '20000000-0000-4000-8000-000000000004', '4-Елена',    'Игоревна',   'Волкова',   '+7 (977) 100-04-04', '@evolkova',   'https://linkedin.com/in/evolkova',   NOW()),
    (5, '20000000-0000-4000-8000-000000000005', '5-Дмитрий',  'Павлович',   'Орлов',     '+7 (915) 100-05-05', '@dorlov',     'https://linkedin.com/in/dorlov',     NOW()),
    (6, '20000000-0000-4000-8000-000000000006', '6-Анна',     'Викторовна', 'Морозова',  '+7 (981) 100-06-06', '@amorozova',  'https://linkedin.com/in/amorozova',  NOW()),
    (7, '20000000-0000-4000-8000-000000000007', '7-Сергей',   'Николаевич', 'Лебедев',   '+7 (905) 100-07-07', '@slebedev',   'https://linkedin.com/in/slebedev',   NOW()),
    (8, '20000000-0000-4000-8000-000000000008', '8-Ольга',    'Андреевна',  'Соколова',  '+7 (967) 100-08-08', '@osokolova',  'https://linkedin.com/in/osokolova',  NOW()),
    (9, '20000000-0000-4000-8000-000000000009', '9-Никита',   NULL,         'Фролов',    '+7 (999) 100-09-09', '@nfrolov',    NULL,                                 NOW()),
    (10, '20000000-0000-4000-8000-000000000010', '10-Виктория', 'Олеговна',   'Романова',  NULL,                 NULL,          'https://linkedin.com/in/vromanova',  NOW());

-- =============================================================================
-- Seeker experience
-- =============================================================================

INSERT INTO seeker_experience (seeker_id, company_name, position, description, start_date, end_date, created_at, updated_at)
SELECT s.id, v.company_name, v.position, v.description, v.start_date::date, v.end_date::date, NOW(), NOW()
FROM (VALUES
    ('seeker1@procrush.test',  'Яндекс',           'Backend-разработчик',  'Разработка микросервисов на Kotlin/Spring Boot, PostgreSQL, Kafka.', '2019-03-01', '2022-08-31'),
    ('seeker1@procrush.test',  'ТехНова',          'Senior Backend',       'Архитектура API, code review, менторинг junior-разработчиков.',    '2022-09-01', NULL),
    ('seeker2@procrush.test',  'VK',               'Frontend-разработчик', 'React, TypeScript, дизайн-система, A/B-тесты интерфейсов.',      '2020-06-01', '2024-01-31'),
    ('seeker2@procrush.test',  'ДизайнМинт',       'Lead Frontend',        'Ведение фронтенд-команды, Vue.js и React, Storybook.',             '2024-02-01', NULL),
    ('seeker3@procrush.test',  'Сбер',             'Product Manager',      'B2C-продукт, roadmap, метрики, работа с 3 командами разработки.', '2017-01-01', '2021-12-31'),
    ('seeker3@procrush.test',  'АджайлВоркс',      'Senior PM',            'Запуск новых направлений, CustDev, приоритизация бэклога.',        '2022-03-01', NULL),
    ('seeker4@procrush.test',  'Лаборатория Касперского', 'QA-инженер', 'Автотесты на Java, CI/CD, нагрузочное тестирование.',              '2018-09-01', '2023-05-31'),
    ('seeker4@procrush.test',  'КлаудБридж',       'QA Lead',              'Построение QA-процессов, Playwright, тестовая стратегия.',         '2023-06-01', NULL),
    ('seeker5@procrush.test',  'Ozon',             'DevOps-инженер',       'Kubernetes, Terraform, мониторинг Grafana/Prometheus.',            '2019-11-01', '2023-12-31'),
    ('seeker5@procrush.test',  'КлаудБридж',       'Senior DevOps',        'Миграции в AWS, GitOps, оптимизация инфраструктуры.',              '2024-01-01', NULL),
    ('seeker6@procrush.test',  'Тинькофф',         'UX/UI дизайнер',       'Мобильные приложения, user research, Figma, прототипы.',            '2020-02-01', '2024-06-30'),
    ('seeker6@procrush.test',  'ДизайнМинт',       'Product Designer',     'Дизайн-системы, usability-тесты, handoff разработчикам.',          '2024-07-01', NULL),
    ('seeker7@procrush.test',  'ДатаПульс',        'Data Engineer',        'ETL-пайплайны, Spark, Airflow, хранилище данных.',                 '2018-04-01', '2022-10-31'),
    ('seeker7@procrush.test',  'ДатаПульс',        'Lead Data Engineer',   'Архитектура DWH, менторинг, оптимизация запросов.',                '2022-11-01', NULL),
    ('seeker8@procrush.test',  'Авито',            'Team Lead',            'Команда 8 человек, Kotlin backend, планирование спринтов.',        '2016-05-01', '2020-12-31'),
    ('seeker8@procrush.test',  'ТехНова',          'Engineering Manager',  '2 продуктовые команды, найм, performance review.',                 '2021-01-01', NULL),
    ('seeker9@procrush.test',  'Стартап «Кодик»',  'Junior Backend',       'REST API на Python, первый коммерческий опыт.',                    '2024-09-01', NULL),
    ('seeker10@procrush.test', 'МГУ',              'Лаборант',             'Подготовка данных для исследований, Python, Excel.',               '2022-09-01', '2024-06-30')
) AS v(email, company_name, position, description, start_date, end_date)
JOIN seekers s ON s.user_id = (SELECT id FROM users WHERE email = v.email);

-- =============================================================================
-- Seeker education
-- =============================================================================

INSERT INTO seeker_education (seeker_id, institution, degree, specialization, end_year, created_at, updated_at)
SELECT s.id, v.institution, v.degree, v.specialization, v.end_year, NOW(), NOW()
FROM (VALUES
    ('seeker1@procrush.test',  'МФТИ',                    'Магистр',  'Прикладная математика и информатика', 2018),
    ('seeker2@procrush.test',  'ИТМО',                    'Бакалавр', 'Программная инженерия',              2020),
    ('seeker3@procrush.test',  'ВШЭ',                     'Магистр',  'Управление продуктом',              2016),
    ('seeker4@procrush.test',  'УрФУ',                    'Бакалавр', 'Информационные системы',            2018),
    ('seeker5@procrush.test',  'НИУ ВШЭ',                 'Бакалавр', 'Прикладная информатика',            2019),
    ('seeker6@procrush.test',  'Британская высшая школа дизайна', 'Бакалавр', 'Графический дизайн',     2019),
    ('seeker7@procrush.test',  'МГУ',                     'Магистр',  'Механика и математическое моделирование', 2017),
    ('seeker8@procrush.test',  'СПбГУ',                   'Магистр',  'Программная инженерия',             2015),
    ('seeker9@procrush.test',  'МИФИ',                    'Бакалавр', 'Прикладная информатика',            2024),
    ('seeker10@procrush.test', 'МГУ',                     'Бакалавр', 'Социология',                        2024)
) AS v(email, institution, degree, specialization, end_year)
JOIN seekers s ON s.user_id = (SELECT id FROM users WHERE email = v.email);

-- =============================================================================
-- Seeker skills
-- =============================================================================

INSERT INTO seeker_skills (seeker_id, skill_id, created_at, updated_at)
SELECT s.id, sk.id, NOW(), NOW()
FROM (VALUES
    ('seeker1@procrush.test',  'Kotlin'), ('seeker1@procrush.test',  'Java'), ('seeker1@procrush.test',  'PostgreSQL'),
    ('seeker1@procrush.test',  'Spring Boot'), ('seeker1@procrush.test', 'Docker'), ('seeker1@procrush.test', 'REST API'),
    ('seeker2@procrush.test',  'TypeScript'), ('seeker2@procrush.test', 'React'), ('seeker2@procrush.test', 'Vue.js'),
    ('seeker2@procrush.test',  'JavaScript'), ('seeker2@procrush.test', 'Figma'), ('seeker2@procrush.test', 'Git'),
    ('seeker3@procrush.test',  'Agile'), ('seeker3@procrush.test', 'Scrum'), ('seeker3@procrush.test', 'Project Management'),
    ('seeker3@procrush.test',  'Data Analysis'), ('seeker3@procrush.test', 'Technical Writing'),
    ('seeker4@procrush.test',  'Java'), ('seeker4@procrush.test', 'CI/CD'), ('seeker4@procrush.test', 'SQL'),
    ('seeker4@procrush.test',  'Docker'), ('seeker4@procrush.test', 'Agile'),
    ('seeker5@procrush.test',  'Kubernetes'), ('seeker5@procrush.test', 'Docker'), ('seeker5@procrush.test', 'AWS'),
    ('seeker5@procrush.test',  'CI/CD'), ('seeker5@procrush.test', 'Python'), ('seeker5@procrush.test', 'Terraform'),
    ('seeker6@procrush.test',  'Figma'), ('seeker6@procrush.test', 'UI/UX Design'), ('seeker6@procrush.test', 'Technical Writing'),
    ('seeker7@procrush.test',  'Python'), ('seeker7@procrush.test', 'SQL'), ('seeker7@procrush.test', 'PostgreSQL'),
    ('seeker7@procrush.test',  'Data Analysis'), ('seeker7@procrush.test', 'Machine Learning'),
    ('seeker8@procrush.test',  'Kotlin'), ('seeker8@procrush.test', 'Java'), ('seeker8@procrush.test', 'Agile'),
    ('seeker8@procrush.test',  'Scrum'), ('seeker8@procrush.test', 'Project Management'), ('seeker8@procrush.test', 'Microservices'),
    ('seeker9@procrush.test',  'Python'), ('seeker9@procrush.test', 'PostgreSQL'), ('seeker9@procrush.test', 'Git'),
    ('seeker10@procrush.test', 'Data Analysis'), ('seeker10@procrush.test', 'Technical Writing')
) AS v(email, skill_name)
JOIN seekers s ON s.user_id = (SELECT id FROM users WHERE email = v.email)
JOIN skills sk ON sk.name = v.skill_name;

-- =============================================================================
-- Seeker desired positions
-- =============================================================================

INSERT INTO seeker_desired_positions (seeker_id, occupation_id, created_at)
SELECT s.id, o.id, NOW()
FROM (VALUES
    ('seeker1@procrush.test',  'Backend-разработчик'),
    ('seeker1@procrush.test',  'Fullstack-разработчик'),
    ('seeker2@procrush.test',  'Frontend-разработчик'),
    ('seeker2@procrush.test',  'Fullstack-разработчик'),
    ('seeker3@procrush.test',  'Product Manager'),
    ('seeker3@procrush.test',  'Project Manager'),
    ('seeker4@procrush.test',  'QA-инженер'),
    ('seeker5@procrush.test',  'DevOps-инженер'),
    ('seeker6@procrush.test',  'UX/UI дизайнер'),
    ('seeker6@procrush.test',  'Product Designer'),
    ('seeker7@procrush.test',  'Data Engineer'),
    ('seeker8@procrush.test',  'Team Lead'),
    ('seeker8@procrush.test',  'Product Manager'),
    ('seeker9@procrush.test',  'Backend-разработчик'),
    ('seeker10@procrush.test', 'Data Engineer'),
    ('seeker10@procrush.test', 'Product Manager')
) AS v(email, occupation_name)
JOIN seekers s ON s.user_id = (SELECT id FROM users WHERE email = v.email)
JOIN occupations o ON o.name = v.occupation_name AND o.is_leaf = true;

-- =============================================================================
-- Survey results (seekers 1–8: all core surveys + test 2/64qn; seeker 9: first 3 core)
-- =============================================================================

INSERT INTO survey_results (seeker_id, survey_id, answers, calculated_results, started_at, completed_at, updated_at)
SELECT
    s.id,
    sv.id,
    CASE sv.code
        WHEN '2501-10-5KEY' THEN jsonb_build_object(
            '1', 'Карьера мечты — ' || s.first_name || ' занимается любимым делом с ростом',
            '2', 'Делай сегодня то, что другие не хотят — завтра будешь жить иначе',
            '3', 'Есть автономия, понятные цели и сильная команда',
            '4', 'Доверяет, даёт обратную связь и не микроменеджит',
            '5', 'Открытый, профессиональный, без токсичности'
        )::text
        WHEN '2520-10-ETYA' THEN '{"chosen_qualities":[2,6,9,13,15,20,24,29,32,36,39,41]}'::text
        WHEN '2521-10-NEYA' THEN '{"rejected_qualities":[4,7,11,16,19,23,27,35]}'::text
        WHEN '2530-10-12F4' THEN (
            SELECT jsonb_object_agg('q' || i, '{"1":2,"2":3,"3":3,"4":2}'::jsonb)::text
            FROM generate_series(1, 12) AS i
        )
        WHEN '2550-10-1IN2' THEN jsonb_build_object(
            '1', 1 + (s.id % 2), '2', 2 - (s.id % 2), '3', 1 + (s.id % 2),
            '4', 2 - (s.id % 2), '5', 1 + (s.id % 2), '6', 2 - (s.id % 2),
            '7', 1 + (s.id % 2), '8', 2 - (s.id % 2), '9', 1 + (s.id % 2),
            '10', 2 - (s.id % 2)
        )::text
        WHEN '2551-10-GNFL' THEN ('{"positive_factors":[' || (2 + s.id % 5) || ',' || (4 + s.id % 4) || ',' || (8 + s.id % 3) || ']}')::text
        WHEN '2552-10-RDFL' THEN ('{"annoying_factors":[' || (1 + s.id % 3) || ',' || (5 + s.id % 4) || ',' || (9 + s.id % 5) || ',' || (13 + s.id % 3) || ',' || (17 + s.id % 4) || ',' || (21 + s.id % 2) || ']}')::text
        WHEN '2560-10-BLBN' THEN (
            SELECT jsonb_object_agg('section_' || i, '{"1":5,"2":5}'::jsonb)::text
            FROM generate_series(1, 7) AS i
        )
    END,
    CASE sv.code
        WHEN '2501-10-5KEY' THEN '{"responses":[{"id":1,"answer":"ok"},{"id":2,"answer":"ok"}]}'
        WHEN '2520-10-ETYA' THEN '{"axis_totals":{"A3":5,"A1":2,"B3":3,"B1":1,"C3":1,"C1":0,"D3":4,"D1":1}}'
        WHEN '2521-10-NEYA' THEN '{"axis_totals":{"A3":2,"A1":1,"B3":3,"B1":0,"C3":2,"C1":1,"D3":1,"D1":1}}'
        WHEN '2530-10-12F4' THEN ('{"totals":{"Q":' || (24 + s.id % 8) || ',"W":' || (22 + s.id % 6) || ',"E":' || (26 + s.id % 5) || ',"R":' || (20 + s.id % 7) || '}}')
        WHEN '2550-10-1IN2' THEN '{"dilemmas":[{"code":"S"},{"code":"Yo"},{"code":"R"}]}'
        WHEN '2551-10-GNFL' THEN '{"green_score":4,"factor_codes":["S","W","Ip"]}'
        WHEN '2552-10-RDFL' THEN '{"risk_profile":{"R":3,"Z":2,"W":2,"S":1}}'
        WHEN '2560-10-BLBN' THEN ('{"role_totals":{"BR_CO":' || (8 + s.id % 4) || ',"BR_PL":' || (10 + s.id % 3) || ',"BR_CF":' || (7 + s.id % 5) || ',"BR_ME":' || (9 + s.id % 4) || '}}')
    END,
    NOW() - (s.id || ' days')::interval,
    NOW() - ((s.id - 1) || ' days')::interval,
    NOW()
FROM seekers s
JOIN users u ON u.id = s.user_id
CROSS JOIN surveys sv
WHERE u.email BETWEEN 'seeker1@procrush.test' AND 'seeker8@procrush.test'
  AND sv.group_code = 'core';

INSERT INTO survey_results (seeker_id, survey_id, answers, calculated_results, started_at, completed_at, updated_at)
SELECT
    s.id,
    sv.id,
    CASE sv.code
        WHEN '2501-10-5KEY' THEN '{"1":"Хочу развиваться в backend","2":"Учись каждый день","3":"Когда вижу результат","4":"Терпеливый наставник","5":"Молодой и амбициозный"}'::text
        WHEN '2520-10-ETYA' THEN '{"chosen_qualities":[3,10,14,18,22,26,30,33,37,40,44,47]}'::text
        WHEN '2521-10-NEYA' THEN '{"rejected_qualities":[1,6,12,15,20,25,30,42]}'::text
    END,
    CASE sv.code
        WHEN '2501-10-5KEY' THEN '{"responses":[{"id":1,"answer":"junior"}]}'
        WHEN '2520-10-ETYA' THEN '{"axis_totals":{"A3":3,"B3":4,"C3":2,"D3":3}}'
        WHEN '2521-10-NEYA' THEN '{"axis_totals":{"A3":2,"B3":2,"C3":3,"D3":1}}'
    END,
    NOW() - interval '3 days',
    NOW() - interval '2 days',
    NOW()
FROM seekers s
JOIN users u ON u.id = s.user_id
JOIN surveys sv ON sv.code IN ('2501-10-5KEY', '2520-10-ETYA', '2521-10-NEYA')
WHERE u.email = 'seeker9@procrush.test';

-- Test 2 (group 64qn): 64QN personality questionnaire
INSERT INTO survey_results (seeker_id, survey_id, answers, calculated_results, started_at, completed_at, updated_at)
SELECT
    s.id,
    sv.id,
    (SELECT jsonb_object_agg(i::text, (i % 5))::text FROM generate_series(1, 64) AS i),
    ('{"pole_totals":{"D":' || (48 + s.id % 10) || ',"I":' || (42 + s.id % 8) || ',"S":' || (55 + s.id % 12) || ',"C":' || (50 + s.id % 9) || '}}'),
    NOW() - interval '1 day',
    NOW(),
    NOW()
FROM seekers s
JOIN users u ON u.id = s.user_id
JOIN surveys sv ON sv.code = '2540-10-64QN'
WHERE u.email BETWEEN 'seeker1@procrush.test' AND 'seeker8@procrush.test';

-- =============================================================================
-- Employer job profiles (for matching)
-- =============================================================================

INSERT INTO employer_job_profiles (id, employer_id, occupation_id, description, required_personality, is_active, created_at, updated_at)
SELECT v.id, e.id, o.id, v.description, v.required_personality, true, NOW(), NOW()
FROM (VALUES
    (1, 'employer1@procrush.test', 'Backend-разработчик',
     'Разработка микросервисов на Kotlin/Spring Boot. Команда 6 человек, гибрид, code review и менторинг.',
     '{"axisDominance":0.55,"axisInfluence":0.40,"axisStability":0.70,"axisIntegrity":0.85,"axisAutonomy":0.75,"axisPace":0.50}'),
    (2, 'employer1@procrush.test', 'Team Lead',
     'Техническое лидерство backend-команды, архитектура, найм и развитие инженеров.',
     '{"axisDominance":0.80,"axisInfluence":0.75,"axisStability":0.55,"axisIntegrity":0.60,"axisAutonomy":0.65,"axisPace":0.70}'),
    (3, 'employer2@procrush.test', 'DevOps-инженер',
     'Поддержка Kubernetes-кластеров, CI/CD, миграции в AWS, on-call по графику.',
     '{"axisDominance":0.50,"axisInfluence":0.35,"axisStability":0.85,"axisIntegrity":0.90,"axisAutonomy":0.80,"axisPace":0.60}'),
    (4, 'employer2@procrush.test', 'QA-инженер',
     'Автотесты, тестовая стратегия, интеграция в CI/CD. Фокус на стабильность релизов.',
     '{"axisDominance":0.40,"axisInfluence":0.35,"axisStability":0.90,"axisIntegrity":0.90,"axisAutonomy":0.50,"axisPace":0.45}'),
    (5, 'employer3@procrush.test', 'Data Engineer',
     'Построение ETL-пайплайнов, DWH, Spark/Airflow. Работа с аналитиками и ML-командой.',
     '{"axisDominance":0.45,"axisInfluence":0.30,"axisStability":0.75,"axisIntegrity":0.92,"axisAutonomy":0.78,"axisPace":0.48}'),
    (6, 'employer4@procrush.test', 'UX/UI дизайнер',
     'Мобильные и веб-интерфейсы, user research, дизайн-система, тесная работа с разработкой.',
     '{"axisDominance":0.35,"axisInfluence":0.78,"axisStability":0.60,"axisIntegrity":0.55,"axisAutonomy":0.65,"axisPace":0.65}'),
    (7, 'employer4@procrush.test', 'Product Designer',
     'End-to-end дизайн продукта: от исследований до handoff, A/B-тесты, дизайн-системы.',
     '{"axisDominance":0.38,"axisInfluence":0.72,"axisStability":0.62,"axisIntegrity":0.58,"axisAutonomy":0.68,"axisPace":0.62}'),
    (8, 'employer5@procrush.test', 'Product Manager',
     'B2B SaaS, roadmap, метрики, работа с 2 командами разработки и стейкхолдерами.',
     '{"axisDominance":0.75,"axisInfluence":0.68,"axisStability":0.58,"axisIntegrity":0.62,"axisAutonomy":0.70,"axisPace":0.65}'),
    (9, 'employer5@procrush.test', 'Project Manager',
     'Agile-трансформация клиентов, управление сроками и рисками, фасилитация команд.',
     '{"axisDominance":0.65,"axisInfluence":0.70,"axisStability":0.65,"axisIntegrity":0.60,"axisAutonomy":0.55,"axisPace":0.60}')
) AS v(id, email, occupation_name, description, required_personality)
JOIN employers e ON e.user_id = (SELECT id FROM users WHERE email = v.email)
JOIN occupations o ON o.name = v.occupation_name AND o.is_leaf = true;

INSERT INTO job_profile_skills (job_profile_id, skill_id)
SELECT jp.id, sk.id
FROM (VALUES
    ('employer1@procrush.test', 'Backend-разработчик', 'Kotlin'),
    ('employer1@procrush.test', 'Backend-разработчик', 'Java'),
    ('employer1@procrush.test', 'Backend-разработчик', 'PostgreSQL'),
    ('employer1@procrush.test', 'Backend-разработчик', 'Spring Boot'),
    ('employer1@procrush.test', 'Backend-разработчик', 'REST API'),
    ('employer1@procrush.test', 'Backend-разработчик', 'Docker'),
    ('employer1@procrush.test', 'Team Lead', 'Kotlin'),
    ('employer1@procrush.test', 'Team Lead', 'Agile'),
    ('employer1@procrush.test', 'Team Lead', 'Scrum'),
    ('employer1@procrush.test', 'Team Lead', 'Microservices'),
    ('employer1@procrush.test', 'Team Lead', 'Project Management'),
    ('employer2@procrush.test', 'DevOps-инженер', 'Kubernetes'),
    ('employer2@procrush.test', 'DevOps-инженер', 'Docker'),
    ('employer2@procrush.test', 'DevOps-инженер', 'AWS'),
    ('employer2@procrush.test', 'DevOps-инженер', 'CI/CD'),
    ('employer2@procrush.test', 'DevOps-инженер', 'Python'),
    ('employer2@procrush.test', 'QA-инженер', 'Java'),
    ('employer2@procrush.test', 'QA-инженер', 'CI/CD'),
    ('employer2@procrush.test', 'QA-инженер', 'SQL'),
    ('employer2@procrush.test', 'QA-инженер', 'Docker'),
    ('employer2@procrush.test', 'QA-инженер', 'Agile'),
    ('employer3@procrush.test', 'Data Engineer', 'Python'),
    ('employer3@procrush.test', 'Data Engineer', 'SQL'),
    ('employer3@procrush.test', 'Data Engineer', 'PostgreSQL'),
    ('employer3@procrush.test', 'Data Engineer', 'Data Analysis'),
    ('employer3@procrush.test', 'Data Engineer', 'Machine Learning'),
    ('employer4@procrush.test', 'UX/UI дизайнер', 'Figma'),
    ('employer4@procrush.test', 'UX/UI дизайнер', 'UI/UX Design'),
    ('employer4@procrush.test', 'Product Designer', 'Figma'),
    ('employer4@procrush.test', 'Product Designer', 'UI/UX Design'),
    ('employer4@procrush.test', 'Product Designer', 'Technical Writing'),
    ('employer5@procrush.test', 'Product Manager', 'Agile'),
    ('employer5@procrush.test', 'Product Manager', 'Scrum'),
    ('employer5@procrush.test', 'Product Manager', 'Project Management'),
    ('employer5@procrush.test', 'Product Manager', 'Data Analysis'),
    ('employer5@procrush.test', 'Project Manager', 'Agile'),
    ('employer5@procrush.test', 'Project Manager', 'Scrum'),
    ('employer5@procrush.test', 'Project Manager', 'Project Management')
) AS v(email, occupation_name, skill_name)
JOIN employers e ON e.user_id = (SELECT id FROM users WHERE email = v.email)
JOIN occupations o ON o.name = v.occupation_name AND o.is_leaf = true
JOIN employer_job_profiles jp ON jp.employer_id = e.id AND jp.occupation_id = o.id
JOIN skills sk ON sk.name = v.skill_name;

-- =============================================================================
-- Personality profiles (seekers 1–8, generation_status = READY)
-- =============================================================================

INSERT INTO seeker_personal_profiles (
    seeker_id, title, description, profile, autonomy, thinking_style, burnout_risk,
    connections, creativity, drive, thinking,
    axis_dominance, axis_influence, axis_stability, axis_integrity, axis_autonomy, axis_pace,
    burnout_risk_overload, burnout_risk_conflicts, burnout_risk_demotivation, burnout_risk_stress,
    energy_sources, stop_factors, generation_status, generation_error, updated_at
)
SELECT
    s.id,
    a.title,
    a.description,
    a.profile,
    a.autonomy,
    a.thinking_style,
    a.burnout_risk,
    jsonb_build_object(
        'description', 'Ваш раздел СВЯЗИ показывает, как вы выстраиваете отношения и взаимодействуете в команде.',
        'top_strength_index', a.conn_top,
        'traits', jsonb_build_array(
            jsonb_build_object('label', 'Вы дипломатичны', 'scale_position', a.c0, 'left_pole', 'Прямолинейность', 'right_pole', 'Дипломатичность',
                'details', jsonb_build_object('description', 'Учитываете интересы сторон и ищете конструктивные решения.', 'good_day', 'Сглаживаете острые углы', 'bad_day', 'Избегаете жёсткой обратной связи', 'succeed_through', jsonb_build_array('слушание', 'компромисс', 'ясная позиция'))),
            jsonb_build_object('label', 'Вы поддерживающи', 'scale_position', a.c1, 'left_pole', 'Автономность', 'right_pole', 'Поддержка',
                'details', jsonb_build_object('description', 'Цените команду и готовы помогать коллегам.', 'good_day', 'Создаёте доверие', 'bad_day', 'Берёте чужую ответственность', 'succeed_through', jsonb_build_array('эмпатия', 'надёжность', 'границы'))),
            jsonb_build_object('label', 'Вы сдержанны', 'scale_position', a.c2, 'left_pole', 'Эмоциональность', 'right_pole', 'Сбалансированность',
                'details', jsonb_build_object('description', 'Контролируете эмоции в рабочих ситуациях.', 'good_day', 'Сохраняете спокойствие', 'bad_day', 'Кажетесь отстранённым', 'succeed_through', jsonb_build_array('стабильность', 'фокус', 'обратная связь'))),
            jsonb_build_object('label', 'Вы общительны', 'scale_position', a.c3, 'left_pole', 'Сдержанность', 'right_pole', 'Общительность',
                'details', jsonb_build_object('description', 'Легко устанавливаете контакт с новыми людьми.', 'good_day', 'Вовлекаете команду', 'bad_day', 'Отвлекаетесь на социальное', 'succeed_through', jsonb_build_array('нетворкинг', 'энергия', 'структура')))
        )
    ),
    jsonb_build_object(
        'description', 'Ваш раздел КРЕАТИВНОСТЬ показывает баланс между прагматизмом и новаторством.',
        'top_strength_index', a.crea_top,
        'traits', jsonb_build_array(
            jsonb_build_object('label', 'Вы адаптивны', 'scale_position', a.cr0, 'left_pole', 'Фокус', 'right_pole', 'Адаптивность',
                'details', jsonb_build_object('description', 'Гибко переключаетесь между задачами.', 'good_day', 'Быстро находите решения', 'bad_day', 'Теряете глубину', 'succeed_through', jsonb_build_array('гибкость', 'скорость', 'приоритеты'))),
            jsonb_build_object('label', 'Вы прагматичны', 'scale_position', a.cr1, 'left_pole', 'Инновации', 'right_pole', 'Прагматизм',
                'details', jsonb_build_object('description', 'Предпочитаете проверенные подходы.', 'good_day', 'Снижаете риски', 'bad_day', 'Сопротивляетесь новому', 'succeed_through', jsonb_build_array('реализм', 'эффективность', 'опыт'))),
            jsonb_build_object('label', 'Вы открыты опыту', 'scale_position', a.cr2, 'left_pole', 'Классика', 'right_pole', 'Открытость',
                'details', jsonb_build_object('description', 'Интересуетесь новыми идеями и подходами.', 'good_day', 'Внедряете улучшения', 'bad_day', 'Распыляетесь', 'succeed_through', jsonb_build_array('любопытство', 'эксперимент', 'фокус')))
        )
    ),
    jsonb_build_object(
        'description', 'Ваш раздел ДРАЙВ отражает внутреннюю мотивацию и амбициозность.',
        'top_strength_index', a.drive_top,
        'traits', jsonb_build_array(
            jsonb_build_object('label', 'Вы уверенны', 'scale_position', a.d0, 'left_pole', 'Скромность', 'right_pole', 'Уверенность',
                'details', jsonb_build_object('description', 'Верите в свои силы и берёте ответственность.', 'good_day', 'Ведёте за собой', 'bad_day', 'Переоцениваете возможности', 'succeed_through', jsonb_build_array('решительность', 'инициатива', 'рефлексия'))),
            jsonb_build_object('label', 'Вы целеустремленны', 'scale_position', a.d1, 'left_pole', 'Терпение', 'right_pole', 'Достижения',
                'details', jsonb_build_object('description', 'Ориентированы на результат и рост.', 'good_day', 'Доводите до конца', 'bad_day', 'Выгораете от перегруза', 'succeed_through', jsonb_build_array('цели', 'дисциплина', 'баланс'))),
            jsonb_build_object('label', 'Вы дисциплинированы', 'scale_position', a.d2, 'left_pole', 'Расслабленность', 'right_pole', 'Дисциплина',
                'details', jsonb_build_object('description', 'Следуете плану и соблюдаете договорённости.', 'good_day', 'Предсказуемы для команды', 'bad_day', 'Жёстки к себе', 'succeed_through', jsonb_build_array('планирование', 'надёжность', 'гибкость'))),
            jsonb_build_object('label', 'Вы независимы', 'scale_position', a.d3, 'left_pole', 'Исполнительность', 'right_pole', 'Независимость',
                'details', jsonb_build_object('description', 'Предпочитаете автономную работу.', 'good_day', 'Берёте инициативу', 'bad_day', 'Сложно с подчинением', 'succeed_through', jsonb_build_array('самостоятельность', 'ответственность', 'коллаборация')))
        )
    ),
    jsonb_build_object(
        'description', 'Ваш раздел МЫШЛЕНИЕ описывает стиль решения задач.',
        'top_strength_index', 0,
        'traits', jsonb_build_array(
            jsonb_build_object('label', 'Вы аналитичны', 'scale_position', a.th0, 'left_pole', 'Интуиция', 'right_pole', 'Аналитика',
                'details', jsonb_build_object('description', 'Опираетесь на данные и логику.', 'good_day', 'Находите корень проблемы', 'bad_day', 'Застреваете в деталях', 'succeed_through', jsonb_build_array('анализ', 'структура', 'скорость')))
        )
    ),
    a.axis_dominance, a.axis_influence, a.axis_stability, a.axis_integrity, a.axis_autonomy, a.axis_pace,
    a.br_overload, a.br_conflicts, a.br_demotivation, a.br_stress,
    jsonb_build_object(
        'title', 'Источники энергии',
        'items', jsonb_build_array(
            jsonb_build_object('title', a.es0_title, 'description', a.es0_desc),
            jsonb_build_object('title', a.es1_title, 'description', a.es1_desc),
            jsonb_build_object('title', a.es2_title, 'description', a.es2_desc)
        )
    ),
    jsonb_build_object(
        'title', 'Стоп-факторы',
        'items', jsonb_build_array(
            jsonb_build_object('title', a.sf0_title, 'description', a.sf0_desc),
            jsonb_build_object('title', a.sf1_title, 'description', a.sf1_desc)
        )
    ),
    'READY',
    NULL,
    NOW()
FROM seekers s
JOIN users u ON u.id = s.user_id
JOIN (VALUES
    ('seeker1@procrush.test', 'Стратегический аналитик',
     'Системное мышление и внимание к деталям. Ценит автономию и чёткие цели.',
     'Иван сочетает глубокий анализ с прагматичным подходом к инженерным задачам.',
     'Высокая потребность в самостоятельности при понятных рамках ответственности.',
     'Аналитический, опирается на данные и архитектурные паттерны.',
     'Умеренный риск при хронической перегрузке без делегирования.',
     3, 1, 0.62, 0.45, 0.71, 0.83, 0.78, 0.55,
     0.55, 0.68, 0.42, 0.25, 0.35, 0.40, 0.68,
     0.55, 0.35, 0.40, 0.68, 0.85,
     0.55, 0.40, 0.35, 0.45, 0,
     'Амбициозные технические задачи', 'Сложные архитектурные вызовы заряжают и дают ощущение профессионального роста каждый квартал.',
     'Автономность', 'Свобода выбора решений и доверие команды — главный источник мотивации в долгосрочной перспективе.',
     'Обучение', 'Новые технологии и менторство junior-разработчиков поддерживают интерес даже в рутинные периоды.',
     'Микроменеджмент', 'Постоянный контроль каждого шага снижает продуктивность и вызывает раздражение.',
     'Хаотичные процессы', 'Размытые приоритеты и частая смена целей без объяснений быстро истощают.'),
    ('seeker2@procrush.test', 'Креативный коммуникатор',
     'Энергичная, ориентирована на пользователя. Сильна в визуальном мышлении и командной работе.',
     'Мария создаёт интерфейсы, которые сочетают эстетику и удобство, и вдохновляет команду идеями.',
     'Комфортна при совместном принятии решений, но ценит творческую свободу.',
     'Визуально-ассоциативный, быстро прототипирует гипотезы.',
     'Риск выгорания при жёстких дедлайнах без творческого пространства.',
     1, 2, 0.35, 0.82, 0.48, 0.55, 0.60, 0.72,
     0.70, 0.75, 0.80, 0.65, 0.72, 0.55, 0.35,
     0.65, 0.50, 0.40, 0.55, 0.60,
     0.70, 0.45, 0.50, 0.55, 1,
     'Творческие проекты', 'Дизайн-челленджи с реальным влиянием на продукт дают максимальный приток энергии и вовлечённости.',
     'Командный дух', 'Работа с вдохновлённой командой и открытым обменом идеями заряжает на весь спринт.',
     'Признание', 'Видимый отклик пользователей и позитивная обратная связь усиливают мотивацию.',
     'Однообразие задач', 'Монотонная вёрстка без исследований быстро снижает интерес к работе.',
     'Игнорирование UX', 'Когда бизнес давит на скорость в ущерб качеству интерфейса, мотивация падает.'),
    ('seeker3@procrush.test', 'Прагматичный лидер продукта',
     'Балансирует интересы бизнеса, пользователей и команды. Сильные навыки приоритизации.',
     'Алексей умеет превращать стратегию в конкретный roadmap и вести стейкхолдеров к общему решению.',
     'Предпочитает делегировать, но контролирует ключевые метрики.',
     'Стратегический, сочетает данные и качественные инсайты.',
     'Умеренный риск при конфликте стейкхолдеров и давлении сроков.',
     0, 0, 0.78, 0.70, 0.58, 0.65, 0.72, 0.68,
     0.60, 0.50, 0.75, 0.55, 0.45, 0.40, 0.50,
     0.60, 0.70, 0.45, 0.55, 0.65,
     0.50, 0.65, 0.55, 0.45, 0,
     'Влияние на продукт', 'Возможность формировать продуктовую стратегию и видеть результат в метриках — ключевой драйвер.',
     'Сильная команда', 'Работа с компетентными инженерами и дизайнерами даёт энергию и уверенность в решениях.',
     'Рост компании', 'Масштабирование продукта и выход на новые рынки мотивирует брать амбициозные цели.',
     'Бюрократия', 'Длинные согласования и отчёты ради отчётов тормозят и демотивируют.',
     'Размытые цели', 'Отсутствие чёткого видения от руководства создаёт хаос в приоритетах.'),
    ('seeker4@procrush.test', 'Надёжный контролёр качества',
     'Внимательна к деталям, методична, ценит стабильность процессов и предсказуемость релизов.',
     'Елена строит тестовую стратегию, которая защищает продукт от регрессий без лишней бюрократии.',
     'Предпочитает чёткие процедуры и документированные стандарты качества.',
     'Системный, проверяет гипотезы через тест-кейсы и метрики дефектов.',
     'Низкий риск при стабильной нагрузке, повышается при авралах перед релизом.',
     2, 1, 0.42, 0.38, 0.88, 0.90, 0.55, 0.48,
     0.35, 0.82, 0.90, 0.30, 0.25, 0.40, 0.45,
     0.45, 0.85, 0.55, 0.40, 0.75,
     0.35, 0.45, 0.25, 0.40, 1,
     'Качество продукта', 'Уверенность, что продукт стабилен и пользователи довольны, даёт профессиональное удовлетворение.',
     'Прозрачные процессы', 'Чёткие критерии приёмки и регламенты тестирования создают комфортную рабочую среду.',
     'Автоматизация', 'Построение автотестов и улучшение CI/CD приносит ощутимый результат и экономит время.',
     'Хаос в требованиях', 'Постоянные изменения без обновления тестов создают стресс и ощущение бессмысленной работы.',
     'Давление на скорость', 'Жертвование качеством ради дедлайна противоречит профессиональным ценностям.'),
    ('seeker5@procrush.test', 'Системный DevOps-инженер',
     'Спокоен под давлением, ориентирован на автоматизацию и надёжность инфраструктуры.',
     'Дмитрий проектирует отказоустойчивые системы и снижает операционные риски через IaC и мониторинг.',
     'Высокая автономность, предпочитает самостоятельно принимать технические решения.',
     'Инженерный, опирается на метрики и SRE-практики.',
     'Умеренный риск при дежурствах и ночных инцидентах.',
     3, 0, 0.55, 0.40, 0.80, 0.88, 0.85, 0.62,
     0.50, 0.88, 0.75, 0.35, 0.30, 0.55, 0.60,
     0.40, 0.50, 0.75, 0.70, 0.75,
     0.55, 0.45, 0.60, 0.55, 0,
     'Автоматизация', 'Каждый автоматизированный процесс даёт ощутимый результат и снижает рутину для всей команды.',
     'Стабильность систем', 'Высокий uptime и предсказуемые релизы — главный показатель успеха и источник гордости.',
     'Инженерные вызовы', 'Масштабирование инфраструктуры и оптимизация затрат заряжают в долгосрочной перспективе.',
     'Ручные операции', 'Постоянные ручные деплои и отсутствие документации быстро выматывают.',
     'Культ переработок', 'Ночные инциденты без ротации дежурств ведут к хронической усталости.'),
    ('seeker6@procrush.test', 'Эмпатичный продуктовый дизайнер',
     'Чувствительна к потребностям пользователей, сильна в исследованиях и визуальной коммуникации.',
     'Анна создаёт дизайн, который решает реальные проблемы пользователей и согласуется с бизнес-целями.',
     'Нуждается в творческой свободе, но ценит обратную связь от команды и пользователей.',
     'Дизайн-мышление, от эмпатии к прототипу и тестированию.',
     'Повышенный риск при токсичной обратной связи и игнорировании UX-исследований.',
     1, 1, 0.38, 0.75, 0.62, 0.58, 0.68, 0.65,
     0.72, 0.78, 0.55, 0.70, 0.78, 0.60, 0.72,
     0.55, 0.45, 0.50, 0.72, 0.60,
     0.72, 0.55, 0.50, 0.60, 1,
     'Пользовательский отклик', 'Положительные результаты usability-тестов и рост метрик вовлечённости заряжают на месяцы вперёд.',
     'Коллаборация', 'Тесная работа с разработчиками и PM в атмосфере взаимного уважения даёт энергию.',
     'Визуальное мастерство', 'Создание элегантных интерфейсов и дизайн-систем приносит эстетическое удовлетворение.',
     'Токсичная критика', 'Публичное обесценивание работы без конструктива подрывает уверенность и мотивацию.',
     'Игнорирование исследований', 'Решения без данных о пользователях воспринимаются как профессиональная ошибка.'),
    ('seeker7@procrush.test', 'Аналитик данных',
     'Глубоко погружён в данные, ценит точность и воспроизводимость аналитических пайплайнов.',
     'Сергей строит надёжные ETL-процессы и помогает бизнесу принимать решения на основе фактов.',
     'Предпочитает работать самостоятельно над сложными задачами с минимальным вмешательством.',
     'Аналитический, гипотезы проверяет через SQL и статистику.',
     'Низкий риск при стабильных задачах, растёт при постоянных срочных ad-hoc запросах.',
     0, 0, 0.48, 0.35, 0.75, 0.92, 0.80, 0.50,
     0.40, 0.90, 0.85, 0.30, 0.35, 0.45, 0.40,
     0.35, 0.90, 0.60, 0.70, 0.60,
     0.90, 0.40, 0.35, 0.45, 0,
     'Сложные датасеты', 'Работа с большими объёмами данных и нетривиальными задачами даёт профессиональное удовлетворение.',
     'Влияние на решения', 'Когда аналитика напрямую влияет на стратегию компании, мотивация максимальна.',
     'Чистая архитектура', 'Построение надёжного DWH и оптимизация пайплайнов приносит долгосрочную ценность.',
     'Грязные данные', 'Постоянная борьба с некачественными источниками без ресурсов на исправление демотивирует.',
     'Ad-hoc хаос', 'Бесконечные срочные запросы без приоритизации мешают глубокой работе.'),
    ('seeker8@procrush.test', 'Харизматичный руководитель',
     'Уверенный лидер, мотивирует команду, сочетает стратегическое видение с операционным управлением.',
     'Ольга выстраивает высокоэффективные команды и балансирует скорость доставки с качеством.',
     'Делегирует, но держит руку на пульсе ключевых решений и найма.',
     'Стратегический, быстро переключается между уровнями абстракции.',
     'Повышенный риск при конфликтах в команде и давлении сверху.',
     0, 0, 0.85, 0.78, 0.55, 0.60, 0.70, 0.75,
     0.80, 0.50, 0.65, 0.70, 0.75, 0.65, 0.50,
     0.70, 0.70, 0.55, 0.65, 0.65,
     0.50, 0.65, 0.60, 0.55, 0,
     'Рост команды', 'Развитие людей и успехи команды — главный источник профессиональной гордости.',
     'Стратегические цели', 'Амбициозные OKR и видимый прогресс компании заряжают и задают направление.',
     'Влияние', 'Возможность формировать культуру и процессы в организации даёт смысл работе.',
     'Политические игры', 'Интриги и присвоение заслуг подрывают доверие и желание лидировать.',
     'Микроменеджмент сверху', 'Когда руководство не доверяет решениям, эффективность лидера падает.')
) AS a(
    email, title, description, profile, autonomy, thinking_style, burnout_risk,
    conn_top, crea_top,
    axis_dominance, axis_influence, axis_stability, axis_integrity, axis_autonomy, axis_pace,
    c0, c1, c2, c3, cr0, cr1, cr2, d0, d1, d2, d3, th0,
    br_overload, br_conflicts, br_demotivation, br_stress,
    drive_top,
    es0_title, es0_desc, es1_title, es1_desc, es2_title, es2_desc,
    sf0_title, sf0_desc, sf1_title, sf1_desc
) ON u.email = a.email;

-- =============================================================================
-- Superpowers and talents (linked to seeker personal profiles)
-- =============================================================================

INSERT INTO seeker_superpowers_and_talents (seeker_personal_profile_id, superpowers_and_talents_id, is_pronounced, created_at, updated_at)
SELECT s.id, sat.id, v.is_pronounced, NOW(), NOW()
FROM (VALUES
    ('seeker1@procrush.test', 'Системный анализ',       true),
    ('seeker1@procrush.test', 'Принятие решений',       true),
    ('seeker1@procrush.test', 'Адаптивность и обучаемость', false),
    ('seeker2@procrush.test', 'Коммуникация и влияние', true),
    ('seeker2@procrush.test', 'Адаптивность и обучаемость', true),
    ('seeker2@procrush.test', 'Работа с данными',       false),
    ('seeker3@procrush.test', 'Стратегический лидер',   true),
    ('seeker3@procrush.test', 'Принятие решений',       true),
    ('seeker3@procrush.test', 'Мотивация команды',      true),
    ('seeker4@procrush.test', 'Системный анализ',       true),
    ('seeker4@procrush.test', 'Адаптивность и обучаемость', false),
    ('seeker5@procrush.test', 'Системный анализ',       true),
    ('seeker5@procrush.test', 'Принятие решений',       false),
    ('seeker5@procrush.test', 'Лидерство в неопределенности', true),
    ('seeker6@procrush.test', 'Коммуникация и влияние', true),
    ('seeker6@procrush.test', 'Адаптивность и обучаемость', true),
    ('seeker7@procrush.test', 'Работа с данными',       true),
    ('seeker7@procrush.test', 'Системный анализ',       true),
    ('seeker7@procrush.test', 'Принятие решений',       false),
    ('seeker8@procrush.test', 'Стратегический лидер',   true),
    ('seeker8@procrush.test', 'Мотивация команды',      true),
    ('seeker8@procrush.test', 'Лидерство в неопределенности', true)
) AS v(email, talent_name, is_pronounced)
JOIN seekers s ON s.user_id = (SELECT id FROM users WHERE email = v.email)
JOIN superpowers_and_talents sat ON sat.name = v.talent_name;

-- =============================================================================
-- Reset sequences after explicit ids (keeps auto-increment correct for new rows)
-- =============================================================================

SELECT setval(pg_get_serial_sequence('employers', 'id'), (SELECT COALESCE(MAX(id), 1) FROM employers));
SELECT setval(pg_get_serial_sequence('seekers', 'id'), (SELECT COALESCE(MAX(id), 1) FROM seekers));
SELECT setval(pg_get_serial_sequence('employer_job_profiles', 'id'), (SELECT COALESCE(MAX(id), 1) FROM employer_job_profiles));
