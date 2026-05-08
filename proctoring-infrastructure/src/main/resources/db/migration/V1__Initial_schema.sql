-- V1__Initial_schema.sql
-- Initial database schema for Proctoring System

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(100) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'PROCTOR', 'ADMIN', 'SUPER_ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    google_id VARCHAR(100) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Exam sessions table
CREATE TABLE exam_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES users(id),
    proctor_id UUID NOT NULL REFERENCES users(id),
    discipline_code VARCHAR(100) NOT NULL,
    discipline_name VARCHAR(200) NOT NULL,
    exam_token VARCHAR(64) NOT NULL UNIQUE,
    scheduled_start TIMESTAMP NOT NULL,
    scheduled_end TIMESTAMP NOT NULL,
    actual_start TIMESTAMP,
    actual_end TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED', 'ACTIVE', 'COMPLETED', 'TERMINATED', 'CANCELLED')),
    max_violations INTEGER DEFAULT 5,
    violation_threshold DOUBLE PRECISION DEFAULT 0.7,
    ai_model_used VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_exam_sessions_student ON exam_sessions(student_id);
CREATE INDEX idx_exam_sessions_proctor ON exam_sessions(proctor_id);
CREATE INDEX idx_exam_sessions_status ON exam_sessions(status);
CREATE INDEX idx_exam_sessions_token ON exam_sessions(exam_token);
CREATE INDEX idx_exam_sessions_schedule ON exam_sessions(scheduled_start, scheduled_end);

-- Violations table
CREATE TABLE violations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    violation_type VARCHAR(50) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    frame_timestamp BIGINT NOT NULL,
    description VARCHAR(500),
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP,
    is_false_positive BOOLEAN DEFAULT false,
    notes VARCHAR(500)
);

CREATE INDEX idx_violations_session ON violations(session_id);
CREATE INDEX idx_violations_type ON violations(violation_type);
CREATE INDEX idx_violations_detected ON violations(detected_at);

-- User permissions for fine-grained RBAC
CREATE TABLE user_permissions (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission)
);

-- Audit logs for compliance and tracking
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100),
    action VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100),
    actor_type VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    request_uri VARCHAR(500),
    http_method VARCHAR(10),
    old_values JSONB,
    new_values JSONB,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- User quotas for rate limiting
CREATE TABLE user_quotas (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    ai_request_limit INTEGER NOT NULL DEFAULT 60,
    ai_requests_used INTEGER NOT NULL DEFAULT 0,
    reset_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 hour')
);

CREATE INDEX idx_user_quotas_reset ON user_quotas(reset_at);

-- Insert default quotas for all users via trigger or batch job
