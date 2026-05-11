package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.*;
import com.aiu.proctoring.application.dto.ViolationDto;
import com.aiu.proctoring.application.port.ExamSessionService;
import com.aiu.proctoring.domain.exception.DomainException;
import com.aiu.proctoring.domain.model.ExamSession;
import com.aiu.proctoring.domain.model.User;
import com.aiu.proctoring.domain.model.Violation;
import com.aiu.proctoring.domain.value.ExamSessionId;
import com.aiu.proctoring.domain.value.UserId;
import com.aiu.proctoring.infrastructure.repository.ExamSessionRepository;
import com.aiu.proctoring.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ExamSessionDto createSession(CreateSessionRequest request, String proctorId) {
        User proctor = userRepository.findById(UserId.from(proctorId))
            .orElseThrow(() -> new DomainException("Proctor not found"));

        if (!proctor.getRole().equals(User.Role.PROCTOR) && !proctor.getRole().equals(User.Role.ADMIN)) {
            throw new DomainException("Only PROCTOR or ADMIN can create sessions");
        }

        List<User> participants = request.getStudentIds().stream()
            .map(id -> userRepository.findById(UserId.from(id))
                .orElseThrow(() -> new DomainException("Student not found: " + id)))
            .collect(Collectors.toList());

        for (User participant : participants) {
            if (!participant.getRole().equals(User.Role.STUDENT)) {
                throw new DomainException("User is not a STUDENT: " + participant.getId());
            }
        }

        String token = generateExamToken();

        ExamSession session = ExamSession.builder()
            .id(ExamSessionId.generate())
            .student(participants.size() == 1 ? participants.get(0) : null) // For backward compatibility
            .proctor(proctor)
            .groupName(request.getGroupName())
            .participants(participants)
            .disciplineCode(request.getDisciplineCode())
            .disciplineName(request.getDisciplineName())
            .examToken(token)
            .scheduledStart(request.getScheduledStart())
            .scheduledEnd(request.getScheduledEnd())
            .maxViolations(request.getMaxViolations())
            .violationThreshold(request.getViolationThreshold())
            .status(ExamSession.Status.CREATED)
            .build();

        examSessionRepository.save(session);

        return mapToDto(session);
    }

    @Override
    @Transactional
    public ExamSessionDto startSession(String sessionId, String proctorId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(sessionId))
            .orElseThrow(() -> new DomainException("Session not found"));

        if (!session.getProctor().getId().getValue().equals(proctorId)) {
            throw new DomainException("Only the assigned proctor can start this session");
        }

        session.start();
        // session already managed; changes auto-flushed

        return mapToDto(session);
    }

    @Override
    @Transactional
    public void startSessionByToken(StartSessionRequest request, String studentId) {
        ExamSession session = examSessionRepository.findByExamToken(request.getExamToken())
            .orElseThrow(() -> new DomainException("Invalid exam token"));

        boolean isParticipant = session.getParticipants() != null &&
            session.getParticipants().stream()
                .anyMatch(p -> p.getId().getValue().equals(studentId));

        if (!isParticipant) {
            throw new DomainException("Token does not belong to this student");
        }

        if (LocalDateTime.now().isBefore(session.getScheduledStart())) {
            throw new DomainException("Exam has not started yet");
        }

        if (LocalDateTime.now().isAfter(session.getScheduledEnd())) {
            throw new DomainException("Exam has already ended");
        }

        session.start();
        // session already managed; changes auto-flushed
    }

    @Override
    @Transactional
    public ExamSessionDto endSession(EndSessionRequest request, String proctorId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(request.getSessionId()))
            .orElseThrow(() -> new DomainException("Session not found"));

        if (!session.getProctor().getId().getValue().equals(proctorId)) {
            throw new DomainException("Only the assigned proctor can end this session");
        }

        ExamSessionDto dto = mapToDto(session);
        examSessionRepository.delete(session);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSessionDto getSession(String sessionId, String requesterId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(sessionId))
            .orElseThrow(() -> new DomainException("Session not found"));

        User requester = userRepository.findById(UserId.from(requesterId))
            .orElseThrow(() -> new DomainException("User not found"));

        // Check authorization
        boolean isParticipant = session.getParticipants() != null &&
            session.getParticipants().stream()
                .anyMatch(p -> p.getId().getValue().equals(requesterId));

        if (!requester.getRole().equals(User.Role.ADMIN) &&
            !requester.getId().getValue().equals(session.getProctor().getId().getValue()) &&
            !requester.getId().getValue().equals(session.getStudent() != null ? session.getStudent().getId().getValue() : null) &&
            !isParticipant) {
            throw new DomainException("Not authorized to view this session");
        }

        return mapToDto(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSessionDto> listSessions(String proctorId, String statusFilter) {
        User proctor = userRepository.findById(UserId.from(proctorId))
            .orElseThrow(() -> new DomainException("User not found"));

        List<ExamSession> sessions;
        if (proctor.getRole().equals(User.Role.ADMIN) || proctor.getRole().equals(User.Role.SUPER_ADMIN)) {
            sessions = statusFilter != null ?
                examSessionRepository.findAll().stream()
                    .filter(s -> s.getStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList()) :
                examSessionRepository.findAll();
        } else {
            sessions = statusFilter != null ?
                examSessionRepository.findByProctorId(UserId.from(proctorId)).stream()
                    .filter(s -> s.getStatus().name().equalsIgnoreCase(statusFilter))
                    .collect(Collectors.toList()) :
                examSessionRepository.findByProctorId(UserId.from(proctorId));
        }

        return sessions.stream()
            .sorted(this::compareSessions)
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private int compareSessions(ExamSession left, ExamSession right) {
        List<ExamSession.Status> priority = List.of(
            ExamSession.Status.CREATED,
            ExamSession.Status.ACTIVE,
            ExamSession.Status.COMPLETED,
            ExamSession.Status.TERMINATED,
            ExamSession.Status.CANCELLED
        );

        int leftIndex = priority.indexOf(left.getStatus());
        int rightIndex = priority.indexOf(right.getStatus());

        if (leftIndex != rightIndex) {
            return Integer.compare(leftIndex, rightIndex);
        }

        return right.getCreatedAt().compareTo(left.getCreatedAt());
    }

    @Override
    @Transactional
    public void cancelSession(String sessionId, String proctorId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(sessionId))
            .orElseThrow(() -> new DomainException("Session not found"));

        if (!session.getProctor().getId().getValue().equals(proctorId)) {
            throw new DomainException("Only the assigned proctor can cancel this session");
        }

        session.cancel();
        // session already managed; changes auto-flushed
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyToken(String token) {
        examSessionRepository.findByExamToken(token);
    }

    private String generateExamToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ExamSessionDto mapToDto(ExamSession session) {
        int violationCount = session.getViolations() != null ? session.getViolations().size() : 0;
        double avgConfidence = session.getViolations() != null && !session.getViolations().isEmpty() ?
            session.getViolations().stream()
                .mapToDouble(Violation::getConfidence)
                .average()
                .orElse(0.0) : 0.0;

        List<String> participantIds = session.getParticipants() != null ?
            session.getParticipants().stream()
                .map(p -> p.getId().getValue())
                .collect(Collectors.toList()) : List.of();

        List<String> participantNames = session.getParticipants() != null ?
            session.getParticipants().stream()
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .collect(Collectors.toList()) : List.of();

        List<ViolationDto> violationDtos = session.getViolations() != null ?
            session.getViolations().stream()
                .sorted((v1, v2) -> v2.getDetectedAt().compareTo(v1.getDetectedAt()))
                .map(v -> ViolationDto.builder()
                    .id(v.getId() != null ? v.getId().toString() : null)
                    .sessionId(v.getSession().getId().getValue())
                    .studentId(v.getStudent().getId().getValue())
                    .type(v.getType())
                    .confidence(v.getConfidence())
                    .frameTimestamp(v.getFrameTimestamp())
                    .description(v.getDescription())
                    .detectedAt(v.getDetectedAt())
                    .reviewedBy(v.getReviewedBy())
                    .reviewedAt(v.getReviewedAt())
                    .isFalsePositive(v.getIsFalsePositive())
                    .notes(v.getNotes())
                    .build())
                .collect(Collectors.toList()) : List.of();

        return ExamSessionDto.builder()
            .id(session.getId().getValue())
            .studentId(session.getStudent() != null ? session.getStudent().getId().getValue() : null)
            .studentName(session.getStudent() != null ? session.getStudent().getFirstName() + " " + session.getStudent().getLastName() : null)
            .participantIds(participantIds)
            .participantNames(participantNames)
            .groupName(session.getGroupName())
            .proctorId(session.getProctor().getId().getValue())
            .proctorName(session.getProctor().getFirstName() + " " + session.getProctor().getLastName())
            .disciplineCode(session.getDisciplineCode())
            .disciplineName(session.getDisciplineName())
            .examToken(session.getExamToken())
            .scheduledStart(session.getScheduledStart())
            .scheduledEnd(session.getScheduledEnd())
            .actualStart(session.getActualStart())
            .actualEnd(session.getActualEnd())
            .status(session.getStatus().name())
            .violationCount(violationCount)
            .averageConfidence(avgConfidence)
            .aiModelUsed(session.getAiModelUsed())
            .createdAt(session.getCreatedAt())
            .violations(violationDtos)
            .build();
    }
}
