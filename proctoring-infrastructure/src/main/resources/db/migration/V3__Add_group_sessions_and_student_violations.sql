-- V3__Add_group_sessions_and_student_violations.sql
-- Add support for group exam sessions and student-specific violations

-- Make student_id optional in exam_sessions for group sessions
ALTER TABLE exam_sessions ALTER COLUMN student_id DROP NOT NULL;

-- Add group name for group sessions
ALTER TABLE exam_sessions ADD COLUMN group_name VARCHAR(200);

-- Create participants table for group sessions
CREATE TABLE exam_session_participants (
    session_id UUID NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id, student_id)
);

CREATE INDEX idx_exam_session_participants_session ON exam_session_participants(session_id);
CREATE INDEX idx_exam_session_participants_student ON exam_session_participants(student_id);

-- Add student_id to violations for student-specific violations
ALTER TABLE violations ADD COLUMN student_id UUID REFERENCES users(id);

CREATE INDEX idx_violations_student ON violations(student_id);

-- Update existing violations to set student_id from session
UPDATE violations SET student_id = exam_sessions.student_id
FROM exam_sessions WHERE violations.session_id = exam_sessions.id;

-- Make student_id not null for violations (after update)
ALTER TABLE violations ALTER COLUMN student_id SET NOT NULL;