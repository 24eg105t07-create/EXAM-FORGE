package com.examforge.dto;

import com.examforge.entity.Difficulty;
import com.examforge.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ResultDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttemptSummaryDto {
        private Long attemptId;
        private Long scheduleId;
        private String examTitle;
        private String subjectName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private BigDecimal totalScore;
        private BigDecimal maxMarks;
        private BigDecimal percentage;
        private Boolean passed;
        private Boolean isResultReleased;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedResultDto {
        private Long attemptId;
        private Long scheduleId;
        private String examTitle;
        private String subjectName;
        private String studentName;
        private String rollNumber;
        private BigDecimal totalScore;
        private BigDecimal maxMarks;
        private BigDecimal percentage;
        private Boolean passed;
        private int totalQuestions;
        private int correctCount;
        private int incorrectCount;
        private int unansweredCount;
        private Map<String, BreakdownScoreDto> topicBreakdown;
        private Map<String, BreakdownScoreDto> difficultyBreakdown;
        private List<QuestionResultDetailDto> answers;
        private LocalDateTime evaluatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreakdownScoreDto {
        private int totalQuestions;
        private int correctCount;
        private BigDecimal totalMarks;
        private BigDecimal marksObtained;
        private BigDecimal percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResultDetailDto {
        private Long questionId;
        private Integer questionOrder;
        private String questionText;
        private QuestionType questionType;
        private Difficulty difficulty;
        private String lessonTitle;
        private Long selectedOptionId;
        private String selectedOptionLabel;
        private String selectedOptionText;
        private String correctOptionLabel;
        private String correctOptionText;
        private String studentTextAnswer;
        private Boolean isCorrect;
        private BigDecimal marksAwarded;
        private BigDecimal maxMarks;
        private String explanation;
        private String teacherComment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassAnalyticsDto {
        private Long scheduleId;
        private String examTitle;
        private String subjectName;
        private String className;
        private int totalEnrolled;
        private int attemptsCount;
        private int passedCount;
        private BigDecimal passPercentage;
        private BigDecimal averageScore;
        private BigDecimal highestScore;
        private BigDecimal lowestScore;
        private BigDecimal maxMarks;
        private Map<String, BreakdownScoreDto> topicStats;
        private Map<String, BreakdownScoreDto> difficultyStats;
        private List<StudentRankDto> rankings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRankDto {
        private int rank;
        private Long studentId;
        private String studentName;
        private String rollNumber;
        private BigDecimal score;
        private BigDecimal percentage;
        private Boolean passed;
    }
}
