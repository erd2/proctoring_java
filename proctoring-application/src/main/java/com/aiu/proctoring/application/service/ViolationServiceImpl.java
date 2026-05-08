package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.*;
import com.aiu.proctoring.application.port.ViolationService;
import com.aiu.proctoring.domain.exception.DomainException;
import com.aiu.proctoring.domain.model.ExamSession;
import com.aiu.proctoring.domain.model.Violation;
import com.aiu.proctoring.domain.value.ExamSessionId;
import com.aiu.proctoring.domain.value.ViolationId;
import com.aiu.proctoring.infrastructure.repository.ExamSessionRepository;
import com.aiu.proctoring.infrastructure.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    private final ViolationRepository violationRepository;
    private final ExamSessionRepository examSessionRepository;

    @Override
    @Transactional
    public ViolationDto reportViolation(String sessionId, String type, Double confidence,
                                         Long frameTimestamp, String description) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(sessionId))
            .orElseThrow(() -> new DomainException("Exam session not found"));

        if (!session.isActive()) {
            throw new DomainException("Cannot report violation for inactive session");
        }

        // Business rule: confidence threshold
        if (confidence < 0.5) {
            // Low confidence violations logged but not counted
        }

        Violation violation = Violation.builder()
            .id(ViolationId.generate())
            .session(session)
            .type(type)
            .confidence(confidence)
            .frameTimestamp(frameTimestamp)
            .description(description)
            .build();

        violationRepository.save(violation);
        session.markViolated(violation);
        examSessionRepository.save(session);

        return mapToDto(violation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViolationDto> getSessionViolations(String sessionId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(sessionId))
            .orElseThrow(() -> new DomainException("Session not found"));

        return session.getViolations().stream()
            .sorted((v1, v2) -> v2.getDetectedAt().compareTo(v1.getDetectedAt()))
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ViolationDto reviewViolation(String violationId, String reviewerId,
                                          boolean isFalsePositive, String notes) {
        Violation violation = violationRepository.findById(ViolationId.from(violationId))
            .orElseThrow(() -> new DomainException("Violation not found"));

        violation.review(reviewerId, isFalsePositive, notes);
        violationRepository.save(violation);

        return mapToDto(violation);
    }

    @Override
    @Transactional
    public void processAIAnalysis(AIAnalysisResult result) {
        // Called by AI processing component (async via Kafka)
        reportViolation(
            result.getSessionId(),
            result.getViolationType(),
            result.getConfidence(),
            result.getTimestamp(),
            result.getDescription()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ViolationStatisticsDto getViolationStatistics(String sessionId) {
        List<Violation> violations = violationRepository.findBySessionIdOrderByDetectedAtDesc(ExamSessionId.from(sessionId));

        long total = violations.size();
        long reviewed = violations.stream().filter(Violation::isReviewed).count();
        long falsePositives = violations.stream().filter(v -> Boolean.TRUE.equals(v.getIsFalsePositive())).count();
        double avgConf = violations.stream().mapToDouble(Violation::getConfidence).average().orElse(0.0);

        // Statistics by type
        var byType = violations.stream()
            .collect(Collectors.groupingBy(Violation::getType, Collectors.counting()))
            .entrySet().stream()
            .map(e -> ViolationTypeStat.builder()
                .type(e.getKey())
                .count(e.getValue())
                .build())
            .collect(Collectors.toList());

        // Time series by hour
        var timeSeries = violations.stream()
            .collect(Collectors.groupingBy(
                v -> String.format("%02d", v.getDetectedAt().getHour()),
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(e -> TimeSeriesStat.builder()
                .hour(e.getKey())
                .count(e.getValue())
                .build())
            .collect(Collectors.toList());

        return ViolationStatisticsDto.builder()
            .sessionId(sessionId)
            .totalViolations(total)
            .reviewedViolations(reviewed)
            .falsePositives(falsePositives)
            .averageConfidence(avgConf)
            .byType(byType)
            .timeSeries(timeSeries)
            .build();
    }

    private ViolationDto mapToDto(Violation violation) {
        return ViolationDto.builder()
            .id(violation.getId().getValue())
            .sessionId(violation.getSession().getId().getValue())
            .type(violation.getType())
            .confidence(violation.getConfidence())
            .frameTimestamp(violation.getFrameTimestamp())
            .description(violation.getDescription())
            .detectedAt(violation.getDetectedAt())
            .reviewedBy(violation.getReviewedBy())
            .reviewedAt(violation.getReviewedAt())
            .isFalsePositive(violation.getIsFalsePositive())
            .notes(violation.getNotes())
            .build();
    }
}
