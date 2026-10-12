package com.examforge.controller;

import com.examforge.dto.AcademicDtos;
import com.examforge.security.SecurityUtils;
import com.examforge.service.SubjectService;
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
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Course and Subject management and teacher assignments")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "Get all subjects (or assigned subjects for teacher)")
    public ResponseEntity<List<AcademicDtos.SubjectDto>> getSubjects() {
        if (SecurityUtils.isTeacher()) {
            Long teacherId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(subjectService.getSubjectsForTeacher(teacherId));
        }
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<AcademicDtos.SubjectDto> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Create subject (Management only)")
    public ResponseEntity<AcademicDtos.SubjectDto> createSubject(@Valid @RequestBody AcademicDtos.CreateSubjectRequest request) {
        return new ResponseEntity<>(subjectService.createSubject(request), HttpStatus.CREATED);
    }
}
