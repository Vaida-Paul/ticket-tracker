-- Extra demo data for local development.
-- All three new users have the password: password123

INSERT INTO users (email, password_hash, full_name, role, auth_provider, created_at, updated_at)
VALUES
    ('admin2@test.com',  '$2a$10$H1NiaJu/tTb2RgsqhS05QOg0RFm6F5E2E7889wc/mE2XfS74stHBS', 'Admin Two',  'ADMIN',           'LOCAL', NOW(), NOW()),
    ('pm2@test.com',     '$2a$10$H1NiaJu/tTb2RgsqhS05QOg0RFm6F5E2E7889wc/mE2XfS74stHBS', 'PM Two',     'PROJECT_MANAGER', 'LOCAL', NOW(), NOW()),
    ('member2@test.com', '$2a$10$H1NiaJu/tTb2RgsqhS05QOg0RFm6F5E2E7889wc/mE2XfS74stHBS', 'Member Two', 'MEMBER',          'LOCAL', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO projects (name, description, created_by, created_at, updated_at)
SELECT v.name, v.description, u.id, v.created_at, v.updated_at
FROM (VALUES
    ('Customer Portal Revamp', 'Rebuild the customer self-service portal with a new design system and a faster checkout flow.', 'pm2@test.com', NOW() - INTERVAL '18 days', NOW() - INTERVAL '3 days'),
    ('Analytics Dashboard', 'Internal dashboard for delivery metrics, sprint health, and ticket throughput.', 'admin2@test.com', NOW() - INTERVAL '12 days', NOW() - INTERVAL '1 day'),
    ('Mobile Push Notifications', 'Add push notifications for assignments, mentions, and due-date reminders.', 'pm2@test.com', NOW() - INTERVAL '9 days', NOW() - INTERVAL '6 hours'),
    ('Security Hardening', 'Close the remaining audit findings: rate limiting, token rotation, and dependency upgrades.', 'admin2@test.com', NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 hours')
) AS v(name, description, creator_email, created_at, updated_at)
JOIN users u ON u.email = v.creator_email
WHERE NOT EXISTS (SELECT 1 FROM projects p WHERE p.name = v.name);

INSERT INTO tickets (title, description, status, priority, project_id, assignee_id, due_date, created_at, updated_at)
SELECT
    v.title,
    v.description,
    v.status,
    v.priority,
    p.id,
    (SELECT u.id FROM users u WHERE u.email = v.assignee_email),
    v.due_date,
    v.created_at,
    v.updated_at
FROM (VALUES
    ('Add SSO button to customer portal', 'Add a "Sign in with company SSO" button and wire it to the identity provider metadata URL.', 'OPEN', 'HIGH', 'Customer Portal Revamp', 'member2@test.com', NOW() + INTERVAL '2 days', NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 day'),
    ('Fix broken avatar upload', 'Avatar uploads fail for images larger than 5 MB. Return a proper 413 and show a helpful error.', 'IN_PROGRESS', 'MEDIUM', 'Customer Portal Revamp', 'member@test.com', NOW() + INTERVAL '6 hours', NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 hours'),
    ('Create reusable data table component', 'Extract the ticket table into a reusable component with sorting, empty states, and responsive cards.', 'OPEN', 'MEDIUM', 'Customer Portal Revamp', 'member2@test.com', NOW() + INTERVAL '5 days', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('Add sprint burndown chart', 'Use the existing sprint endpoint and render a burndown chart for the current sprint.', 'IN_PROGRESS', 'HIGH', 'Analytics Dashboard', 'pm2@test.com', NOW() + INTERVAL '3 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '1 day'),
    ('Expose ticket throughput API', 'Add an endpoint that returns created vs. closed tickets per week for the last 12 weeks.', 'OPEN', 'URGENT', 'Analytics Dashboard', 'pm@test.com', NOW() - INTERVAL '1 day', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('Cache dashboard aggregates', 'Measure the current dashboard response time, then cache the aggregate queries if needed.', 'OPEN', 'MEDIUM', 'Analytics Dashboard', NULL, NOW() + INTERVAL '9 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('Design notification preferences screen', 'Create the preferences screen for assignment, mention, and due-date notifications.', 'OPEN', 'LOW', 'Mobile Push Notifications', 'member2@test.com', NOW() + INTERVAL '4 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days'),
    ('Implement push token registration', 'Register device push tokens on login and make the operation idempotent per user and device.', 'IN_PROGRESS', 'HIGH', 'Mobile Push Notifications', 'pm2@test.com', NOW() + INTERVAL '1 day', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 hours'),
    ('Handle expired push tokens', 'Remove or refresh push tokens that the provider reports as expired or invalid.', 'OPEN', 'MEDIUM', 'Mobile Push Notifications', 'pm@test.com', NOW() - INTERVAL '3 days', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('Add rate limiting to auth endpoints', 'Apply per-IP and per-account rate limits to login, register, and refresh.', 'DONE', 'HIGH', 'Security Hardening', 'admin@test.com', NOW() - INTERVAL '5 days', NOW() - INTERVAL '12 days', NOW() - INTERVAL '2 days'),
    ('Upgrade vulnerable dependencies', 'Upgrade transitive dependencies flagged by the security scan and verify compatibility.', 'IN_PROGRESS', 'URGENT', 'Security Hardening', 'admin2@test.com', NOW() + INTERVAL '12 hours', NOW() - INTERVAL '9 days', NOW() - INTERVAL '6 hours'),
    ('Document incident response runbook', 'Write the on-call runbook for auth outages, database failover, and mail delivery failures.', 'CANCELLED', 'LOW', 'Security Hardening', NULL, NOW() + INTERVAL '20 days', NOW() - INTERVAL '8 days', NOW() - INTERVAL '3 days'),
    ('Add audit log for role changes', 'Record who changed a user role, when, and from which role to which role.', 'OPEN', 'MEDIUM', 'Security Hardening', 'admin@test.com', NULL, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')
) AS v(title, description, status, priority, project_name, assignee_email, due_date, created_at, updated_at)
JOIN projects p ON p.name = v.project_name
WHERE NOT EXISTS (SELECT 1 FROM tickets t WHERE t.title = v.title);

INSERT INTO comments (ticket_id, author_id, body, created_at, updated_at)
SELECT t.id, u.id, v.body, v.created_at, v.updated_at
FROM (VALUES
    ('Add SSO button to customer portal', 'pm2@test.com', 'The identity provider team confirmed the metadata URL. Please wire it up behind a feature flag.', NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
    ('Add SSO button to customer portal', 'member2@test.com', 'Started on the frontend button. Waiting for the staging client ID.', NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
    ('Fix broken avatar upload', 'member@test.com', 'The upload fails only for images larger than 5 MB. Reproduced locally.', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('Fix broken avatar upload', 'pm@test.com', 'Let us also return a proper 413 instead of a generic 500.', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('Add sprint burndown chart', 'pm2@test.com', 'Use the existing sprint endpoint; no new aggregation table needed.', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('Expose ticket throughput API', 'pm@test.com', 'This is now blocking the weekly report. Prioritize it today.', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('Expose ticket throughput API', 'admin2@test.com', 'I can pick this up after the dependency upgrade. ETA tomorrow morning.', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('Cache dashboard aggregates', 'pm2@test.com', 'Measure the current response time first; caching may not be the bottleneck.', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('Design notification preferences screen', 'member2@test.com', 'The preferences screen is ready. The API contract is in the ticket description.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('Implement push token registration', 'pm2@test.com', 'Token registration needs to be idempotent. Please add a uniqueness check.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('Upgrade vulnerable dependencies', 'admin2@test.com', 'Two transitive dependencies are still flagged. I am checking compatibility now.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('Add audit log for role changes', 'admin@test.com', 'We need this before the next access review. Keep the audit entries immutable.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days')
) AS v(ticket_title, author_email, body, created_at, updated_at)
JOIN tickets t ON t.title = v.ticket_title
JOIN users u ON u.email = v.author_email
WHERE NOT EXISTS (
    SELECT 1
    FROM comments c
    WHERE c.ticket_id = t.id
      AND c.author_id = u.id
      AND c.body = v.body
);

INSERT INTO notification_log (user_id, ticket_id, digest_date, batch_id, sent_at)
SELECT u.id, t.id, v.digest_date, v.batch_id, v.sent_at
FROM (VALUES
    ('pm@test.com', 'Optimize N+1 queries in ticket list', CURRENT_DATE - 1, 'seed-digest-01', NOW() - INTERVAL '1 day'),
    ('member@test.com', 'Fix login bug on iOS', CURRENT_DATE - 2, 'seed-digest-02', NOW() - INTERVAL '2 days'),
    ('pm2@test.com', 'Expose ticket throughput API', CURRENT_DATE - 1, 'seed-digest-01', NOW() - INTERVAL '1 day')
) AS v(user_email, ticket_title, digest_date, batch_id, sent_at)
JOIN users u ON u.email = v.user_email
JOIN tickets t ON t.title = v.ticket_title
ON CONFLICT (user_id, ticket_id, digest_date) DO NOTHING;
