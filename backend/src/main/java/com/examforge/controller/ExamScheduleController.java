package com.examforge.controller;

import com.examforge.dto.ExamScheduleDtos;
import com.examforge.security.SecurityUtils;
import com.examforge.service.ExamScheduleService;
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
@Tag(name = "Exam Schedules", description = "Examination scheduling, conflict checking, calendar, and availability")
public class ExamScheduleController {

    private final ExamScheduleService examScheduleService;

    @PostMapping("/management/schedules")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Schedule approved examination (Management only)")
    public ResponseEntity<ExamScheduleDtos.ExamScheduleDto> scheduleExam(
            @Valid @RequestBody ExamScheduleDtos.CreateScheduleRequest request
    ) {
        return new ResponseEntity<>(examScheduleService.scheduleExam(request), HttpStatus.CREATED);
    }

    @PostMapping("/management/schedules/{id}/cancel")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Cancel examination schedule")
    public ResponseEntity<ExamScheduleDtos.ExamScheduleDto> cancelSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ExamScheduleDtos.CancelScheduleRequest request
    ) {
        return ResponseEntity.ok(examScheduleService.cancelSchedule(id, request));
    }

    @PostMapping("/management/schedules/{id}/release-results")
    @PreAuthorize("hasRole('MANAGEMENT')")
    @Operation(summary = "Manually release results for examination")
    public ResponseEntity<Void> releaseResults(@PathVariable Long id) {
        examScheduleService.releaseResults(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/schedules")
    @Operation(summary = "Get all examination schedules")
    public ResponseEntity<List<ExamScheduleDtos.ExamScheduleDto>> getAllSchedules() {
        return ResponseEntity.ok(examScheduleService.getAllSchedules());
    }

    @GetMapping("/schedules/{id}")
    @Operation(summary = "Get examination schedule by ID")
    public ResponseEntity<ExamScheduleDtos.ExamScheduleDto> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(examScheduleService.getScheduleById(id));
    }

    @GetMapping("/student/available-schedules")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get upcoming and available examinations for current student")
    public ResponseEntity<List<ExamScheduleDtos.ExamScheduleDto>> getAvailableForStudent() {
        Long studentId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(examScheduleService.getAvailableForStudent(studentId));
    }
}
