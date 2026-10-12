package com.examforge.dto;

import com.examforge.entity.AttemptStatus;
import com.examforge.entity.QuestionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExamTakingDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentExamSessionDto {
        private Long attemptId;
        private Long scheduleId;
        private String examTitle;
        private String subjectName;
        private Integer durationMinutes;
        private BigDecimal maxMarks;
        private String instructions;
        private LocalDateTime startTime;
        private LocalDateTime deadlineTime;
        private long remainingSeconds;
        private AttemptStatus status;
        private List<StudentQuestionDto> questions;
        private Map<Long, SavedAnswerDto> savedAnswers; // questionId -> answer
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentQuestionDto {
        private Long questionId;
        private Integer questionOrder;
        private String questionText;
        private QuestionType questionType;
        private BigDecimal marks;
        private List<StudentOptionDto> options; // NO isCorrect or explanation!
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentOptionDto {
        private Long id;
        private String optionLabel;
        private String optionText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavedAnswerDto {
        private Long questionId;
        private Long selectedOptionId;
        private String textAnswer;
        private Boolean isMarkedForReview;
        private LocalDateTime lastSavedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveAnswerRequest {
        @NotNull(message = "Question ID is required")
        private Long questionId;
        private Long selectedOptionId;
        private String textAnswer;
        private Boolean isMarkedForReview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitExamResponse {
        private Long attemptId;
        private AttemptStatus status;
        private LocalDateTime submittedAt;
        private String message;
        private BigDecimal score; // populated if results released immediately
        private BigDecimal maxMarks;
    }
}
