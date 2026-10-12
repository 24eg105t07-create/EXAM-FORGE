package com.examforge.ai;

import com.examforge.entity.Difficulty;
import com.examforge.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AiModels {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionGenerationRequest {
        private String subjectName;
        private String subjectCode;
        private List<LessonContext> lessons;
        private int totalQuestions;
        private int easyCount;
        private int mediumCount;
        private int hardCount;
        private int mcqCount;
        private int trueFalseCount;
        private int shortAnswerCount;
        private BigDecimal marksPerQuestion;
        private String additionalInstructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonContext {
        private Long lessonId;
        private String title;
        private Integer unitNumber;
        private int targetQuestionCount;
        private String extractedDocumentContent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedQuestion {
        private Long lessonId;
        private String questionText;
        private QuestionType questionType;
        private Difficulty difficulty;
        private BigDecimal marks;
        private String explanation;
        private String sourceReference;
        private List<GeneratedOption> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedOption {
        private String optionLabel; // A, B, C, D
        private String optionText;
        private boolean isCorrect;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationResult {
        private String provider;
        private boolean success;
        private String rawResponse;
        private String errorMessage;
        private List<GeneratedQuestion> questions;
    }
}
