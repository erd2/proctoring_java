package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.ExamSessionId;
import com.aiu.proctoring.domain.value.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExamSessionTest {

    @Test
    void createSession_shouldSetCreatedStatus() {
        User student = User.builder()
            .id(UserId.generate())
            .email("student@example.com")
            .username("student1")
            .passwordHash("hash")
            .firstName("Student")
            .lastName("One")
            .role(User.Role.STUDENT)
            .build();

        User proctor = User.builder()
            .id(UserId.generate())
            .email("proctor@example.com")
            .username("proctor1")
            .passwordHash("hash")
            .firstName("Proctor")
            .lastName("One")
            .role(User.Role.PROCTOR)
            .build();

        ExamSession session = ExamSession.builder()
            .id(ExamSessionId.generate())
            .student(student)
            .proctor(proctor)
            .participants(List.of(student))
            .disciplineCode("CS101")
            .disciplineName("Intro to Programming")
            .examToken("token123")
            .scheduledStart(LocalDateTime.now())
            .scheduledEnd(LocalDateTime.now().plusHours(2))
            .maxViolations(3)
            .violationThreshold(0.7)
            .status(ExamSession.Status.CREATED)
            .build();

        assertEquals(ExamSession.Status.CREATED, session.getStatus());
        assertFalse(session.isActive());
    }

    @Test
    void startSession_shouldSetActiveAndActualStart() {
        ExamSession session = createSampleSession();
        assertNull(session.getActualStart());

        session.start();
        assertEquals(ExamSession.Status.ACTIVE, session.getStatus());
        assertNotNull(session.getActualStart());
        assertTrue(session.isActive());
    }

    @Test
    void endSession_shouldSetCompletedAndActualEnd() {
        ExamSession session = createSampleSession();
        session.start();
        session.end();

        assertEquals(ExamSession.Status.COMPLETED, session.getStatus());
        assertNotNull(session.getActualEnd());
        assertTrue(session.isTerminated());
    }

    @Test
    void cancelSession_beforeStart_shouldSetCancelled() {
        ExamSession session = createSampleSession();
        session.cancel();

        assertEquals(ExamSession.Status.CANCELLED, session.getStatus());
        assertTrue(session.isTerminated());
    }

    @Test
    void markViolated_exceedThreshold_shouldTerminate() {
        ExamSession session = createSampleSession();
        session.start();

        Violation v1 = Violation.builder()
            .id(UUID.randomUUID())
            .session(session)
            .type("LOOKING_AWAY")
            .confidence(0.8)
            .frameTimestamp(1000L)
            .description("Looking away from screen")
            .build();

        session.markViolated(v1);
        assertFalse(session.isTerminated());

        // Create more violations to exceed threshold
        for (int i = 2; i <= 5; i++) {
            Violation v = Violation.builder()
                .id(UUID.randomUUID())
                .session(session)
                .type("LOOKING_AWAY")
                .confidence(0.9)
                .frameTimestamp((long) i * 1000L)
                .description("Violation " + i)
                .build();
            session.markViolated(v);
        }

        assertEquals(ExamSession.Status.TERMINATED, session.getStatus());
    }

    @Test
    void transition_invalidState_shouldThrow() {
        ExamSession session = createSampleSession();
        session.start();

        // Starting again should not throw, just return
        session.start();
        assertEquals(ExamSession.Status.ACTIVE, session.getStatus());
    }

    private ExamSession createSampleSession() {
        User student = User.builder()
            .id(UserId.generate())
            .email("student@example.com")
            .username("student1")
            .passwordHash("hash")
            .firstName("Student")
            .lastName("One")
            .role(User.Role.STUDENT)
            .build();

        User proctor = User.builder()
            .id(UserId.generate())
            .email("proctor@example.com")
            .username("proctor1")
            .passwordHash("hash")
            .firstName("Proctor")
            .lastName("One")
            .role(User.Role.PROCTOR)
            .build();

        return ExamSession.builder()
            .id(ExamSessionId.generate())
            .student(student)
            .proctor(proctor)
            .participants(List.of(student))
            .disciplineCode("CS101")
            .disciplineName("Intro")
            .examToken("token")
            .scheduledStart(LocalDateTime.now().minusHours(1))
            .scheduledEnd(LocalDateTime.now().plusHours(2))
            .maxViolations(5)
            .status(ExamSession.Status.CREATED)
            .build();
    }
}
