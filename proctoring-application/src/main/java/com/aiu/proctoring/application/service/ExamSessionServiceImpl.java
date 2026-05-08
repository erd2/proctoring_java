package com.aiu.proctoring.application.service;

import com.aiu.proctoring.application.dto.*;
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

        User student = userRepository.findById(UserId.from(request.getStudentId()))
            .orElseThrow(() -> new DomainException("Student not found"));

        if (!student.getRole().equals(User.Role.STUDENT)) {
            throw new DomainException("User is not a STUDENT");
        }

        String token = generateExamToken();

        ExamSession session = ExamSession.builder()
            .id(ExamSessionId.generate())
            .student(student)
            .proctor(proctor)
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
        examSessionRepository.save(session);

        return mapToDto(session);
    }

    @Override
    @Transactional
    public void startSessionByToken(StartSessionRequest request, String studentId) {
        ExamSession session = examSessionRepository.findByExamToken(request.getExamToken())
            .orElseThrow(() -> new DomainException("Invalid exam token"));

        if (!session.getStudent().getId().getValue().equals(studentId)) {
            throw new DomainException("Token does not belong to this student");
        }

        if (LocalDateTime.now().isBefore(session.getScheduledStart())) {
            throw new DomainException("Exam has not started yet");
        }

        if (LocalDateTime.now().isAfter(session.getScheduledEnd())) {
            throw new DomainException("Exam has already ended");
        }

        session.start();
        examSessionRepository.save(session);
    }

    @Override
    @Transactional
    public ExamSessionDto endSession(EndSessionRequest request, String proctorId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(request.getSessionId()))
            .orElseThrow(() -> new DomainException("Session not found"));

        if (!session.getProctor().getId().getValue().equals(proctorId)) {
            throw new DomainException("Only the assigned proctor can end this session");
        }

        session.end();
        examSessionRepository.save(session);

        return mapToDto(session);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamSessionDto getSession(String sessionId, String requesterId) {
        ExamSession session = examSessionRepository.findById(ExamSessionId.from(sessionId))
            .orElseThrow(() -> new DomainException("Session not found"));

        User requester = userRepository.findById(UserId.from(requesterId))
            .orElseThrow(() -> new DomainException("User not found"));

        // Check authorization
        if (!requester.getRole().equals(User.Role.ADMIN) &&
            !requester.getId().getValue().equals(session.getProctor().getId().getValue()) &&
            !requester.getId().getValue().equals(session.getStudent().getId().getValue())) {
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
            .map(this::mapToDto)
            .collect(Collectors.toList());
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
        examSessionRepository.save(session);
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

        return ExamSessionDto.builder()
            .id(session.getId().getValue())
            .studentId(session.getStudent().getId().getValue())
            .studentName(session.getStudent().getFirstName() + " " + session.getStudent().getLastName())
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
            .build();
    }
}
