package com.examforge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AcademicDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentDto {
        private Long id;
        private String code;
        private String name;
        private String description;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDepartmentRequest {
        @NotBlank(message = "Department code is required")
        private String code;
        @NotBlank(message = "Department name is required")
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademicYearDto {
        private Long id;
        private String yearName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAcademicYearRequest {
        @NotBlank(message = "Year name is required")
        private String yearName;
        @NotNull(message = "Start date is required")
        private LocalDate startDate;
        @NotNull(message = "End date is required")
        private LocalDate endDate;
        private Boolean isCurrent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassDto {
        private Long id;
        private Long departmentId;
        private String departmentName;
        private String code;
        private String name;
        private Integer semester;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateClassRequest {
        @NotNull(message = "Department ID is required")
        private Long departmentId;
        @NotBlank(message = "Class code is required")
        private String code;
        @NotBlank(message = "Class name is required")
        private String name;
        @NotNull(message = "Semester is required")
        private Integer semester;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionDto {
        private Long id;
        private Long classId;
        private String className;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSectionRequest {
        @NotNull(message = "Class ID is required")
        private Long classId;
        @NotBlank(message = "Section name is required")
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectDto {
        private Long id;
        private Long departmentId;
        private String departmentName;
        private String code;
        private String name;
        private String description;
        private Integer credits;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSubjectRequest {
        @NotNull(message = "Department ID is required")
        private Long departmentId;
        @NotBlank(message = "Subject code is required")
        private String code;
        @NotBlank(message = "Subject name is required")
        private String name;
        private String description;
        private Integer credits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentEnrollmentDto {
        private Long id;
        private Long studentId;
        private String studentName;
        private String studentEmail;
        private Long sectionId;
        private String sectionName;
        private Long classId;
        private String className;
        private String rollNumber;
        private Long academicYearId;
        private String academicYearName;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollStudentRequest {
        @NotNull(message = "Student ID is required")
        private Long studentId;
        @NotNull(message = "Section ID is required")
        private Long sectionId;
        @NotBlank(message = "Roll number is required")
        private String rollNumber;
        @NotNull(message = "Academic year ID is required")
        private Long academicYearId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherAssignmentDto {
        private Long id;
        private Long teacherId;
        private String teacherName;
        private String teacherEmail;
        private Long subjectId;
        private String subjectName;
        private String subjectCode;
        private Long classId;
        private String className;
        private Long sectionId;
        private String sectionName;
        private Long academicYearId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignTeacherRequest {
        @NotNull(message = "Teacher ID is required")
        private Long teacherId;
        @NotNull(message = "Subject ID is required")
        private Long subjectId;
        @NotNull(message = "Class ID is required")
        private Long classId;
        private Long sectionId;
        @NotNull(message = "Academic year ID is required")
        private Long academicYearId;
    }
}
