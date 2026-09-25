INSERT INTO projects (id, name, description, created_by, created_at, updated_at)
VALUES
    (1, 'Mobile App Redesign',      'Redesign the mobile app UI/UX for Q4 release',  1, NOW() - INTERVAL '30 days', NOW() - INTERVAL '5 days'),
    (2, 'Backend Performance',      'Optimize API response times to under 100ms',      2, NOW() - INTERVAL '20 days', NOW() - INTERVAL '2 days'),
    (3, 'Customer Support Portal',  'Self-service portal for customer tickets',       1, NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 day')
    ON CONFLICT (id) DO NOTHING;


INSERT INTO tickets (id, title, description, status, priority, project_id, assignee_id, due_date, created_at, updated_at)
VALUES
    -- OVERDUE
    (1,  'Fix login bug on iOS',           'Users cannot log in on iOS 17 after update',            'IN_PROGRESS', 'URGENT', 1, 3, NOW() - INTERVAL '2 days',  NOW() - INTERVAL '15 days', NOW() - INTERVAL '1 day'),
    (2,  'Optimize N+1 queries in ticket list', 'List page slow on large datasets (>10k tickets)',   'OPEN',        'URGENT', 2, 2, NOW() - INTERVAL '1 day',   NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    (3,  'Migration script failed on staging',  'Flyway V2 checksum mismatch on staging DB',        'IN_PROGRESS', 'HIGH',   2, 2, NOW() - INTERVAL '3 days',  NOW() - INTERVAL '7 days',  NOW() - INTERVAL '2 days'),

    -- DUE SOON (within 24h)
    (4,  'Update dashboard UI',            'New design mockups ready in Figma',                     'OPEN',        'MEDIUM', 1, 3, NOW() + INTERVAL '6 hours', NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days'),
    (5,  'Add API rate limiting',          'Protect public endpoints from abuse',                   'IN_PROGRESS', 'HIGH',   2, 2, NOW() + INTERVAL '20 hours', NOW() - INTERVAL '4 days',  NOW() - INTERVAL '1 day'),

    -- NORMAL (due in a few days)
    (6,  'Add Redis caching layer',        'Cache frequent read queries for ticket list',           'OPEN',        'HIGH',   2, 2, NOW() + INTERVAL '5 days',  NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days'),
    (7,  'Implement dark mode',            'User requested feature, high engagement',               'OPEN',        'MEDIUM', 1, 3, NOW() + INTERVAL '10 days', NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days'),
    (8,  'Refactor authentication layer',  'Move to Spring Security OAuth2 Resource Server',        'IN_PROGRESS', 'HIGH',   2, 3, NOW() + INTERVAL '7 days',  NOW() - INTERVAL '3 days',  NOW() - INTERVAL '1 day'),
    (9,  'Add CSV export for reports',     'Customers want to export ticket history',               'OPEN',        'LOW',    3, 3, NOW() + INTERVAL '14 days', NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days'),
    (10, 'Support SAML SSO',               'Enterprise customers require SAML login',               'OPEN',        'MEDIUM', 3, 2, NOW() + INTERVAL '21 days', NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days'),

    -- DONE
    (11, 'Write OpenAPI documentation',    'Complete Swagger docs for all endpoints',               'DONE',        'LOW',    2, 1, NOW() - INTERVAL '2 days',  NOW() - INTERVAL '8 days',  NOW() - INTERVAL '1 day'),
    (12, 'Upgrade to Spring Boot 3.4',     'Latest stable release with security patches',           'DONE',        'MEDIUM', 2, 2, NOW() - INTERVAL '5 days',  NOW() - INTERVAL '15 days', NOW() - INTERVAL '5 days')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO comments (id, ticket_id, author_id, body, created_at, updated_at)
VALUES
    (1,  1,  2, 'Reproduced on iOS 17.1. Working on a fix now.',                                     NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
    (2,  1,  1, 'Please prioritize this — it blocks our Q4 release.',                                NOW() - INTERVAL '13 days', NOW() - INTERVAL '13 days'),
    (3,  1,  3, 'Found related issue in Keychain access. Pushing a patch today.',                    NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days'),
    (4,  2,  2, 'Found the issue in TicketService.searchTickets. Query runs 3x per request.',        NOW() - INTERVAL '9 days',  NOW() - INTERVAL '9 days'),
    (5,  2,  1, 'Add @EntityGraph on the repository method — should fix N+1.',                       NOW() - INTERVAL '8 days',  NOW() - INTERVAL '8 days'),
    (6,  3,  2, 'Staging DB has a different checksum for V2. Need to align with prod.',              NOW() - INTERVAL '6 days',  NOW() - INTERVAL '6 days'),
    (7,  3,  1, 'Let''s baseline the schema and reset history. Document in runbook.',                NOW() - INTERVAL '5 days',  NOW() - INTERVAL '5 days'),
    (8,  4,  3, 'New mockups look great. Starting on the header component first.',                   NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days'),
    (9,  5,  2, 'Which endpoints need rate limiting? Only public auth ones or all?',                 NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days'),
    (10, 5,  1, 'Start with /api/auth/** — those are the ones exposed publicly.',                    NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days'),
    (11, 6,  2, 'Redis cluster is up. Configuring Spring Cache abstraction.',                        NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days'),
    (12, 8,  3, 'Should we use JJWT or Spring''s JwtEncoder? Let''s align on OAuth2 RS.',            NOW() - INTERVAL '2 days',  NOW() - INTERVAL '2 days'),
    (13, 8,  1, 'Go with Spring OAuth2 RS — better long-term for Entra ID migration.',               NOW() - INTERVAL '1 day',   NOW() - INTERVAL '1 day'),
    (14, 11, 2, 'Docs are complete. Merged and deployed to staging.',                                NOW() - INTERVAL '3 days',  NOW() - INTERVAL '3 days'),
    (15, 12, 1, 'Upgrade successful. No breaking changes in our codebase.',                          NOW() - INTERVAL '4 days',  NOW() - INTERVAL '4 days')
    ON CONFLICT (id) DO NOTHING;


SELECT setval('projects_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM projects), false);
SELECT setval('tickets_id_seq',  (SELECT COALESCE(MAX(id), 0) + 1 FROM tickets),  false);
SELECT setval('comments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM comments), false);