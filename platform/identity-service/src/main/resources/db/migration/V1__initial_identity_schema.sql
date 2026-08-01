CREATE TABLE clients (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    client_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    CONSTRAINT pk_clients PRIMARY KEY (id),
    CONSTRAINT uk_clients_client_id UNIQUE (client_id)
);

CREATE TABLE roles (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE users (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL,
    token_version INTEGER NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    token VARCHAR(512) NOT NULL,
    session_id UUID NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL,
    client_id UUID NOT NULL,
    revoked_at TIMESTAMP(6) WITH TIME ZONE,
    device_name VARCHAR(255),
    ip_address VARCHAR(45),
    user_id UUID NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_client_roles (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    client_id UUID NOT NULL,
    assigned_by UUID,
    assigned_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_user_client_roles PRIMARY KEY (id),
    CONSTRAINT uk_user_client_roles_user_client_role UNIQUE (user_id, client_id, role_id),
    CONSTRAINT fk_user_client_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_client_roles_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_user_client_roles_client FOREIGN KEY (client_id) REFERENCES clients (id)
);

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_session ON refresh_tokens (session_id);

CREATE INDEX idx_user_client_role_user ON user_client_roles (user_id);
CREATE INDEX idx_user_client_role_role ON user_client_roles (role_id);
CREATE INDEX idx_user_client_role_client ON user_client_roles (client_id);
CREATE INDEX idx_user_client ON user_client_roles (user_id, client_id);
