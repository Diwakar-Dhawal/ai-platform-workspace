-- Seed the insighttube client
INSERT INTO clients (id, created_at, updated_at, client_id, name, description)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    NOW(),
    NOW(),
    'insighttube',
    'InsightTube',
    'YouTube analytics and content management application'
)
ON CONFLICT (client_id) DO NOTHING;

-- Seed an admin user (password: Admin@1234, BCrypt encoded)
INSERT INTO users (id, created_at, updated_at, username, email, password_hash, status, email_verified, token_version)
VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    NOW(),
    NOW(),
    'admin',
    'admin@insighttube.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ACTIVE',
    true,
    0
)
ON CONFLICT (username) DO NOTHING;

-- Assign ADMIN role to the admin user for the insighttube client
INSERT INTO user_client_roles (id, created_at, updated_at, user_id, role_id, client_id, assigned_at)
SELECT
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33',
    NOW(),
    NOW(),
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    r.id,
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    NOW()
FROM roles r
WHERE r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_client_roles ucr
    WHERE ucr.user_id = 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22'
    AND ucr.client_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'
    AND ucr.role_id = r.id
);
