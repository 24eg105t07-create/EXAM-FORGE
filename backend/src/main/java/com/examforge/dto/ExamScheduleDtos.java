package com.examforge.dto;

import com.examforge.entity.ResultReleasePolicy;
import com.examforge.entity.ScheduleStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamScheduleDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamScheduleDto {
        private Long id;
        private Long examPaperId;
        private String examPaperTitle;
        private Long subjectId;
        private String subjectName;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private String title;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer durationMinutes;
        private BigDecimal maxMarks;
        private String instructions;
        private ResultReleasePolicy resultReleasePolicy;
        private Boolean isResultsReleased;
        private ScheduleStatus status;
        private String cancelledReason;
        private int eligibleStudentsCount;
        private int attemptsCount;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateScheduleRequest {
        @NotNull(message = "Exam paper ID is required")
        private Long examPaperId;

        @NotNull(message = "Class ID is required")
        private Long classId;

        private Long sectionId;

        @NotBlank(message = "Title is required")
        private String title;

        @NotNull(message = "Start time is required")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        private LocalDateTime endTime;

        @NotNull(message = "Duration in minutes is required")
        private Integer durationMinutes;

        private BigDecimal maxMarks;

        private String instructions;

        @NotNull(message = "Result release policy is required")
        private ResultReleasePolicy resultReleasePolicy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelScheduleRequest {
        @NotBlank(message = "Cancellation reason is required")
        private String reason;
    }
}
