INSERT INTO roles (name) VALUES ('ADMIN'), ('CUSTOMER');

INSERT INTO users (email, first_name, last_name, password, is_deleted) VALUES
    ('test@example.com', 'Test', 'User', 'password', FALSE);

INSERT INTO users_roles (user_id, role_id)
SELECT
    (SELECT id FROM users WHERE email = 'test@example.com'),
    (SELECT id FROM roles WHERE name = 'CUSTOMER');
