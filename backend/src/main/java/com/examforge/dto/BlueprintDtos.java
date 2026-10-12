package com.examforge.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BlueprintDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlueprintDto {
        private Long id;
        private Long subjectId;
        private String subjectName;
        private String title;
        private Integer totalQuestions;
        private Integer durationMinutes;
        private BigDecimal maxMarks;
        private Integer easyPercentage;
        private Integer mediumPercentage;
        private Integer hardPercentage;
        private Integer easyQuestionCount;
        private Integer mediumQuestionCount;
        private Integer hardQuestionCount;
        private Integer mcqCount;
        private Integer trueFalseCount;
        private Integer shortAnswerCount;
        private BigDecimal marksPerQuestion;
        private String instructions;
        private String status;
        private List<BlueprintLessonBreakdownDto> lessonBreakdowns;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlueprintLessonBreakdownDto {
        private Long lessonId;
        private String lessonTitle;
        private Integer targetPercentage;
        private Integer questionCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBlueprintRequest {
        @NotNull(message = "Subject ID is required")
        private Long subjectId;

        @NotBlank(message = "Title is required")
        private String title;

        @NotNull(message = "Total questions is required")
        @Min(value = 1, message = "Total questions must be at least 1")
        @Max(value = 100, message = "Total questions cannot exceed 100")
        private Integer totalQuestions;

        @NotNull(message = "Duration in minutes is required")
        @Min(value = 5, message = "Duration must be at least 5 minutes")
        private Integer durationMinutes;

        @NotNull(message = "Max marks is required")
        private BigDecimal maxMarks;

        @NotNull(message = "Easy percentage is required")
        @Min(0) @Max(100)
        private Integer easyPercentage;

        @NotNull(message = "Medium percentage is required")
        @Min(0) @Max(100)
        private Integer mediumPercentage;

        @NotNull(message = "Hard percentage is required")
        @Min(0) @Max(100)
        private Integer hardPercentage;

        @NotNull(message = "MCQ count is required")
        @Min(0)
        private Integer mcqCount;

        @NotNull(message = "True/False count is required")
        @Min(0)
        private Integer trueFalseCount;

        @NotNull(message = "Short answer count is required")
        @Min(0)
        private Integer shortAnswerCount;

        private BigDecimal marksPerQuestion;

        private String instructions;

        @NotEmpty(message = "At least one lesson must be selected")
        private List<LessonSelectionItem> lessonSelections;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonSelectionItem {
        @NotNull(message = "Lesson ID is required")
        private Long lessonId;

        @NotNull(message = "Target percentage is required")
        @Min(1) @Max(100)
        private Integer targetPercentage;
    }
}
