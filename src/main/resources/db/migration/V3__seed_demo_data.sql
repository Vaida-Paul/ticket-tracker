INSERT INTO users (id, email, password_hash, full_name, role, auth_provider, created_at, updated_at)
VALUES
    (1, 'admin@test.com',  '$2a$10$spbiOXIpB54QuKGb3GJ48OiiPtZ0wNP9GkU8YhfrANNcFUR/Sl3Xe', 'Admin User',  'ADMIN',           'LOCAL', NOW(), NOW()),
    (2, 'pm@test.com',     '$2a$10$spbiOXIpB54QuKGb3GJ48OiiPtZ0wNP9GkU8YhfrANNcFUR/Sl3Xe', 'PM User',     'PROJECT_MANAGER', 'LOCAL', NOW(), NOW()),
    (3, 'member@test.com', '$2a$10$spbiOXIpB54QuKGb3GJ48OiiPtZ0wNP9GkU8YhfrANNcFUR/Sl3Xe', 'Member User', 'MEMBER',          'LOCAL', NOW(), NOW())
    ON CONFLICT (email) DO NOTHING;

SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users), false);

INSERT INTO projects (id, name, description, created_by, created_at, updated_at)
VALUES
    (1, 'Mobile App Redesign', 'Redesign the mobile app UI/UX for Q4 release', 1, NOW() - INTERVAL '30 days', NOW() - INTERVAL '5 days'),
    (2, 'Backend Performance', 'Optimize API response times to under 100ms', 2, NOW() - INTERVAL '20 days', NOW() - INTERVAL '2 days')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO tickets (id, title, description, status, priority, project_id, assignee_id, due_date, created_at, updated_at)
VALUES
    (1, 'Fix login bug on iOS',                'Users cannot log in on iOS 17 after update',    'IN_PROGRESS', 'URGENT', 1, 3, NOW() - INTERVAL '2 days',  NOW() - INTERVAL '15 days', NOW() - INTERVAL '1 day'),
    (2, 'Optimize N+1 queries in ticket list', 'List page slow on large datasets',              'OPEN',        'URGENT', 2, 2, NOW() - INTERVAL '1 day',   NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    (3, 'Update dashboard UI',                 'New design mockups ready in Figma',             'OPEN',        'MEDIUM', 1, 3, NOW() + INTERVAL '6 hours', NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days'),
    (4, 'Add Redis caching layer',             'Cache frequent read queries for ticket list',   'OPEN',        'HIGH',   2, 2, NOW() + INTERVAL '5 days',  NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days'),
    (5, 'Write OpenAPI documentation',         'Complete Swagger docs for all endpoints',       'DONE',        'LOW',    2, 1, NOW() - INTERVAL '2 days',  NOW() - INTERVAL '8 days',  NOW() - INTERVAL '1 day')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO comments (id, ticket_id, author_id, body, created_at, updated_at)
VALUES
    (1, 1, 2, 'Reproduced on iOS 17.1. Working on a fix now.',        NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
    (2, 1, 1, 'Please prioritize this — it blocks our Q4 release.',   NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
    (3, 2, 2, 'Found the issue in TicketService.searchTickets.',      NOW() - INTERVAL '9 days',  NOW() - INTERVAL '9 days')
    ON CONFLICT (id) DO NOTHING;

SELECT setval('projects_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM projects), false);
SELECT setval('tickets_id_seq',  (SELECT COALESCE(MAX(id), 0) + 1 FROM tickets),  false);
SELECT setval('comments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM comments), false);