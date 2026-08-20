-- Add verification_tokens table for email verification and password reset
CREATE TABLE verification_tokens (
    id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE,
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    user_id UUID NOT NULL,
    token VARCHAR(100) NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT pk_verification_tokens PRIMARY KEY (id),
    CONSTRAINT uk_verification_tokens_token UNIQUE (token),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_verification_token ON verification_tokens (token);
CREATE INDEX idx_verification_user_type ON verification_tokens (user_id, token_type);
