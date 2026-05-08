package com.aiu.proctoring.infrastructure.repository;

import com.aiu.proctoring.domain.model.ExamSession;
import com.aiu.proctoring.domain.value.ExamSessionId;
import com.aiu.proctoring.domain.value.UserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, ExamSessionId>, JpaSpecificationExecutor<ExamSession> {
    List<ExamSession> findByStudentId(UserId studentId);
    List<ExamSession> findByProctorId(UserId proctorId);
    List<ExamSession> findByStatus(ExamSession.Status status);
    Optional<ExamSession> findByExamToken(String examToken);
    List<ExamSession> findByScheduledStartBetween(LocalDateTime start, LocalDateTime end);
    boolean existsByExamToken(String examToken);
}
