package com.examforge.dto;

import com.examforge.entity.Difficulty;
import com.examforge.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class QuestionDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDto {
        private Long id;
        private Long subjectId;
        private Long lessonId;
        private String lessonTitle;
        private String questionText;
        private QuestionType questionType;
        private Difficulty difficulty;
        private BigDecimal marks;
        private String explanation;
        private String sourceReference;
        private Boolean isAiGenerated;
        private List<OptionDto> options;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDto {
        private Long id;
        private String optionLabel;
        private String optionText;
        private Boolean isCorrect;
        private Integer orderIndex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateQuestionRequest {
        @NotNull(message = "Subject ID is required")
        private Long subjectId;
        @NotNull(message = "Lesson ID is required")
        private Long lessonId;
        @NotBlank(message = "Question text is required")
        private String questionText;
        @NotNull(message = "Question type is required")
        private QuestionType questionType;
        @NotNull(message = "Difficulty is required")
        private Difficulty difficulty;
        private BigDecimal marks;
        private String explanation;
        private String sourceReference;
        private List<OptionDto> options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateQuestionsRequest {
        @NotNull(message = "Blueprint ID is required")
        private Long blueprintId;
        private String additionalInstructions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegenerateQuestionRequest {
        @NotNull(message = "Question ID is required")
        private Long questionId;
        private String instructions;
    }
}
