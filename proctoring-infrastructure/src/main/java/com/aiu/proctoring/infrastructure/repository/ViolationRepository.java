package com.aiu.proctoring.infrastructure.repository;

import com.aiu.proctoring.domain.model.Violation;
import com.aiu.proctoring.domain.value.ExamSessionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ViolationRepository extends JpaRepository<Violation, UUID> {
    List<Violation> findBySessionId(ExamSessionId sessionId);
    List<Violation> findBySessionIdAndType(ExamSessionId sessionId, String type);
    Long countBySessionIdAndDetectedAtBetween(ExamSessionId sessionId, LocalDateTime start, LocalDateTime end);
    List<Violation> findBySessionIdOrderByDetectedAtDesc(ExamSessionId sessionId);
    long countBySessionId(ExamSessionId sessionId);
}
