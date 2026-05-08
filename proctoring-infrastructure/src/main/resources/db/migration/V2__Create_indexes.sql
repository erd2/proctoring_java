-- V2__Create_indexes.sql
-- Additional indexes for performance optimization

-- Composite index for violation queries
CREATE INDEX IF NOT EXISTS idx_violations_session_type ON violations(session_id, violation_type);
CREATE INDEX IF NOT EXISTS idx_violations_session_detected ON violations(session_id, detected_at DESC);

-- Covering index for session queries
CREATE INDEX IF NOT EXISTS idx_exam_sessions_proctor_status ON exam_sessions(proctor_id, status);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_student_status ON exam_sessions(student_id, status);

-- Partial index for active sessions only
CREATE INDEX IF NOT EXISTS idx_exam_sessions_active ON exam_sessions(id)
WHERE status = 'ACTIVE';

-- GIN index for JSONB fields in audit logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_new_values ON audit_logs USING GIN (new_values);
CREATE INDEX IF NOT EXISTS idx_audit_logs_old_values ON audit_logs USING GIN (old_values);

-- Unique constraint to ensure one active session per student at a time
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_student_session ON exam_sessions(student_id)
WHERE status = 'ACTIVE';
