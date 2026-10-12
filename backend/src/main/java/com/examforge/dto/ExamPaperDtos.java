package com.examforge.dto;

import com.examforge.entity.PaperStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExamPaperDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamPaperDto {
        private Long id;
        private Long blueprintId;
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private Long creatorId;
        private String creatorName;
        private String title;
        private Integer totalQuestions;
        private BigDecimal maxMarks;
        private Integer durationMinutes;
        private PaperStatus status;
        private Integer version;
        private Boolean isLocked;
        private LocalDateTime approvedAt;
        private String approvedByName;
        private List<PaperQuestionDto> questions;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaperQuestionDto {
        private Long id;
        private Integer questionOrder;
        private BigDecimal marks;
        private QuestionDtos.QuestionDto question;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePaperQuestionOrderRequest {
        @NotNull
        private List<QuestionOrderItem> questionOrders;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionOrderItem {
        private Long questionId;
        private Integer questionOrder;
    }
}
