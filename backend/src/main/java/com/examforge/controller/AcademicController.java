package com.examforge.controller;

import com.examforge.dto.AcademicDtos;
import com.examforge.service.AcademicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Academic Structure", description = "Departments, classes, sections, student enrollments, and teacher assignments")
public class AcademicController {

    private final AcademicService academicService;

    // --- Departments ---
    @GetMapping("/academic/departments")
    @Operation(summary = "List all departments")
    public ResponseEntity<List<AcademicDtos.DepartmentDto>> getDepartments() {
        return ResponseEntity.ok(academicService.getAllDepartments());
    }

    @PostMapping("/management/departments")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Create department (Management only)")
    public ResponseEntity<AcademicDtos.DepartmentDto> createDepartment(@Valid @RequestBody AcademicDtos.CreateDepartmentRequest request) {
        return new ResponseEntity<>(academicService.createDepartment(request), HttpStatus.CREATED);
    }

    // --- Academic Years ---
    @GetMapping("/academic/years")
    @Operation(summary = "List all academic years")
    public ResponseEntity<List<AcademicDtos.AcademicYearDto>> getAcademicYears() {
        return ResponseEntity.ok(academicService.getAllAcademicYears());
    }

    @PostMapping("/management/academic-years")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Create academic year (Management only)")
    public ResponseEntity<AcademicDtos.AcademicYearDto> createAcademicYear(@Valid @RequestBody AcademicDtos.CreateAcademicYearRequest request) {
        return new ResponseEntity<>(academicService.createAcademicYear(request), HttpStatus.CREATED);
    }

    // --- Classes & Sections ---
    @GetMapping("/academic/classes")
    @Operation(summary = "List all academic classes")
    public ResponseEntity<List<AcademicDtos.ClassDto>> getClasses() {
        return ResponseEntity.ok(academicService.getAllClasses());
    }

    @PostMapping("/management/classes")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Create academic class (Management only)")
    public ResponseEntity<AcademicDtos.ClassDto> createClass(@Valid @RequestBody AcademicDtos.CreateClassRequest request) {
        return new ResponseEntity<>(academicService.createClass(request), HttpStatus.CREATED);
    }

    @GetMapping("/academic/classes/{classId}/sections")
    @Operation(summary = "List sections for class")
    public ResponseEntity<List<AcademicDtos.SectionDto>> getSections(@PathVariable Long classId) {
        return ResponseEntity.ok(academicService.getSectionsByClass(classId));
    }

    @PostMapping("/management/sections")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Create class section (Management only)")
    public ResponseEntity<AcademicDtos.SectionDto> createSection(@Valid @RequestBody AcademicDtos.CreateSectionRequest request) {
        return new ResponseEntity<>(academicService.createSection(request), HttpStatus.CREATED);
    }

    // --- Student Enrollment ---
    @PostMapping("/management/enrollments")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Enroll student into section (Management only)")
    public ResponseEntity<AcademicDtos.StudentEnrollmentDto> enrollStudent(@Valid @RequestBody AcademicDtos.EnrollStudentRequest request) {
        return new ResponseEntity<>(academicService.enrollStudent(request), HttpStatus.CREATED);
    }

    @GetMapping("/academic/classes/{classId}/enrollments")
    @Operation(summary = "Get enrollments for class")
    public ResponseEntity<List<AcademicDtos.StudentEnrollmentDto>> getEnrollments(@PathVariable Long classId) {
        return ResponseEntity.ok(academicService.getEnrollmentsByClass(classId));
    }

    // --- Teacher Assignments ---
    @PostMapping("/management/assignments")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Assign teacher to subject and class (Management only)")
    public ResponseEntity<AcademicDtos.TeacherAssignmentDto> assignTeacher(@Valid @RequestBody AcademicDtos.AssignTeacherRequest request) {
        return new ResponseEntity<>(academicService.assignTeacher(request), HttpStatus.CREATED);
    }

    @GetMapping("/academic/assignments")
    @Operation(summary = "List all teacher subject assignments")
    public ResponseEntity<List<AcademicDtos.TeacherAssignmentDto>> getAssignments() {
        return ResponseEntity.ok(academicService.getAllTeacherAssignments());
    }
}
