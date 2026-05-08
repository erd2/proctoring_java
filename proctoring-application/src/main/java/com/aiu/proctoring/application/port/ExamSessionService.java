package com.aiu.proctoring.application.port;

import com.aiu.proctoring.application.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Port interface for exam session management operations.
 */
public interface ExamSessionService {

    ExamSessionDto createSession(CreateSessionRequest request, String proctorId);

    ExamSessionDto startSession(String sessionId, String proctorId);

    void startSessionByToken(StartSessionRequest request, String studentId);

    ExamSessionDto endSession(EndSessionRequest request, String proctorId);

    ExamSessionDto getSession(String sessionId, String requesterId);

    List<ExamSessionDto> listSessions(String proctorId, String status);

    void cancelSession(String sessionId, String proctorId);

    void verifyToken(String token);
}
