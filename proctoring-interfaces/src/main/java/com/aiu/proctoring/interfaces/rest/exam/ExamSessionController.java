package com.aiu.proctoring.interfaces.rest.exam;

import com.aiu.proctoring.application.dto.*;
import com.aiu.proctoring.application.port.ExamSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exam session management endpoints for PROCTOR and STUDENT roles.
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    /**
     * Create a new proctoring session (PROCTOR only).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExamSessionDto>> createSession(
        @Valid @RequestBody CreateSessionRequest request,
        @RequestAttribute("userId") String userId
    ) {
        try {
            ExamSessionDto session = examSessionService.createSession(request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session created", session));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to create session: " + e.getMessage()));
        }
    }

    /**
     * Start a proctoring session manually (PROCTOR only).
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<ApiResponse<ExamSessionDto>> startSession(
        @PathVariable String sessionId,
        @RequestAttribute("userId") String userId
    ) {
        try {
            ExamSessionDto session = examSessionService.startSession(sessionId, userId);
            return ResponseEntity.ok(ApiResponse.success("Session started", session));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to start session: " + e.getMessage()));
        }
    }

    /**
     * Student starts session using exam token.
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<ExamSessionDto>> startSessionByToken(
        @Valid @RequestBody StartSessionRequest request,
        @RequestAttribute("userId") String studentId
    ) {
        try {
            examSessionService.startSessionByToken(request, studentId);
            return ResponseEntity.ok(ApiResponse.success("Session started", null));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to start session: " + e.getMessage()));
        }
    }

    /**
     * End a proctoring session (PROCTOR only).
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<ApiResponse<ExamSessionDto>> endSession(
        @PathVariable String sessionId,
        @Valid @RequestBody EndSessionRequest request,
        @RequestAttribute("userId") String userId
    ) {
        try {
            ExamSessionDto session = examSessionService.endSession(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Session ended", session));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to end session: " + e.getMessage()));
        }
    }

    /**
     * Get session details by ID.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<ExamSessionDto>> getSession(
        @PathVariable String sessionId,
        @RequestAttribute("userId") String userId
    ) {
        try {
            ExamSessionDto session = examSessionService.getSession(sessionId, userId);
            return ResponseEntity.ok(ApiResponse.success(null, session));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Session not found"));
        }
    }

    /**
     * List sessions (filterable by status).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamSessionDto>>> listSessions(
        @RequestAttribute("userId") String userId,
        @RequestParam(required = false) String status
    ) {
        try {
            List<ExamSessionDto> sessions = examSessionService.listSessions(userId, status);
            return ResponseEntity.ok(ApiResponse.success(null, sessions));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to fetch sessions: " + e.getMessage()));
        }
    }

    /**
     * Cancel a session (PROCTOR only).
     */
    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelSession(
        @PathVariable String sessionId,
        @RequestAttribute("userId") String userId
    ) {
        try {
            examSessionService.cancelSession(sessionId, userId);
            return ResponseEntity.ok(ApiResponse.success("Session cancelled", null));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to cancel session: " + e.getMessage()));
        }
    }

    /**
     * Verify exam token validity (for student login).
     */
    @GetMapping("/verify-token")
    public ResponseEntity<ApiResponse<Void>> verifyToken(@RequestParam String token) {
        try {
            examSessionService.verifyToken(token);
            return ResponseEntity.ok(ApiResponse.success("Token is valid", null));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid exam token"));
        }
    }
}
