package com.examforge.dto;

import com.examforge.entity.ProcessingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DocumentDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDto {
        private Long id;
        private Long subjectId;
        private String subjectName;
        private Integer unitNumber;
        private String title;
        private String description;
        private Integer orderIndex;
        private int documentCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLessonRequest {
        @NotNull(message = "Subject ID is required")
        private Long subjectId;
        @NotNull(message = "Unit number is required")
        private Integer unitNumber;
        @NotBlank(message = "Lesson title is required")
        private String title;
        private String description;
        private Integer orderIndex;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private Long id;
        private Long lessonId;
        private String lessonTitle;
        private Long subjectId;
        private String subjectName;
        private String fileName;
        private Long fileSize;
        private String mimeType;
        private ProcessingStatus processingStatus;
        private String extractedSnippet;
        private String errorMessage;
        private LocalDateTime createdAt;
    }
}
