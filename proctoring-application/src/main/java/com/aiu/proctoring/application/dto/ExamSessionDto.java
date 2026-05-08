package com.aiu.proctoring.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Exam session DTO for responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSessionDto {
    private String id;
    private String studentId;
    private String studentName;
    private List<String> participantIds;
    private List<String> participantNames;
    private String groupName;
    private String proctorId;
    private String proctorName;
    private String disciplineCode;
    private String disciplineName;
    private String examToken;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private String status;
    private Integer violationCount;
    private Double averageConfidence;
    private String aiModelUsed;
    private LocalDateTime createdAt;
}
